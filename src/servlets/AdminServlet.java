package servlets;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import tools.ValidatedRequestHandler;

@SuppressWarnings("serial")
public class AdminServlet extends HttpServlet {
	public void doGet(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException
	{
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Pragma","no-cache");
		response.setDateHeader ("Expires", 0);

		Hashtable<String, String> parameters = ValidatedRequestHandler.processRequest(request);
		HttpSession session = request.getSession(true);


 		
 		
 		// TO DO need to check and test this block:
 		/*
 		// if there is a "error" paramter name, means the validation is incorrect
 		
 		if(parameters.containsKey("error")) {
 		
 			for( Enumeration<String> e = session.getAttributeNames(); e.hasMoreElements(); ){
 				session.removeAttribute(e.nextElement());
 			}
 			if(parameters.containsKey("isAdmin")) parameters.remove("isAdmin");
 		}
 		*/
 		
 		
		
 		// TO DO: produce a better (more detail) exception message
 		
 	


		String redirectURL = response.encodeRedirectURL("Error.jsp");
		if(parameters.containsKey("isAdmin")){
			if( parameters.get("isAdmin").equals("true") )
				session.setAttribute("isAdmin", parameters.get("isAdmin"));
			else
				session.removeAttribute("isAdmin");
		}else{
			session.removeAttribute("isAdmin");
			session.setAttribute("error", "This page can only be accessed from within Concerto.");
		}
		if(parameters.containsKey("target")){
			redirectURL = response.encodeRedirectURL(parameters.get("target")+".jsp");
		}
		response.sendRedirect(redirectURL);
	}
	public void doPost(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException
	{
		doGet(request, response);
	}
}
