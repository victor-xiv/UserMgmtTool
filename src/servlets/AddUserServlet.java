package servlets;

import javax.naming.NamingException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;
import javax.xml.rpc.ServiceException;

import org.apache.log4j.Logger;

import tools.ConcertoAPI;
import tools.EmailClient;
import tools.SupportTrackerJDBC;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import ldap.LdapConstants;
import ldap.LdapProperty;
import ldap.LdapTool;

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
		HttpSession session = request.getSession(true);
		Map<String,String[]> paramMaps = (Map<String,String[]>)request.getParameterMap();
		Map<String,String[]> maps = new HashMap<String, String[]>();
		maps.putAll(paramMaps);
		maps.put("isLdapClient", new String[]{"true"});
		maps.put("password01", new String[]{"password1"});
		
		String sAMAccountName = request.getParameter("sAMAccountName").trim();
		logger.info("Username: "+sAMAccountName);
			if( sAMAccountName == null || sAMAccountName.equals("")){
				response.getWriter().write("false|User was not added with invalid username.");
				return;
			}
			
			
			LdapTool lt = null;	  
			int clientAccountId = -1;
			try {
				// connect to ldap server
				lt = new LdapTool();
				// add user into support tracker DB
				clientAccountId = SupportTrackerJDBC.addClient(maps);
			} catch (Exception e1) {
				if(lt != null) lt.close();
				response.getWriter().write("false|User was not added because: " + e1.getMessage());
				return;
			}
			
			
			if( clientAccountId > 0 ){
				maps.put("info", new String[]{Integer.toString(clientAccountId)});	
				
				// add user into ldap server
				boolean addStatus = lt.addUser(maps);
				lt.close();
				
				if( addStatus ){
					String fullname = "";
					if(maps.get("displayName")[0] != null){
						fullname = maps.get("displayName")[0];
					}else{
						fullname = maps.get("givenName")[0] + " " + maps.get("sn")[0];
					}
					
					
					try {
						ConcertoAPI.addClientUser(maps.get("sAMAccountName")[0], Integer.toString(clientAccountId), fullname, maps.get("description")[0], maps.get("mail")[0]);
					} catch (ServiceException e) {
						response.getWriter().write("false|User was not added because: " + e.getMessage());
						return;
					}
					
					
					EmailClient.sendEmailApproved(maps.get("mail")[0], maps.get("displayName")[0], maps.get("sAMAccountName")[0], maps.get("password01")[0]);
					response.getWriter().write("true|User "+maps.get("displayName")[0]+" was added successfully with user id: "+maps.get("sAMAccountName")[0]);
					session.setAttribute("message", "<font color=\"green\"><b>User '"+sAMAccountName+"' has been added successfully.</b></font>");
				}else{
					response.getWriter().write("false|User "+maps.get("displayName")[0]+" was not added.");
					session.setAttribute("message", "<font color=\"red\"><b>Addition of user '"+sAMAccountName+"' has failed.</b></font>");
				}
				
			}else{
				response.getWriter().write("false|User "+maps.get("displayName")[0]+" was not added to database.");
				session.setAttribute("message", "<font color=\"red\"><b>Addition of user '"+sAMAccountName+"' has failed.</b></font>");
			}
			String redirectURL = response.encodeRedirectURL("AddNewUser.jsp");
			response.sendRedirect(redirectURL);

		/*String username = request.getParameter("sAMAccountName");
		LdapTool lt = new LdapTool();
		String[] password = {"password1"};
		copyMaps.put("password01", password);
		boolean userAdded = lt.addUser(copyMaps);
		lt.close();
		if( userAdded ){
			session.setAttribute("message", "<font color=\"green\"><b>User '"+username+"' has been added successfully.</b></font>");
			logger.info("User has been added successfully.");
		}else{
			session.setAttribute("message", "<font color=\"red\"><b>Addition of user '"+username+"' has failed.</b></font>");
			logger.info("Addition of user '"+username+"' has failed.");
		}
		String redirectURL = response.encodeRedirectURL("AddNewUser.jsp");
		response.sendRedirect(redirectURL);*/
	}
}