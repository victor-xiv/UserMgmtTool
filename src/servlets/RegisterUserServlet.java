package servlets;

import javax.servlet.http.HttpServlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import tools.ConcertoAPI;
import tools.SupportTrackerJDBC;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.concerto.sdk.security.ValidatedRequest;
import com.concerto.sdk.security.InvalidRequestException;
import ldap.*;

@SuppressWarnings("serial")
public class RegisterUserServlet extends HttpServlet {
	//ADDITIONAL VARIABLE
	Logger logger = Logger.getRootLogger();
	
	public void doGet(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException
    {
		String username = "";
		HttpSession session = request.getSession(true);
		try{
			ValidatedRequest req = new ValidatedRequest(request, LdapProperty.getProperty(LdapConstants.CONCERTO_VALIDATOR));
			if( req.getParameter("username") != null ){
				username = req.getParameter("username");
			}else{
				session.setAttribute("error", "This page can only be accessed from within Concerto.");
				String redirectURL = response.encodeRedirectURL("RegisterUser.jsp");
				response.sendRedirect(redirectURL);
			}
		}catch(InvalidRequestException ex){
			session.setAttribute("error", "This page can only be accessed from within Concerto.");
		}
		session.setAttribute("username", username);
		String redirectURL = response.encodeRedirectURL("RegisterUser.jsp");
		response.sendRedirect(redirectURL);
    }
	
	@SuppressWarnings("unchecked")
	public void doPost(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException
    {
		HttpSession session = request.getSession(true);
		String username = (String)session.getAttribute("username");
		session.removeAttribute("username");
		if( username == null ){
			session.setAttribute("error", "This page can only be accessed from within Concerto.");
			String redirectURL = response.encodeRedirectURL("RegisterUser.jsp");
			response.sendRedirect(redirectURL);
		}else{
			Map<String,String[]> userDetails = new HashMap<String,String[]>();
			userDetails.putAll((Map<String,String[]>)request.getParameterMap());
			userDetails.put("password", new String[]{request.getParameter("password01")});
			userDetails.put("sAMAccountName", new String[]{username});
			userDetails.put("isLdapClient", new String[]{"true"});

			LdapTool lt = new LdapTool();
			//ADDITIONAL CODE
			//Whether an error has occured
			boolean good = true;
			//Get company and mail info
			String company = request.getParameter("company");
			String email = request.getParameter("mail");
			//Get list of Orion Health email addresses from database - SPT-311
			List<String> emails = SupportTrackerJDBC.getEmails();
			//If an Orion User (no company and email registered as staff) - SPT-311
			if ( (request.getParameter("company").equals(""))&&(emails.contains(email.toLowerCase())) ) {
				//Set company as Orion Health
				userDetails.put("company", new String[]{"Orion Health"});
				company = "Orion Health";
			}
			//Check if userDN already exists - SPT-320
			if (lt.userDNExists(request.getParameter("displayName"), company)) {
				//If so, create error message and return as message to display
				String message = "<font color=\"red\">";
				message = "Unable to create user account. ";
				message += "An account has already been created with the same name. <br />";
				message += "Please contact Orion Health Support: <ul>";
				message += "<li>Phone </li>";
				message += "<li>Email <a href=\"mailto:support@orionhealth.com\">support@Orionhealth.com</a> </li>";
				message += "<li>Raise a Ticket </li></ul>";
				message += "</font>";
				session.setAttribute("error", message);
				logger.info("UserDN for user '"+username+"' already exists.");
				//Flag error
				good = false;
			}
			//Check if email already used in account and no error has occurred - SPT-314
			else if (lt.emailExists(email, company)&&good) {
				//If so, create error message and return as message to display
				String message = "<font color=\"red\">";
				message = "Unable to create user account. ";
				message += "An account with this email address ("+email+") already exists. <br />";
				message += "Please contact Orion Health Support: <ul>";
				message += "<li>Phone </li>";
				message += "<li>Email <a href=\"mailto:support@orionhealth.com\">support@Orionhealth.com</a> </li>";
				message += "<li>Raise a Ticket </li></ul>";
				message += "</font>";
				session.setAttribute("error", message);
				logger.info("Email '"+email+"' already in use.");
				//Flag error
				good = false;
			}
			//Check if username already exists and no error has occurred - SPT-320
			if (lt.usernameExists(username, company)&&good) {
				//If so, create error message and return as message to display
				String message = "<font color=\"red\">";
				message = "Unable to create user account. ";
				message += "An account has already been created for this user. <br />";
				message += "Please contact Orion Health Support: <ul>";
				message += "<li>Phone </li>";
				message += "<li>Email <a href=\"mailto:support@orionhealth.com\">support@Orionhealth.com</a> </li>";
				message += "<li>Raise a Ticket </li></ul>";
				message += "</font>";
				session.setAttribute("error", message);
				logger.info("Username '"+username+"' already exists.");
				//Flag error
				good = false;
			}
			//If the company has not been set as an OU and no error has occurred - SPT-316
			if ((!lt.companyExists(company))&&good) {
				//Get list of supported companies from database
				List<String> orgs = SupportTrackerJDBC.getOrganisations();
				//If this company is supported, set as OU
				if (orgs.contains(company)) {
					lt.addCompany(company);
					lt.addCompanyAsGroup(company);
					//Otherwise error and quit
				} else {
					session.setAttribute("error", "Your registration has failed - your organisation "+company+" has not been registered. "  
							+ "Please contact the system administrator.");
					//Flag error
					good = false;
				}
			}
			//If no error has occurred,
			if (good) {
				//ADDITIONAL CODE ENDS
				if(lt.addUser(userDetails)){
					session.setAttribute("passed", "You have been registered successfully.");
					ConcertoAPI.enableNT(username);
				}else{
					session.setAttribute("error", "Your registration has failed.  Please contact the system administrator.");
				} 
			}
			lt.close();
		}
		String redirectURL = response.encodeRedirectURL("RegisterUser.jsp");
		response.sendRedirect(redirectURL);
	}
}