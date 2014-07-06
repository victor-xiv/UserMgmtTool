package servlets;

import javax.naming.NamingException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import tools.LoggerTool;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import ldap.LdapTool;

@SuppressWarnings("serial")
public class UpdateUserDetailsServlet extends HttpServlet {
	Logger logger = LoggerTool.setupDefaultRootLogger();
	
	public void doGet(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException
    {
		logger = LoggerTool.setupRootLogger(request);
		
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
		logger = LoggerTool.setupRootLogger(request);
		
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
			if( updateStatus[0].equals("true") ){
				session.setAttribute("passed", "User has been updated successfully.");
				logger.info("User has been updated successfully.");
			}else{
				session.setAttribute("failed", updateStatus[1]);
				logger.info(updateStatus[1]);
			}
			
			String redirectURL = response.encodeRedirectURL("UserDetails.jsp?dn="+java.net.URLEncoder.encode(updateStatus[1]));
			response.sendRedirect(redirectURL);
		}
	}
}