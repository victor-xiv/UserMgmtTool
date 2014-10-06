package servlets;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;

import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import ldap.ErrorConstants;
import ldap.LdapTool;

import org.apache.log4j.Logger;

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
		String userDN = "";
		HttpSession session = request.getSession(true);
		if(session.getAttribute("dn") != null){
			userDN = (String)session.getAttribute("dn");
			
		} else {

			// validate and encrypt the request
			Hashtable<String, String> reqParams = ValidatedRequestHandler.processRequest(request);
			
			
			if (reqParams.containsKey("userDN") && reqParams.get("userDN") != null) {
				userDN = reqParams.get("userDN");
				
			// if there is no "userDN" key and no "error" key in the request parameters
			// It means that the request is valid but the request doesn't contains "userDN" => can't process further
			} else if (!reqParams.containsKey("error")) {
				session.setAttribute("error", "<font color=\"red\">Non-ldap user cannot change username via this menu.</font>");
				logger.error(ErrorConstants.NO_USERDN_SPECIFIED);
				String redirectURL = response.encodeRedirectURL("ChangeUserPassword.jsp");
				response.sendRedirect(redirectURL);
				return;
			}

			// if request validation and encryption failed => reqParams must contains "error" key
			if(reqParams.containsKey("error")){
				session.setAttribute("error", "<font color=\"red\">" + reqParams.get("error")  + "</font>");
				// we are not logging this error here, because it is already logged in the ValidatedRequestHandler.processRequest()
			}
		}
		
		// userDN must not be an empty String
		if (userDN.isEmpty()){
			session.setAttribute("error", "<font color=\"red\">There is no userDN found in the request parameters.</font>");
			logger.error("userDN is an empty String");
		}
		
		logger.info("Redirect request to: " + "ChangeUserPassword.jsp");
		
		session.setAttribute("userDN", userDN);
		String redirectURL = response.encodeRedirectURL("ChangeUserPassword.jsp");
		response.sendRedirect(redirectURL);
    }
	
	
	/**
	 * Accept the new password request and update that new password in the Ldap server
	 */
	public void doPost(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException{
		
		HttpSession session = request.getSession(true);
		String userDN = (String)session.getAttribute("userDN");
		if( userDN == null )
			userDN = request.getParameter("userDN");
		String password01 = request.getParameter("password01");
		
		
		/**
		 * connecting to Ldap server
		 */
		LdapTool lt = null;
		try {
			lt = new LdapTool();
		} catch (FileNotFoundException fe){
			session.setAttribute("error", "<font color=\"red\">" + fe.getMessage()  + "</font>");
			// we are not logging this error here, because it is already logged in LdapTool()
		} catch (NamingException e) {
			session.setAttribute("error", "<font color=\"red\">" + e.getMessage()  + "</font>");
			// we are not logging this error here, because it is already logged in LdapTool()
		}
		
		// if only Ldap server can be connected successfully
		// update the password
		if( lt != null){
			if(lt.changePassword(userDN, password01)){
				session.setAttribute("passed", "The password was changed successfully.");
				logger.info("\" " + userDN + " \"" +  "password was successfully updated.");
			}else{
				session.setAttribute("failed", "The password change has failed.");
				logger.info("\" " + userDN + " \"" +  "password was unsuccessfully updated.");
			}
			lt.close();
		}
		
		session.setAttribute("userDN", userDN);
		String redirectURL = response.encodeRedirectURL("ChangeUserPassword.jsp");
		response.sendRedirect(redirectURL);
	}
}
