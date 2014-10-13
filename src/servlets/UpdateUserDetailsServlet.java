package servlets;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import ldap.LdapTool;

import org.apache.log4j.Logger;

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
		Map<String,String[]> paramMaps = (Map<String,String[]>)request.getParameterMap();
		
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
		
		if( lt != null){
			String[] updateStatus = lt.updateUser(paramMaps);
			lt.close();
			String redirectURL = "";
			if( updateStatus[0].equals("true") ){
				session.setAttribute("passed", "User has been updated successfully.");
				redirectURL = response.encodeRedirectURL("UserDetails.jsp?dn="+java.net.URLEncoder.encode(updateStatus[1]));
				logger.debug("User has been updated successfully.");
			}else{
				session.setAttribute("failed", updateStatus[1]);
				redirectURL = response.encodeRedirectURL("UserDetails.jsp?dn="+java.net.URLEncoder.encode( paramMaps.get("dn")[0]));
				logger.debug(updateStatus[1]);
			}
			
			response.sendRedirect(redirectURL);
		}
	}
}