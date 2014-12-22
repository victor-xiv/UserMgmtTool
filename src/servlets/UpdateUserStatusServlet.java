package servlets;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ldap.ErrorConstants;
import ldap.LdapTool;

import org.apache.log4j.Logger;

import tools.ConcertoAPI;
import tools.SupportTrackerJDBC;

@SuppressWarnings("serial")
public class UpdateUserStatusServlet extends HttpServlet {
	
	Logger logger = Logger.getRootLogger(); // initiate as a default root logger
	
	/**
	 * update the user status in LDAP server, Concerto DB and Support Tracker DB
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException
    {
		logger.debug("UpdateUserStatusServlet about to process Get request: " + request.getParameterMap());
		
		String userDN = request.getParameter("dn");
		String action = request.getParameter("action");
		boolean updated = false;
		
		if(userDN != null && action != null){
			// connecting to LDAP server
			LdapTool lt = null;
			try {
				lt = new LdapTool();
			} catch (FileNotFoundException fe){
				response.getWriter().write("false|" + fe.getMessage());
				return;
				// we're not logging here, because it has been logged in LdapTool();
			} catch (NamingException e) {
				response.getWriter().write("false|" + e.getMessage());
				// we're not logging here, because it has been logged in LdapTool();
				return;
			}
			
			// if LDAP can't be connected it should be thrown exception.
			// if there's no exception thrown and lt is null => a weird error occured.
			if (lt == null){
				response.getWriter().write("false|" + ErrorConstants.UNKNOWN_ERR);
				return;
			}
			
			boolean enabled = false;
			if(action.equals("enabling")){
				updated = lt.enableUser(userDN);
				enabled = true;
			} else if(action.equals("disabling")){
				updated = lt.disableUser(userDN);
				enabled = false;
			} else {
				lt.close();
				return;
			}
			
			String username = lt.getUsername(userDN);
			Attributes attrs = lt.getUserAttributes(userDN);
			try {
				String id = attrs.get("Info")==null ? "0" : attrs.get("Info").get().toString();
				
				// if it is a staff account
				if(userDN.contains(lt.ORION_HEALTH_NAME)){
					if(enabled) SupportTrackerJDBC.enableStaffAccount(username);
					else SupportTrackerJDBC.disableStaffAccount(username);
					
				// if client account
				} else {
					SupportTrackerJDBC.toggleUserStatus(username, id, enabled);
				}
				
				if(enabled) new ConcertoAPI().undeleteAccountOfGivenUser(username);
				else new ConcertoAPI().deleteAccountOfGivenUser(username);
			} catch (Exception e1) {
				response.getWriter().write("false|" + e1.getMessage());
				return;
				// we're not logging here, because it has been logged
			}
			
			lt.close();
		}
		
		if(updated){
			if(action.equals("enabling")){
				response.getWriter().write("true|User is enabled.");
			}else{
				response.getWriter().write("true|User is disabled.");
			}
		}else{
			response.getWriter().write("false|This page encountered a problem with the last action.");
		}
    }
	
	public void doPost(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException
    {
		doGet(request, response);
	}
}