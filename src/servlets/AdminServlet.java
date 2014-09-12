//done


package servlets;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import tools.LoggerTool;
import tools.ValidatedRequestHandler;

@SuppressWarnings("serial")
public class AdminServlet extends HttpServlet {
	
	Logger logger = Logger.getRootLogger(); // initiate as a default root logger
	
	
	/**
	 * received the request, validate and decrypt the request. Redirect to an appropriate .jsp file
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException
	{	
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Pragma","no-cache");
		response.setDateHeader ("Expires", 0);

		// get the existing session or create a new one (if there's no existing) 
		HttpSession session = request.getSession(true);
		
		logger.info("About to descrypt a request from session: " + session.toString());
		
		// validate and decrypt the request
		Hashtable<String, String> parameters = ValidatedRequestHandler.processRequest(request);
		
		logger.info("Request has been decrypted.");

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
		
		logger.info("AdminHandler redirected the request to " + redirectURL);
		response.sendRedirect(redirectURL);
	}
	public void doPost(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException
	{
		doGet(request, response);
	}
}
