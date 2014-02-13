package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ldap.LdapProperty;
import ldap.LdapTool;
import ldap.UserMgmtConstants;

public class ForgotPasswordServlet extends HttpServlet {
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
		//Handler for writing output
		PrintWriter out;
		
		//Initialise constants
		Properties props = LdapProperty.getConfiguration();
		String conserver = props.getProperty(UserMgmtConstants.CONCERTO_URL);//"supporttracker.orionhealth.com";
		
		//Get output writer and set to output html
		out = response.getWriter();
		response.setContentType("text/html");
		
		//Get parameters
		String password = request.getParameter("password");
		String userDN = request.getParameter("user");
		
		//Print HTML Header
		printHeader(out, conserver);
		
		//Get LdapTool and change Password.
		LdapTool lt = new LdapTool();
		//If successful, print success
		if (lt.changePassword(userDN, password))
			out.println("Password changed successfully.");
		//If fail, print output
		else {
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
