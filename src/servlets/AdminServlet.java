//done


package servlets;

import java.io.IOException;
import java.util.Hashtable;
import java.util.List;

import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import ldap.LdapTool;

import org.apache.log4j.Logger;

import tools.ValidatedRequestHandler;

@SuppressWarnings("serial")
public class AdminServlet extends HttpServlet {
	
	Logger logger = Logger.getRootLogger(); // initiate as a default root logger
	
	public static final String OHGROUPS_ALLOWED_ACCESSED = "Orion Health groups that the user have access right on";
	public static final String WHO_USING_THIS_TOOL = "the current user who is using or querying this tool right now. not the user whom his/her account is being worked on.";
	
	/**
	 * received the request, validate and decrypt the request. Redirect to an appropriate .jsp file
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException
	{	
		logger.debug("AdminServlet about to process Get request: " + request.getParameterMap());
		
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Pragma","no-cache");
		response.setDateHeader ("Expires", 0);

		// get the existing session or create a new one (if there's no existing) 
		HttpSession session = request.getSession(true);
		
		logger.debug("About to descrypt a request from session: " + session.toString());
		
		// validate and decrypt the request
		Hashtable<String, String> parameters = ValidatedRequestHandler.processRequest(request);
		
		logger.debug("Request has been decrypted.");
		
		
		
		// check if the parameters contains userDN
		// then get the user permission levels
		// get the Orion Health groups for this user
		// and set it into the session
		//
		// trying to find useful method in LdapTool to help doing this
		// have looed up to getUserAttributes()
		if(parameters.containsKey("userDN")){
			String unescappedUserDN = parameters.get("userDN");
			if(unescappedUserDN!=null && !unescappedUserDN.trim().isEmpty()){
				try {
					LdapTool lt = new LdapTool();
					List<String> groups = lt.getOrionHealthGroupsThisUserAllowToAccess(unescappedUserDN);
					lt.close();
					session.setAttribute(this.OHGROUPS_ALLOWED_ACCESSED, groups);
					
					// this session attribute is used only in ChangePasswordServlet
					session.setAttribute(this.WHO_USING_THIS_TOOL, unescappedUserDN);
				} catch (NamingException e) {
					parameters.put("error", e.getMessage());
					// we are not logging here because it has been logged  in LdapTool();
				}
			}
			
		}
		
		
		
		

 		// if there is a "error" paramter name, means the validation is incorrect
 		if(parameters.containsKey("error")) {
// 			for( Enumeration<String> e = session.getAttributeNames(); e.hasMoreElements(); ){
// 				session.removeAttribute(e.nextElement());
// 			}
 			if(parameters.containsKey("isAdmin")) parameters.remove("isAdmin");
 		}

		String redirectURL = response.encodeRedirectURL("Error.jsp");

		// check whether the user is an admin user 
		if(parameters.containsKey("isAdmin")){
			if( parameters.get("isAdmin").equals("true") )
				session.setAttribute("isAdmin", parameters.get("isAdmin"));
			else
				session.removeAttribute("isAdmin");
		}else{
			session.removeAttribute("isAdmin");
			session.setAttribute("error", parameters.get("error"));
		}
		if(parameters.containsKey("target")){
			redirectURL = response.encodeRedirectURL(parameters.get("target")+".jsp");
		}
		
		logger.debug("AdminHandler redirected the request to " + redirectURL);
		response.sendRedirect(redirectURL);
	}
	public void doPost(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException
	{
		doGet(request, response);
	}
}
