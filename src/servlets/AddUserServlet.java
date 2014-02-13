package servlets;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import tools.ConcertoAPI;
import tools.EmailClient;
import tools.SupportTrackerJDBC;

import java.io.File;
import java.io.IOException;
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
		
		String sAMAccountName = request.getParameter("sAMAccountName");
		logger.info("Username: "+sAMAccountName);
			if( sAMAccountName == null ){
				response.getWriter().write("false|User was not added with invalid username.");
				return;
			}
			if( sAMAccountName.equals("") ){
				response.getWriter().write("false|User was not added with invalid username.");
				return;
			}
			int clientAccountId = SupportTrackerJDBC.addClient(maps);
			if( clientAccountId > 0 ){
				maps.put("info", new String[]{Integer.toString(clientAccountId)});
				LdapTool lt = new LdapTool();
				boolean addStatus = lt.addUser(maps);
				if( addStatus ){
					String fullname = "";
					if(maps.get("displayName")[0] != null){
						fullname = maps.get("displayName")[0];
					}else{
						fullname = maps.get("givenName")[0] + " " + maps.get("sn")[0];
					}
					ConcertoAPI.addClientUser(maps.get("sAMAccountName")[0], Integer.toString(clientAccountId), fullname, maps.get("description")[0], maps.get("mail")[0]);
					EmailClient.sendEmailApproved(maps.get("mail")[0], maps.get("displayName")[0], maps.get("sAMAccountName")[0], maps.get("password01")[0]);
					response.getWriter().write("true|User "+maps.get("displayName")[0]+" was added successfully with user id: "+maps.get("sAMAccountName")[0]);
					session.setAttribute("message", "<font color=\"green\"><b>User '"+sAMAccountName+"' has been added successfully.</b></font>");
				}else{
					response.getWriter().write("false|User "+maps.get("displayName")[0]+" was not added.");
					session.setAttribute("message", "<font color=\"red\"><b>Addition of user '"+sAMAccountName+"' has failed.</b></font>");
				}
				lt.close();
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