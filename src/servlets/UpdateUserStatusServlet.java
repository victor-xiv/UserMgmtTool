package servlets;

import javax.naming.NamingException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import tools.ConcertoJDBC;
import tools.SupportTrackerJDBC;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;

import ldap.ErrorConstants;
import ldap.LdapTool;

@SuppressWarnings("serial")
public class UpdateUserStatusServlet extends HttpServlet {
	
	/**
	 * update the user status in LDAP server, Concerto DB and Support Tracker DB
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException
    {
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
			
			boolean status = false;
			if(action.equals("enabling")){
				updated = lt.enableUser(userDN);
				status = true;
			} else if(action.equals("disabling")){
				updated = lt.disableUser(userDN);
				status = false;
			} else {
				lt.close();
				return;
			}
			
			String username = lt.getUsername(userDN);
			try {
				ConcertoJDBC.toggleUserStatus(username, status);
				SupportTrackerJDBC.toggleUserStatus(username, status);
			} catch (SQLException e1) {
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