package servlets;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ldap.LdapProperty;
import ldap.LdapTool;
import ldap.UserMgmtConstants;

import org.apache.log4j.Logger;

public class ForgotPasswordServlet extends HttpServlet {
	
	Logger logger = Logger.getRootLogger(); // initiate as a default root logger
	
private static final long serialVersionUID = 1L;
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ForgotPasswordServlet() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		//If not sent from authorised source, redirect to initial jsp
		response.sendRedirect("ForgotPassword.jsp");
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		logger.debug("ForgotPasswordServlet about to process Post request: "  + request.getParameterMap());
		
		//Handler for writing output
		PrintWriter out;
		
		//Initialise constants
		String conserver = LdapProperty.getProperty(UserMgmtConstants.CONCERTO_URL);//"supporttracker.orionhealth.com";
		
		//Get output writer and set to output html
		out = response.getWriter();
		response.setContentType("text/html");
		
		//Get parameters
		String password = request.getParameter("password");
		String userDN = request.getParameter("user");
		
		//Print HTML Header
		printHeader(out, conserver);
		
		
		logger.debug("Connecting to LDAP server.");
		
		//Get LdapTool and change Password.
		LdapTool lt = null;
		try {
			lt = new LdapTool();
		} catch (FileNotFoundException fe){
			logger.error("Cannot connect to LDAP server.", fe);
			
			out.println("Untable to connect to LDAP sever: " + fe.getMessage() + "<br />"+
					"Please contact the server administrator for assistance or email your "+
					"request to <a href=mailto:support@orionhealth.com>support@orionhealth.com</a>");
		} catch (NamingException e) {
			logger.error("Cannot connect to LDAP server.", e);
			
			out.println("Untable to connect to LDAP sever: " + e.getMessage() + "<br />"+
					"Please contact the server administrator for assistance or email your "+
					"request to <a href=mailto:support@orionhealth.com>support@orionhealth.com</a>");
		}
		
		out.println("I can't find any page that link to this servlet. So, I made this line to make the request fail."
				+ "In order to find it and fix it. So, if you see this text, please report to CSS global team."
				+ "The report should include: what link you clicked to get to this message."
				+ "What page you are looking at. A screenshot of this page and the previous page that link to this page."
				+ "Thanks for your cooperation.");
		
		logger.debug("about to change password for userDN: " + userDN);
		
		//If successful, print success
		if (lt.changePassword(userDN, password)){
			out.println("Password changed successfully.");
			logger.debug("Password changed successfully, for userDN: " + userDN );
		//If fail, print output
		} else {
			logger.debug("Password changed unsuccessfully, for userDN: " + userDN);
			out.println("Unable to change password. The password you entered may be too simple or insecure.<br />"+
					"Please contact the server administrator for assistance or email your "+
					"request to <a href=mailto:support@orionhealth.com>support@orionhealth.com</a>");
		}
		//Close LdapTool and print end of html
		lt.close();
		printFooter(out);
	}
	
	//Print HTML Header
	//Parameters: out - handle to print output
	//conserver - URL of concerto server
	protected void printHeader(PrintWriter out, String conserver) {
		out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">");
		out.println("<html>");
		out.println("<head>");
		out.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\">");
		out.println("<title>Submitting Password Request</title>");
		out.println("<link type=\"text/css\" rel=\"stylesheet\" href=\""+conserver+"/common/css/core.css\">");
		out.println("<link rel=\"stylesheet\" type=\"text/css\" href=\""+conserver+"/common/css/widget.css\">");
		out.println("<link rel=\"stylesheet\" type=\"text/css\" href=\""+conserver+"/css/inputtypes.css\">");
		out.println("<link rel=\"stylesheet\" type=\"text/css\" href=\""+conserver+"/css/concerto.css\">");
		out.println("<script src=\""+conserver+"/javascript/Concerto.js\" type=\"text/javascript\"></script>");
		out.println("<script src=\""+conserver+"/javascript/Dialog.js\" type=\"text/javascript\"></script>");
		out.println("</head>");
		out.println("<body>");
	}
	
	//Print HTML Footer
	//Parameter out - handle to print output
	protected void printFooter(PrintWriter out) {
		out.println("</body>");
		out.println("</html>");
	}

}
