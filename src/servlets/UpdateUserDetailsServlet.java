package servlets;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import ldap.LdapTool;

import org.apache.log4j.Logger;

import tools.SupportTrackerJDBC;

@SuppressWarnings("serial")
public class UpdateUserDetailsServlet extends HttpServlet {
	Logger logger = Logger.getRootLogger(); // initiate as a default root logger
	
	public void doGet(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException
    {	
		String redirectURL = response.encodeRedirectURL("UserDetails.jsp?dn="+request.getParameter("dn"));
		response.sendRedirect(redirectURL);
    }
	
	
	/**
	 * Serve Post request to update a Ldap User with the attributes stored in request parameters. 
	 */
	@SuppressWarnings("unchecked")
	public void doPost(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException
    {
		logger.debug("UpdateUserDetailsServlet about to process Post request: " + request.getQueryString());
		
		HttpSession session = request.getSession(true);
		Map<String,String[]> paramMaps = new HashMap<>((Map<String,String[]>)request.getParameterMap());
		
		LdapTool lt = null;
		try {
			lt = new LdapTool();
		} catch (FileNotFoundException fe){
			session.setAttribute("failed", fe.getMessage());
			String redirectURL = response.encodeRedirectURL("UserDetails.jsp?dn="+fe.getMessage());
			response.sendRedirect(redirectURL);					
		} catch (NamingException e) {
			session.setAttribute("failed", e.getMessage());
			String redirectURL = response.encodeRedirectURL("UserDetails.jsp?dn="+e.getMessage());
			response.sendRedirect(redirectURL);
		}
		
		// start updating
		if( lt != null){
			// information needed for updating Support Tracker DB
			String userDN = paramMaps.get("dn")[0];
			String username = lt.getUsername(userDN);
			paramMaps.put("sAMAccountName", new String[]{username});
			String company = lt.getUserCompany(userDN);
			Attributes attrs = lt.getUserAttributes(userDN);
			
			
			
			/**
			 * Note: for the updating on Ldap Account
			 * If you are modifying the display name of an Ldap Account
			 * Its Distinguished Name will also be modified.
			 */

			// updating LDAP account
			String[] updateStatus = lt.updateUser(paramMaps);
			
			// redirect URL
			String redirectURL = redirectURL = response.encodeRedirectURL("UserDetails.jsp?dn="+java.net.URLEncoder.encode(updateStatus[1]));
			
			
			// try to update Support Tracker Account 
			if( updateStatus[0].equals("true") ){ 	
				boolean updateSTDBresult = false;
				try{
					// update Staff account (if the given userDN is an Orion staff)
					if(company.equalsIgnoreCase(lt.ORION_HEALTH_NAME)){
						updateSTDBresult = SupportTrackerJDBC.updateStaffAccount(username, paramMaps);
						
					// update ClientAccount table (if the given userDN is not an Orion staff)
					} else {
						String clientAccountId = attrs.get("Info")==null ? null : attrs.get("Info").get().toString();
						if(clientAccountId != null) updateSTDBresult = SupportTrackerJDBC.updateClientAccount(username, clientAccountId, paramMaps);
					}
					
				} catch (Exception e){
					session.setAttribute("passed", "Ldap account has been updated successfully, but Support Tracker account might have not been updated.");
					logger.debug("failed to update on Support Tracker DB for user: " + updateStatus[1], e);
					lt.close();
					response.sendRedirect(redirectURL);
					return;
				}
				
				
				if(updateSTDBresult) session.setAttribute("passed", "User has been updated successfully.");
				else session.setAttribute("passed", "Ldap account has been updated successfully, but Support Tracker account might have not been updated.");
				logger.debug("User has been updated successfully.");
			}else{
				session.setAttribute("failed", updateStatus[1]);
				logger.debug("failed to update account: " + updateStatus[1]);
			}
			lt.close();
			response.sendRedirect(redirectURL);
		}
	}
}