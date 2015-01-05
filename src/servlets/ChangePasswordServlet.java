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
import tools.SyncAccountDetails;
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
		logger.debug("ChangePasswordServlet about to process GET request: " + request.getParameterMap());
		
		String userDN = "";
		HttpSession session = request.getSession(true);
		String redirectURL = "ChangeUserPassword.jsp";
		
		// there are only two entries to this Servlet GET method
		
		// 1). if the request is getting through UserDetail.jsp
		if(request.getParameter("rqstFrom")!=null &&
				request.getParameter("rqstFrom").trim().equals("userDetail")){
			if(session.getAttribute("dn") != null){
				userDN = (String)session.getAttribute("dn");		
			}
			redirectURL += "?userDetails=true";
		
			
		// 2). if the request is getting through directly from Portal
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
				redirectURL = response.encodeRedirectURL("ChangeUserPassword.jsp");
				response.sendRedirect(redirectURL);
				return;
			}

			// if request validation and encryption failed => reqParams must contains "error" key
			if (reqParams.containsKey("error")) {
				session.setAttribute("error", 
						"<font color=\"red\">" + reqParams.get("error") + "</font>");
				// we are not logging this error here, because it is already
				// logged in the ValidatedRequestHandler.processRequest()
				redirectURL = response.encodeRedirectURL("ChangeUserPassword.jsp");
				response.sendRedirect(redirectURL);
				return;
			}
		}
		
		
		// userDN must not be an empty String
		if (userDN.isEmpty()){
			session.setAttribute("error", "<font color=\"red\">There is no userDN found in the request parameters.</font>");
			logger.error("userDN is an empty String");
			redirectURL = response.encodeRedirectURL("ChangeUserPassword.jsp");
			response.sendRedirect(redirectURL);
			return;
		}
		
		
		
		/**
		 * synchronizing the account details from Support Tracker DB to Ldap account
		 */
		userDN = SyncAccountDetails.syncAndGetBackNewUserDNForGivenUserDN(userDN);
		
		
		
		logger.debug("Redirect request to: ChangeUserPassword.jsp");
		session.setAttribute("userDN", userDN);
		redirectURL = response.encodeRedirectURL(redirectURL);
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
		boolean isPswGenerated = false;
		String result = "failed|Could not get mobile phone.";
		try {
			result = updatePassword(userDN, newPsw, isPswGenerated);
		} catch (NamingException e) {
			response.getWriter().write("Couldn't update Ldap Account's password due to: " + e.getMessage());
		}
		response.getWriter().write(result);
	}


	/**
	 * a helper for doPost() method that change the password of the given user (come with session) with the new and randomly generated by the tools.PasswordGenerator.generatePswForLength() method
	 * @param request HttpServletRequest passed through from doPost() method. the session must contains "userDN" value
	 * @param response HttpServletResponse, used to write a response to the client 
	 * @throws IOException if failed to write to the response channel 
	 * @throws NamingException 
	 */
	private void changePswByGenerateANew(HttpServletRequest request, HttpServletResponse response) throws IOException{
		String userDN = getUserDN(request);
		if(userDN==null){
			response.getWriter().write("failed|Could not get userDN, please check the entry point.");
			return;
		}
		String newPsw = tools.PasswordGenerator.generatePswForLength(8);
		boolean isPswGenerated = true;
		String result = "failed|Could not get mobile phone.";
		try {
			result = updatePassword(userDN, newPsw, isPswGenerated);
		} catch (NamingException e) {
			response.getWriter().write("Couldn't update Ldap Account's password due to: " + e.getMessage());
		}
		response.getWriter().write(result);
	}
	
	

	/**
	 * a helper for doPost() method that check whether the given user (come with the session) has a valid mobile phone number
	 * @param request HttpServletRequest passed through from doPost() method. the session must contains "userDN" value
	 * @param response HttpServletResponse, used to write a response to the client. the response will be either "false" 
	 * if this user doesn't have a mobile phone or has an invalid mobile phone or "true" if this has a valid mobile phone number
	 * @throws IOException if failed to write to the response channel 
	 * @throws NamingException 
	 */
	private void shouldAllowGeneratingPsw(HttpServletRequest request, HttpServletResponse response) throws IOException{
		String userDN = getUserDN(request);
		if(userDN==null) return; // it has been logged in getUserDN() method;
		
		logger.debug("Validate whether this user: " + userDN + " has a valid mobile number.");
		
		String mobile = null;
		try {
			mobile = getMobilePhoneForUser(userDN);
		} catch (NamingException e) {
		}
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
	 * @param isPswGenerated true if newPsw param was generated programatically, false otherwise
	 * @return the result of the updating process as a String
	 * @throws IOException
	 * @throws NamingException 
	 */
	public static String updatePassword(String userDN, String newPsw, boolean isPswGenerated) throws IOException, NamingException {
		Logger logger = Logger.getRootLogger();
		logger.debug("Updating a new password for user: " + userDN );
		
		LdapTool lt = null;
		try {
			lt = new LdapTool();
		} catch (NamingException e2) {
			return "failed|Could not change password for this user. Please contact Orion Health's support team.";
			// we are not logging here, because it has been logged in LdapTool() method
		}
		
		if(!lt.changePassword(userDN, newPsw)){
			lt.close();
			return "failed|Could not change password for this user. Please contact Orion Health's support team.";
			// we are not logging here, because it has been logged in changePassword() method
		}
		
		Attributes atrs = lt.getUserAttributes(userDN);
		if(atrs == null || atrs.get("sAMAccountName") == null){
			logger.error("A new password for this user:" +userDN+  " has been updated. But, it could not get user's login name.");
			lt.close();
			return "failed|The new password has been updated successfully. But it could not be updated to log in with LDAP server because there is no username found.";
		}
		
		String username = null;
		try {
			username = atrs.get("sAMAccountName").get().toString();
		} catch (NamingException e1) {
			logger.error("A new password for this user:" +userDN+  " has been updated. But, it could not get user's login name.");
			lt.close();
			return "failed|The new password has been updated successfully. But it could not be updated to log in with LDAP server because there is no username found.";
		}
		
		try {
			new ConcertoAPI().enableNT(username);
		} catch (Exception e) {
			lt.close();
			// we are not logging there because it has been logged in enableNT() method
			return "failed|The new password has been updated successfully. But it could not be updated to log in with LDAP server.";
		}
		
		
		String mobile = getMobilePhoneForUser(userDN);
		
		if(mobile == null || !isPswGenerated){
			logger.debug("A new password has been updated on this user. " + userDN +". But, his/her mobile phone is invalid");
			lt.close();
			return "passed|A new password for this user: " + username + " has been updated successfully. ";
		} else {
			
			try{
				EmailClient.sendNewPasswordToSMS(mobile, userDN, newPsw);
			} catch (MessagingException e){
				lt.close();
				return "passed|The new password has been updated successfully. But the password couldnot be sent to the given mobile number: " + mobile + ". Because: " + e.getMessage();
			}
			
			
			logger.debug("a new password has been updated successfully for this user: " + userDN + ". A SMS has been sent to this number: " + mobile);
			lt.close();
			return "passed|The new password has been updated successfully. If this user is not receiving a text message at "+mobile+" within 24 hours, please contact Orion Health's support team.";
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
	 * @throws NamingException 
	 */
	public static String getMobilePhoneForUser(String userDN) throws FileNotFoundException, NamingException{
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
		Attributes attrs = lt.getUserAttributes(userDN);
		try{
			String clientAccountId = attrs.get("Info").get().toString();
	
		// II). retrieve the mobile phone number from Support Tracker DB
			String mobile = SupportTrackerJDBC.validateAndGetMobilePhoneOfUser(username, clientAccountId); 
			return mobile;
		} catch (NullPointerException e){
			// NullPointerException thrown when attrs.get("Info") is null (when there's no account id stored in ldap account)
			// so, we can't find the mobile phone from the support tracker db.
			return null;
		}
	}
	
}
