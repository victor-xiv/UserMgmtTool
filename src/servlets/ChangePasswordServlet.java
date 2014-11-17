package servlets;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;

import javax.mail.MessagingException;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import ldap.ErrorConstants;
import ldap.LdapProperty;
import ldap.LdapTool;

import org.apache.log4j.Logger;

import tools.ConcertoAPI;
import tools.EmailClient;
import tools.SupportTrackerJDBC;
import tools.ValidatedRequestHandler;


/**
 * get the encrypted request from the client:
 * + Validate and encrypt that request
 * + Check if there is "userDN" value in the request's parameter
 * + redirect the request to ChangeUserPassword.jsp 
 * @author adminuser
 *
 */
@SuppressWarnings("serial")
public class ChangePasswordServlet extends HttpServlet {
	
	private Logger logger = Logger.getRootLogger(); // initiate as a default root logger
	
	
	/**
	 * Accept the encrypted request, validates and decrypts the request
	 * If request parameters contains "userDN" 
	 * => redirect to ChangeUserPassword.jsp and let user to change his/her password.
	 * otherwise redirect to ChangeUserPassword.jsp with "errro" flag.
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException
    {
		logger.debug("ChangePasswordServlet about to process GET request: " + request.getQueryString());
		
		String userDN = "";
		HttpSession session = request.getSession(true);
		
		// there are only two entries to this Servlet GET method
		
		// 1). if the request is getting through UserDetail.jsp
		if(request.getParameter("rqstFrom")!=null &&
				request.getParameter("rqstFrom").trim().equals("userDetail")){
			if(session.getAttribute("dn") != null){
				userDN = (String)session.getAttribute("dn");		
			}
		
			
		// 2). if the request is getting through directly from Support Tracker 
		} else {
			// validate and encrypt the request
			Hashtable<String, String> reqParams = ValidatedRequestHandler.processRequest(request);

			if (reqParams.containsKey("userDN") && reqParams.get("userDN") != null) {
				userDN = reqParams.get("userDN");

				// if there is no "userDN" key and no "error" key in the request parameters
				// It means that the request is valid but the request doesn't
				// contains "userDN" => can't process further
			} else if (!reqParams.containsKey("error")) {
				session.setAttribute("error",
						"<font color=\"red\">Non-ldap user cannot change username via this menu.</font>");
				logger.error(ErrorConstants.NO_USERDN_SPECIFIED);
				String redirectURL = response.encodeRedirectURL("ChangeUserPassword.jsp");
				response.sendRedirect(redirectURL);
				return;
			}

			// if request validation and encryption failed => reqParams must contains "error" key
			if (reqParams.containsKey("error")) {
				session.setAttribute("error", 
						"<font color=\"red\">" + reqParams.get("error") + "</font>");
				// we are not logging this error here, because it is already
				// logged in the ValidatedRequestHandler.processRequest()
			}
		}
		
		
		// userDN must not be an empty String
		if (userDN.isEmpty()){
			session.setAttribute("error", "<font color=\"red\">There is no userDN found in the request parameters.</font>");
			logger.error("userDN is an empty String");
		}
		
		logger.debug("Redirect request to: " + "ChangeUserPassword.jsp");
		
		session.setAttribute("userDN", userDN);
		String redirectURL = response.encodeRedirectURL("ChangeUserPassword.jsp");
		response.sendRedirect(redirectURL);
    }
	
	
	/**
	 * Accept the new password request and update that new password in the Ldap server
	 */
	public void doPost(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException{
		
		String rqst = request.getParameter("rqst");
		
		logger.debug("ChangePasswordServlet start processing request: " + rqst);
		
		switch (rqst.trim()){
			// if user click on Generate button (on ChangeUserPassword.jsp)
			case "GeneratePassword" :
				changePswByGenerateANew(request, response);
				break;
			
			// automatically processed every time ChangeUserPassword.jsp is loaded
			case "ShouldAllowGeneratingPsw" :
				shouldAllowGeneratingPsw(request, response);
				break;
			
			// if user manually type in new password and click on Submit button (on ChangeUserPassword.jsp)
			case "ChangePassword" :
				changePswByUser(request, response);
				break;
				
			default :
				break;
		}
	}
	
	
	/**
	 * a helper for doPost() method that change the password of the given user (come with session) with the new manually typed in password (come with request)
	 * @param request HttpServletRequest passed through from doPost() method. the session must contains "userDN" value and request must contains "NewPsw" value
	 * @param response HttpServletResponse, used to write a response to the client 
	 * @throws IOException if failed to write to the response channel 
	 */
	private void changePswByUser(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String userDN = getUserDN(request);
		if(userDN==null){
			response.getWriter().write("failed|Could not get userDN, please check the entry point.");
			return;
		}
		String newPsw = request.getParameter("NewPsw");
		String result = updatePassword(userDN, newPsw);
		response.getWriter().write(result);
	}


	/**
	 * a helper for doPost() method that change the password of the given user (come with session) with the new and randomly generated by the tools.PasswordGenerator.generatePswForLength() method
	 * @param request HttpServletRequest passed through from doPost() method. the session must contains "userDN" value
	 * @param response HttpServletResponse, used to write a response to the client 
	 * @throws IOException if failed to write to the response channel 
	 */
	private void changePswByGenerateANew(HttpServletRequest request, HttpServletResponse response) throws IOException{
		String userDN = getUserDN(request);
		if(userDN==null){
			response.getWriter().write("failed|Could not get userDN, please check the entry point.");
			return;
		}
		String newPsw = tools.PasswordGenerator.generatePswForLength(8);
		String result = updatePassword(userDN, newPsw);
		response.getWriter().write(result);
	}
	
	

	/**
	 * a helper for doPost() method that check whether the given user (come with the session) has a valid mobile phone number
	 * @param request HttpServletRequest passed through from doPost() method. the session must contains "userDN" value
	 * @param response HttpServletResponse, used to write a response to the client. the response will be either "false" 
	 * if this user doesn't have a mobile phone or has an invalid mobile phone or "true" if this has a valid mobile phone number
	 * @throws IOException if failed to write to the response channel 
	 */
	private void shouldAllowGeneratingPsw(HttpServletRequest request, HttpServletResponse response) throws IOException{
		String userDN = getUserDN(request);
		if(userDN==null) return; // it has been logged in getUserDN() method;
		
		logger.debug("Validate whether this user: " + userDN + " has a valid mobile number.");
		
		String mobile = getMobilePhoneForUser(userDN);
		if(mobile == null){
			response.getWriter().write("false");
		} else {
			response.getWriter().write("true");
		}
	}
	
	
	/**
	 * update the password of the given user with the given newPsw
	 * @param userDN Ldap userDN whose password need to be changed. userDN must hasnot been escaped any reserved chars
	 * (e.g. userDN="CN=Mike+Jr,OU=Group, I,OU=Clients,DC=orion,DC=dmz")
	 * @param newPsw used to update
	 * @return the result of the updating process as a String
	 * @throws IOException
	 */
	public static String updatePassword(String userDN, String newPsw) throws IOException {
		Logger logger = Logger.getRootLogger();
		logger.debug("Updating a new password for user: " + userDN + " with: " + newPsw);
		
		LdapTool lt = null;
		try {
			lt = new LdapTool();
		} catch (NamingException e2) {
			return "failed|Could not change password for this user. Please contact Orion Health's support team.";
			// we are not logging here, because it has been logged in LdapTool() method
		}
		
		if(!lt.changePassword(userDN, newPsw)){
			return "failed|Could not change password for this user. Please contact Orion Health's support team.";
			// we are not logging here, because it has been logged in changePassword() method
		}
		
		String mobile = getMobilePhoneForUser(userDN);
		
		if(mobile == null){
			logger.error("A new password: " + newPsw + " has been updated on this user. " + userDN +". But, his/her mobile phone is invalid");
			return "A new password: " + newPsw + " has been updated on this user. " + userDN +". But, his/her mobile phone is invalid";
		} else {
			Attributes atrs = lt.getUserAttributes(userDN);
			if(atrs == null || atrs.get("sAMAccountName") == null){
				logger.error("A new password for this user:" +userDN+  " has been updated. But, it could not get user's login name.");
				return "A new password: " + newPsw + " has been updated on this user. But this user's mobile phone number could not be retrieved. Please contact Orion Health's support team.";
			}
			
			String username = null;
			try {
				username = atrs.get("sAMAccountName").get().toString();
			} catch (NamingException e1) {
				logger.error("A new password for this user:" +userDN+  " has been updated. But, it could not get user's login name.");
				return "A new password: " + newPsw + " has been updated on this user. But this user's mobile phone number could not be retrieved. Please contact Orion Health's support team.";
			}
			
			try{
				EmailClient.sendNewPasswordToSMS(mobile, userDN, newPsw);
			} catch (MessagingException e){
				return "The new password has been updated successfully. But the password couldnot be sent to the given mobile number: " + mobile + ". Because: " + e.getMessage();
			}
			
			try {
				ConcertoAPI.enableNT(username);
			} catch (Exception e) {
				// we are not logging there because it has been logged in enableNT() method
				return "The new password has been updated successfully. But it could not be updated to log in with LDAP server.";
			}
			logger.debug("a new password " + newPsw + " updated successfully for this user: " + userDN + ". A SMS has been sent to this number: " + mobile);
			return "The new password has been updated successfully. If this user is not receiving a text message at "+mobile+" within 24 hours, please contact Orion Health's support team.";
		}
	}
	
	
	/**
	 * a helper method to get the userDN from the session or request.getParameter("userDN");
	 * @param request
	 * @return
	 * @throws IOException
	 */
	private String getUserDN(HttpServletRequest request) throws IOException{
		logger.debug("Retrieving userDN from the request and session.");
		
		String userDN = request.getParameter("userDN");
		if( userDN == null ){
			HttpSession session = request.getSession(true);
			userDN = (String)session.getAttribute("userDN"); 
		}
		
		// validate userDN
		if(userDN==null || !(userDN.contains("CN=") && userDN.contains("OU="))){
			logger.error("Could not get a correct userDN. The retrieved userDN is: " + userDN + ". Please check the entry point");
			return null;
		}
		
		return userDN;
	}
	
	
	/**
	 * retrieve the mobile phone of the given user from Support Tracker DB.
	 * 
	 * @param userDN is used by only LDAP server (Support Tracker DB doesn't know anything about userDN).
	 * So, userDN is used by LDAP server to get the login name (sAMAccountName), and then
	 * this login name is used to retrieve the mobile number from Support Tracker DB.
	 * 
	 * @return the mobile phone number as a string (if that mobile phone number is valid), 
	 * otherwise (and if there's no mobile number stored in the Support Tracker DB for the given user) it will return null

	 * @throws FileNotFoundException if LdapTool connection object can't be instantiated
	 */
	public static String getMobilePhoneForUser(String userDN) throws FileNotFoundException{
		Logger logger = Logger.getRootLogger();
		logger.debug("Start retrieving a mobile phone number for this user: " + userDN);
		
	// I). trying to get login name from Ldap Server
		LdapTool lt = null;
		try {
			lt = new LdapTool();
		} catch (NamingException e2) {
			// we are not logging here, because it has been logged in LdapTool() method
			return null;
		}
		
		String username = lt.getUsername(userDN);
		if(username.trim().isEmpty()){
			logger.debug("This userDN: " + userDN + " doesn't have a login (sAMAccountName) name.");
			return "This user doesn't have a login name.";
		}

	// II). retrieve the mobile phone number from Support Tracker DB
		String mobile = SupportTrackerJDBC.validateAndGetMobilePhoneOfUser(username); 
		return mobile;
	}
	
}
