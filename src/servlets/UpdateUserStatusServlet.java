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

import ldap.LdapTool;

@SuppressWarnings("serial")
public class UpdateUserStatusServlet extends HttpServlet {
	public void doGet(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException
    {
		String userDN = request.getParameter("dn");
		String action = request.getParameter("action");
		boolean updated = false;
		if(userDN != null && action != null){
			if(action.equals("enabling")){
				
				
				LdapTool lt = null;
				try {
					lt = new LdapTool();
				} catch (FileNotFoundException fe){
					// TODO Auto-generated catch block
					fe.printStackTrace();					
				} catch (NamingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				// TODO
				if( lt == null){
					
				}
				
				
				
				updated = lt.enableUser(userDN);
				String username = lt.getUsername(userDN);
				ConcertoJDBC.toggleUserStatus(username, true);
				
				try {
					SupportTrackerJDBC.toggleUserStatus(username, true);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
				lt.close();
			}else if(action.equals("disabling")){
				
				
				
				LdapTool lt = null;
				try {
					lt = new LdapTool();
				} catch (FileNotFoundException fe){
					// TODO Auto-generated catch block
					fe.printStackTrace();					
				} catch (NamingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				// TODO
				if( lt == null){
					
				}
				
				
				
				updated = lt.disableUser(userDN);
				String username = lt.getUsername(userDN);
				ConcertoJDBC.toggleUserStatus(username, false);
				
				
				try {
					SupportTrackerJDBC.toggleUserStatus(username, false);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
				lt.close();
			}
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