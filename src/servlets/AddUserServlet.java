package servlets;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import ldap.LdapTool;

import org.apache.log4j.Logger;

import tools.ConcertoAPI;
import tools.EmailClient;
import tools.SupportTrackerJDBC;

@SuppressWarnings("serial")
public class AddUserServlet extends HttpServlet {
	Logger logger = Logger.getRootLogger();
	
	public void doGet(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException
    {
		String redirectURL = response.encodeRedirectURL("AddNewUser.jsp");
		response.sendRedirect(redirectURL);
    }
	
	@SuppressWarnings("unchecked")
	public void doPost(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException
    {
		logger.debug("AddUserServlet about to process Post request: " + request.getQueryString());
		
		HttpSession session = request.getSession(true);
		Map<String,String[]> paramMaps = (Map<String,String[]>)request.getParameterMap();
		Map<String,String[]> maps = new HashMap<String, String[]>();
		maps.putAll(paramMaps);
		maps.put("isLdapClient", new String[]{"true"});
		maps.put("password01", new String[]{"password1"});
		
		
		
		/**
		 * From here to the end of this method is duplicated with servlet.AcceptRequestServlet.doGet().
		 * I can't refactor and make this part to a single method and let them use a single method.
		 * because, they are too strong couple to their respective .jsp.
		 * 
		 * So, if you update this part, please double check servlet.AcceptRequestServlet.doGet(), you might
		 * also need to update that part as well.
		 */
		
		String sAMAccountName = request.getParameter("sAMAccountName").trim();
		logger.debug("Username: "+sAMAccountName);
			if( sAMAccountName == null || sAMAccountName.trim().equals("")){
				String msg = "User was not added with an empty or null .";
				session.setAttribute("message", "<font color=\"red\"><b>" + msg + "</b></font>");
				String redirectURL = response.encodeRedirectURL("AddNewUser.jsp");
				response.sendRedirect(redirectURL);
				return;
			}
			
			// check if sAMAccountName contains any prohibited chars
			String temp = sAMAccountName.replaceAll("[\\,\\<\\>\\;\\=\\*\\[\\]\\|\\:\\~\\#\\+\\&\\%\\{\\}\\?]", "");
			if(temp.length() < sAMAccountName.length()){
				String msg = "User was not added because username contains some forbid speical characters. The special characters allowed to have in username are: ( ) . - _ ` ~ @ $ ^";
				session.setAttribute("message", "<font color=\"red\"><b>" + msg + "</b></font>");
				String redirectURL = response.encodeRedirectURL("AddNewUser.jsp");
				response.sendRedirect(redirectURL);
				return;
			}
			
			LdapTool lt = null;	
			try {
				// connect to ldap server
				lt = new LdapTool();
			} catch (Exception e1) {
				if(lt != null) lt.close();
				String msg = "User was not added because: " + e1.getMessage();
				session.setAttribute("message", "<font color=\"red\"><b>" + msg + "</b></font>");
				String redirectURL = response.encodeRedirectURL("AddNewUser.jsp");
				response.sendRedirect(redirectURL);
				return;
			}
			if( lt == null){
				logger.error("Unknown Error while connecting to LDAP server");
				String msg = "User was not added because of an unknown error while connecting to LDAP server";
				session.setAttribute("message", "<font color=\"red\"><b>" + msg + "</b></font>");
				String redirectURL = response.encodeRedirectURL("AddNewUser.jsp");
				response.sendRedirect(redirectURL);
				return;
			}
			
			// if company doesn't exist in LDAP's "Client" => add the company into "Client"
			if(!lt.companyExists(maps.get("company")[0])){
				try {
					if(!lt.addCompany(maps.get("company")[0])){
						String msg = "The organisation of requesting user doesn't exist and couldn't be added into LDAP's Clients.";
						session.setAttribute("message", "<font color=\"red\"><b>" + msg + "</b></font>");
						String redirectURL = response.encodeRedirectURL("AddNewUser.jsp");
						response.sendRedirect(redirectURL);
						return;
					}
				} catch (NamingException e) {
					
					String msg = "The organisation of requesting user doesn't exist and couldn't be added into LDAP's Clients.";
					msg += " Due to: " + e.getMessage();
					session.setAttribute("message", "<font color=\"red\"><b>" + msg + "</b></font>");
					String redirectURL = response.encodeRedirectURL("AddNewUser.jsp");
					response.sendRedirect(redirectURL);
					return;
				}
			}
			
			// if company doesn't exist in LDAP's "Groups"
			if(!lt.companyExistsAsGroup(maps.get("company")[0])){
				try {
					//  add the company into "Groups"
					if(!lt.addCompanyAsGroup(maps.get("company")[0])){
						// if adding company into group failed
						String msg = "The organisation of requesting user doesn't exist and couldn't be added into LDAP's Groups.";
						session.setAttribute("message", "<font color=\"red\"><b>" + msg + "</b></font>");
						String redirectURL = response.encodeRedirectURL("AddNewUser.jsp");
						response.sendRedirect(redirectURL);
						return;
					}
				} catch (NamingException e) {
					// if adding company into group failed
					String msg = "The organisation of requesting user doesn't exist and couldn't be added into LDAP's Groups. Due to: " + e.getMessage();
					session.setAttribute("message", "<font color=\"red\"><b>" + msg + "</b></font>");
					String redirectURL = response.encodeRedirectURL("AddNewUser.jsp");
					response.sendRedirect(redirectURL);
					return;
				}
			}
						
			// fullname is used to check whether this name exist in LDAP and concerto.
			// and used to add into concerto
			String fullname = "";
			if(maps.get("displayName")[0] != null) 	fullname = maps.get("displayName")[0];
			else 	fullname = maps.get("givenName")[0] + " " + maps.get("sn")[0];
			
			// these variable used for adding a user into ConcertoAPI
			String firstName = maps.get("givenName")[0];
			String lastName = maps.get("sn")[0];
			String userName = maps.get("sAMAccountName")[0];
			String description = maps.get("description")[0];
			String mail = maps.get("mail")[0];
						
						
			// check if username exist in LDAP or Concerto
			boolean usrExistsInLDAP = lt.usernameExists(fullname, maps.get("company")[0]);
			boolean usrExistsInConcerto = false;
			try {
				usrExistsInConcerto = ConcertoAPI.doesUserExist(userName);
			} catch (Exception e) {
				String msg = "Cannot connect to concerto server. " + e.getMessage();
				session.setAttribute("message", "<font color=\"red\"><b>" + msg + "</b></font>");
				String redirectURL = response.encodeRedirectURL("AddNewUser.jsp");
				response.sendRedirect(redirectURL);
				return;
			}
			
			if(usrExistsInLDAP){
				String msg = "Requesting user already exists in LDAP server";
				session.setAttribute("message", "<font color=\"red\"><b>" + msg + "</b></font>");
				String redirectURL = response.encodeRedirectURL("AddNewUser.jsp");
				response.sendRedirect(redirectURL);
				return;
			} else if(usrExistsInConcerto){
				String msg = "Requesting user already exists in Concerto server";
				session.setAttribute("message", "<font color=\"red\"><b>" + msg + "</b></font>");
				String redirectURL = response.encodeRedirectURL("AddNewUser.jsp");
				response.sendRedirect(redirectURL);
				return;
			}		
						
			// ADDING USER ACCOUNT CODE STARTS FROM HERE \\
			
			int clientAccountId = -1;
			try {
				clientAccountId = SupportTrackerJDBC.addClient(maps);
			} catch (SQLException e1) {
				String msg = "User cannot be added to Support Tracker Database, due to: " + e1.getMessage();
				session.setAttribute("message", "<font color=\"red\"><b>" + msg + "</b></font>");
				String redirectURL = response.encodeRedirectURL("AddNewUser.jsp");
				response.sendRedirect(redirectURL);
				return;
				//no need to log, the error has been logged in SupportTrackerJDBC.addClient(maps)
			}		
			
			if( clientAccountId > 0 ){
				maps.put("info", new String[]{Integer.toString(clientAccountId)});	
				
				// add user into ldap server
				boolean addStatus = false;
				
				try {
					addStatus = lt.addUser(maps);
				} catch (Exception e){
					String msg = "User has been added into Support Tracker database. But, it could not be added into LDAP because: " + e.getMessage();
					logger.error(msg, e);
//					response.getWriter().write("false|" + msg);
					session.setAttribute("message", "<font color=\"red\"><b>" + msg + "</b></font>");
					String redirectURL = response.encodeRedirectURL("AddNewUser.jsp");
					response.sendRedirect(redirectURL);
					return;
				}
				lt.close();
				
				if( addStatus ){
					// add user into concerto
					try {
						ConcertoAPI.addClientUser(userName, firstName, lastName, fullname, description, mail, ""+clientAccountId);
					} catch (Exception e) {
						String msg = "User has been added into Support Tracker database and LDAP. But, it could not be added into Concerto Portal because: " + e.getMessage();
//						response.getWriter().write("false|" + msg);
						session.setAttribute("message", "<font color=\"red\"><b>" + msg + "</b></font>");
						String redirectURL = response.encodeRedirectURL("AddNewUser.jsp");
						response.sendRedirect(redirectURL);
						return;
					}
					
					String msg;
					try{
						EmailClient.sendEmailApproved(maps.get("mail")[0], maps.get("displayName")[0], maps.get("sAMAccountName")[0], maps.get("password01")[0]);
						msg = "User "+maps.get("displayName")[0]+" was added successfully with user id: "+maps.get("sAMAccountName")[0];
					} catch (Exception e){
						logger.error("Couldn't send the approval email to " + maps.get("mail")[0], e);
						msg = "Adding a user was done successfully. But it couldn't send the approval email to " + maps.get("mail")[0];
					}
					
					
//					response.getWriter().write("true|"+msg);
					session.setAttribute("message", "<font color=\"green\"><b>"+msg+"</b></font>");
				}else{ // add a user into LDAP is not successful
					// remove the previous added user from Support Tracker DB
					AcceptRequestServlet.deletePreviouslyAddedClientFromSupportTracker(clientAccountId);
					
					String msg = "User "+maps.get("displayName")[0]+" was not added.";
//					response.getWriter().write("false|" + msg);
					session.setAttribute("message", "<font color=\"red\"><b>"+msg+"</b></font>");
				}
				
			}else{
				String msg = "User "+maps.get("displayName")[0]+" was not added to SupportTracker database.";
//				response.getWriter().write("false|" + msg);
				session.setAttribute("message", "<font color=\"red\"><b>" + msg + "</b></font>");
			}
			String redirectURL = response.encodeRedirectURL("AddNewUser.jsp");
			response.sendRedirect(redirectURL);
			
			
			/**
			 * duplicated codes end here
			 */

		/*String username = request.getParameter("sAMAccountName");
		LdapTool lt = new LdapTool();
		String[] password = {"password1"};
		copyMaps.put("password01", password);
		boolean userAdded = lt.addUser(copyMaps);
		lt.close();
		if( userAdded ){
			session.setAttribute("message", "<font color=\"green\"><b>User '"+username+"' has been added successfully.</b></font>");
			logger.debug("User has been added successfully.");
		}else{
			session.setAttribute("message", "<font color=\"red\"><b>Addition of user '"+username+"' has failed.</b></font>");
			logger.debug("Addition of user '"+username+"' has failed.");
		}
		String redirectURL = response.encodeRedirectURL("AddNewUser.jsp");
		response.sendRedirect(redirectURL);*/
	}
}