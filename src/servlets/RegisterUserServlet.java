package servlets;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import ldap.ErrorConstants;
import ldap.LdapTool;

import org.apache.log4j.Logger;

import beans.UserDetails;
import tools.ConcertoAPI;
import tools.SupportTrackerJDBC;
import tools.ValidatedRequestHandler;

@SuppressWarnings("serial")
public class RegisterUserServlet extends HttpServlet {
	//ADDITIONAL VARIABLE
	Logger logger = Logger.getRootLogger(); // initiate as a default root logger
	
	public final static String REGISTER_IS_ORION_STAFF = "registerIsOrionStaff";
	public final static String REGISTER_IS_CLIENT = "registerIsAClient";
	
	public void doGet(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException
    {	
		logger.debug("RegisterUserServlet about to process Get request: " + request.getQueryString());

		String username = "";
		HttpSession session = request.getSession(true);
			
		//ValidatedRequest req = new ValidatedRequest(request, LdapProperty.getProperty(LdapConstants.CONCERTO_VALIDATOR));
		Hashtable<String, String> reqParams = ValidatedRequestHandler.processRequest(request);
			
		
		if(reqParams.get("isOrionStaff")==null || reqParams.get("isAClient")==null){
			session.setAttribute("error", "This page is not configured correctly. Please contact Orion Health Support Team.");
			logger.error("RegisterUserForKB page is not configured correctly. "
					+ "The parameter: isOrionStaff is " + reqParams.get("isOrionStaff") +
					"and isAClient is " + reqParams.get("isAClient")==null +
					". These two parameters must be defined in Portal web page.");
			String redirectURL = response.encodeRedirectURL("RegisterUser.jsp");
			response.sendRedirect(redirectURL);
			return;
		}
		
		if(!reqParams.get("isOrionStaff").equals("true") && !reqParams.get("isAClient").equals("true")){
			session.setAttribute("error", "This page can only be accessed by either an Orion Health's staff or client.");
			logger.error("The user " + username + " accessing RegisterUserForKB. This user is neither an Orion Health's staff nor client.");
			String redirectURL = response.encodeRedirectURL("RegisterUser.jsp");
			response.sendRedirect(redirectURL);
			return;
		}
		
		
		if( reqParams.containsKey("username") && reqParams.get("username") != null ){
			username = reqParams.get("username");
			
			try {
				UserDetails user = new UserDetails();
				user.processUsername(username);
				String company = user.getCompany();
				LdapTool lt = new LdapTool();
				boolean userExist = lt.usernameExists(username, company);
				if(userExist){
					session.setAttribute("error", "You already have an account in Ldap Server. For more detail, please contact Orion Health Support Team.");
					logger.error("User: " + username + " already exist in company: " + company);
					String redirectURL = response.encodeRedirectURL("RegisterUser.jsp");
					response.sendRedirect(redirectURL);
					return;
				}
			} catch (NamingException e) {
				session.setAttribute("error", "Could not connect to Ldap Server.");
				logger.error("Could not connect to Ldap Server", e);
				String redirectURL = response.encodeRedirectURL("RegisterUser.jsp");
				response.sendRedirect(redirectURL);
				return;
			}
			
		
		// if there is no "username" key and no "error" key in the request parameters
		// It means that the request is valid but the request doesn't contains "username" => can't process further
		} else if (!reqParams.containsKey("error")) {
			session.setAttribute("error", "This page can only be accessed from within Concerto.");
			logger.error(ErrorConstants.NO_USERNAME_SPECIFIED);
			String redirectURL = response.encodeRedirectURL("RegisterUser.jsp");
			response.sendRedirect(redirectURL);
			return;
		}
			
		// if request validation and encryption failed => reqParams must contains "error" key
		if(reqParams.containsKey("error")){
			session.setAttribute("error", reqParams.get("error"));
			// we are not logging this error here, because it is already logged in the ValidatedRequestHandler.processRequest()
		}			
		
		// userDN must not be an empty String
		if (username.isEmpty()){
			session.setAttribute("error", "There is no username found in the request parameters.");
			logger.error("username is an empty String");
		}

		LdapTool lt = null;
		try {
			 lt = new LdapTool();
		} catch (NamingException e) {
			session.setAttribute("error", "Could not connect to Ldap Server.");
			logger.error("Could not connect to Ldap Server.");
			String redirectURL = response.encodeRedirectURL("RegisterUser.jsp");
			response.sendRedirect(redirectURL);
			return;
		}
		
		UserDetails user = new UserDetails();
		user.processUsername(username);
		String fullname = user.getDisplayName();
		String company = user.getCompany();
		if(fullname != null && !fullname.trim().isEmpty() && company != null && !company.trim().isEmpty()){
			boolean doesUserExist = lt.userDNExists(fullname, company);
			if(doesUserExist){
				session.setAttribute("error", "You have already been registered. Please contact Orion Health Support Team for more detail.");
				logger.error("User: " + fullname + " from company: " + company + " have already been registered.");
				String redirectURL = response.encodeRedirectURL("RegisterUser.jsp");
				response.sendRedirect(redirectURL);
			}
		}

		
		logger.debug("Redirect request to: " + "RegisterUser.jsp");
		
		session.setAttribute("username", username);
		session.setAttribute(REGISTER_IS_ORION_STAFF,  reqParams.get("isOrionStaff"));
		session.setAttribute(REGISTER_IS_CLIENT, reqParams.get("isAClient"));
		String redirectURL = response.encodeRedirectURL("RegisterUser.jsp");
		response.sendRedirect(redirectURL);
    }
	
	@SuppressWarnings("unchecked")
	public void doPost(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException
    {	
		logger.debug("RegisterUserServlet about to process Post request");

		HttpSession session = request.getSession(true);
		String username = (String)session.getAttribute("username");
		session.removeAttribute("username");
		
		boolean isOrionStaff = session.getAttribute(REGISTER_IS_ORION_STAFF).equals("true");
		boolean isClient = session.getAttribute(REGISTER_IS_CLIENT).equals("true");
		session.removeAttribute(REGISTER_IS_ORION_STAFF);
		session.removeAttribute(REGISTER_IS_CLIENT);
		
		if( username == null || username.trim().isEmpty()){
			session.setAttribute("error", "This page can only be accessed from within Concerto. The username is not specified.");
			String redirectURL = response.encodeRedirectURL("RegisterUser.jsp");
			response.sendRedirect(redirectURL);
			return;
		}
		
		// check if username contains any prohibited chars
		String temp = username.replaceAll("[\\,\\<\\>\\;\\=\\*\\[\\]\\|\\:\\~\\#\\+\\&\\%\\{\\}\\?]", "");
		if(temp.length() < username.length()){
			session.setAttribute("error", "Username contains some forbid speical characters. The special characters allowed to have in username are: ( ) . - _ ` ~ @ $ ^");
			String redirectURL = response.encodeRedirectURL("RegisterUser.jsp");
			response.sendRedirect(redirectURL);
			return;
		}
		
		Map<String, String[]> userDetails = new HashMap<String, String[]>();
		userDetails.putAll((Map<String, String[]>) request.getParameterMap());
		userDetails.put("password01",
				new String[] { request.getParameter("password01") });
		userDetails.put("sAMAccountName", new String[] { username });

		// connecting to LdapServer
		LdapTool lt = null;
		try {
			lt = new LdapTool();
		} catch (NamingException | FileNotFoundException e) {
			String errorMessage = String.format("Your registration has failed because of %s. Please contact the system administrator.", e.getMessage());
			session.setAttribute("error", errorMessage);
			String redirectURL = response.encodeRedirectURL("RegisterUser.jsp");
			response.sendRedirect(redirectURL);
			return;
		}

		/* ADDITIONAL CODE
		   Whether an error has occured*/
		
		boolean good = true; //error flag, if there's no error, good is always true, otherwise it will be set to false 
		
		// Get company and mail info
		String company = request.getParameter("company");
		String email = request.getParameter("mail");

		// Get list of Orion Health email addresses from database - SPT-311
//		List<String> emails = null;
//		try {
//			emails = SupportTrackerJDBC.getEmails();
//		} catch (SQLException e) {
//			String errorMessage = String.format("Your registration has failed because of %s. Please contact the system administrator.", e.getMessage());
//			session.setAttribute("error", errorMessage);
//			String redirectURL = response.encodeRedirectURL("RegisterUser.jsp");
//			response.sendRedirect(redirectURL);
//			return;
//		}

		// If an Orion User (no company and email registered as staff) - SPT-311
//		if ((request.getParameter("company").trim().equals(""))
//				&& (emails.contains(email.toLowerCase()))) {


		if(isClient) userDetails.put("isLdapClient", new String[] { "true" });
		if (isOrionStaff) {  //SPT-1241
			// Set company as Orion Health
			userDetails.put("company", new String[] { LdapTool.ORION_HEALTH_NAME });
			company = LdapTool.ORION_HEALTH_NAME;
		}

		
		// Check if userDN already exists - SPT-320
		if (lt.userDNExists(request.getParameter("displayName"), company)) {
			// If so, create error message and return as message to display
			String message = "<font color=\"red\">";
			message = "Unable to create user account. ";
			message += "An account has already been created with the same name. <br />";
			message += "Please contact Orion Health Support: <ul>";
			message += "<li>Phone </li>";
			message += "<li>Email <a href=\"mailto:support@orionhealth.com\">support@Orionhealth.com</a> </li>";
			message += "<li>Raise a Ticket </li></ul>";
			message += "</font>";
			session.setAttribute("error", message);
			logger.debug("UserDN for user '" + username + "' already exists.");
			// Flag error
			good = false;
		}
		// Check if email already used in account and no error has occurred -
		// SPT-314
		else if (lt.emailExists(email, company) && good) {
			// If so, create error message and return as message to display
			String message = "<font color=\"red\">";
			message = "Unable to create user account. ";
			message += "An account with this email address (" + email + ") already exists. <br />";
			message += "Please contact Orion Health Support: <ul>";
			message += "<li>Phone </li>";
			message += "<li>Email <a href=\"mailto:support@orionhealth.com\">support@Orionhealth.com</a> </li>";
			message += "<li>Raise a Ticket </li></ul>";
			message += "</font>";
			session.setAttribute("error", message);
			logger.debug("Email '" + email + "' already in use.");
			// Flag error
			good = false;
		}
		// Check if username already exists and no error has occurred - SPT-320
		if (lt.usernameExists(username, company) && good) {
			// If so, create error message and return as message to display
			String message = "<font color=\"red\">";
			message = "Unable to create user account. ";
			message += "An account has already been created for this user. <br />";
			message += "Please contact Orion Health Support: <ul>";
			message += "<li>Phone </li>";
			message += "<li>Email <a href=\"mailto:support@orionhealth.com\">support@Orionhealth.com</a> </li>";
			message += "<li>Raise a Ticket </li></ul>";
			message += "</font>";
			session.setAttribute("error", message);
			logger.debug("Username '" + username + "' already exists.");
			// Flag error
			good = false;
		}
		// If the company has not been set as an OU and no error has occurred -
		// SPT-316
		boolean compExistsAsClient = lt.companyExists(company);
		boolean compExistsAsGroup = lt.companyExistsAsGroup(company);

//		if(company.equals(LdapTool.ORION_HEALTH_NAME)){ 
//			// if this user is an orion health staff, 
//			// then we don't need to check whether the Orion Health exists in Clients and Groups 
//			compExistsAsClient = true;
//			compExistsAsGroup = true;
//		}
		
		// Get list of supported companies from database
		List<String> orgs = null;
		try {
			orgs = SupportTrackerJDBC.getOrganisations();
		} catch (SQLException e) {
			String errorMessage = String.format("Your registration has failed because of %s. Please contact the system administrator.", e.getMessage());
			session.setAttribute("error", errorMessage);
			String redirectURL = response.encodeRedirectURL("RegisterUser.jsp");
			response.sendRedirect(redirectURL);
			return;
		}
		
		if(!compExistsAsClient && good){
			// If this company is supported, set as OU
			if (orgs.contains(company)) {
				try {
					compExistsAsClient = lt.addCompany(company);
				} catch (NamingException e) {
					session.setAttribute("error",
					"Your registration has failed - your organisation "
					+ company + " has not been registered. "
					+ "Please contact the system administrator. "
					+ "This due to: " + e.getMessage());
					// Flag error
					good = false;
				}
							
			// Otherwise error and quit
			} else {
				session.setAttribute("error",
				"Your registration has failed - your organisation "
				+ company + " doesn't contain in Support Tracker Database. "
				+ "Please contact the system administrator.");
				// Flag error
				good = false;
			}
		}
		
		if (!compExistsAsGroup && compExistsAsClient && good){
			try {
				compExistsAsGroup = lt.addCompanyAsGroup(company);
			} catch (NamingException e) {
				session.setAttribute("error",
					"Your registration has failed - your organisation "
					+ company
					+ " has been added into LDAP Clients, but it could not be added into LDAP Groups. "
					+ "Please contact the system administrator. "
					+ "This due to: " + e.getMessage());
				// Flag error
				good = false;
			}
		}

		// If no error has occurred,
		if (compExistsAsClient && compExistsAsGroup && good) {
			boolean addUserStatus = false;
			
			try{
				addUserStatus = lt.addUser(userDetails);
			} catch (Exception e){
				session.setAttribute("error", username + " couldn't register into Concerto. Because of: " +e.getMessage());
			}
			
			// ADDITIONAL CODE ENDS
			
			if (addUserStatus) {
				session.setAttribute("passed", "You have been registered into LDAP server successfully.");
				try {
					new ConcertoAPI().setUserToUsePasswordStoredInLdap(username);
				} catch (Exception e) {
					session.setAttribute("error", username + " has been added to LDAP server. But it couldn't be registered into Concerto. Because of: " +e.getMessage());
					// we are not logging this error, because it has been logged
					// in ConcertoAPI.enableNT()
				}
			} else {
				session.setAttribute("error",
						"Your registration has failed. " + "The " +username +" could not be added into LDAP server"
						+ "Please contact the system administrator.");
			}
		}
		lt.close();

		String redirectURL = response.encodeRedirectURL("RegisterUser.jsp");
		response.sendRedirect(redirectURL);
	}
}
