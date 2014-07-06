package servlets;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import ldap.LdapTool;

import org.apache.log4j.Logger;

/**
 * Servlet implementation class AddOrganisationServlet
 */
public class AddOrganisationServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	Logger logger = Logger.getRootLogger();
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public AddOrganisationServlet() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		//Redirect to Organisations.jsp
		String redirectURL = response.encodeRedirectURL("Organisations.jsp");
		response.sendRedirect(redirectURL);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		HttpSession session = request.getSession(true);
		//Get organisation
		String orgname = request.getParameter("org");
		orgname = orgname.trim();


		LdapTool lt = null;
		boolean orgAdded = false;
		boolean orgGroupAdded = false;
		try {
			lt = new LdapTool();
			//Add company as client and as group
			orgAdded = lt.addCompany(orgname);
			
			//If successfully added as client
			if(orgAdded){
				orgGroupAdded = lt.addCompanyAsGroup(orgname);
				
				//If successfully added as group, give success message
				if (orgGroupAdded) {
					session.setAttribute("message", "<font color=\"green\"><b>Organisation '"+orgname+"' has been added successfully.</b></font>");
					logger.info("Organisation has been added successfully. This may take a few minutes to appear in the organisational lists");
				//If failed as group, message that group could not be created
				} else {
					session.setAttribute("message", "<font color=\"red\"><b>" +
							"Organisation '"+orgname+"' was added as a client, but the organizational group could not be created.</b></font>");
					logger.info("Organisation added as client, not as group.");
				}
			} else {
				session.setAttribute("message", "<font color=\"red\"><b>Addition of organisation '"+orgname+"' has failed.</b></font>");
				logger.info("Addition of organisation '"+orgname+"' has failed because of an unknown exception");
			}
			
			lt.close();
			
		} catch (FileNotFoundException fe){
			session.setAttribute("message", "<font color=\"red\"><b>Addition of organisation '"+orgname+"' has failed.</b>"
							+ "<b>"+ fe.getMessage() +"</b></font>");
			// don't need to log this exception, because it has been logged in the LdapTool() constructor
			
		} catch (NamingException e) {
			session.setAttribute("message", "<font color=\"red\"><b>Addition of organisation '"+orgname+"' has failed.</b>"
							+ "<b>Because: "+ e.getMessage() +"</b></font>");
			// don't need to log this exception, because it has been logged in the LdapTool() constructor
		}
		
		//Redirect to Organisations.jsp
		String redirectURL = response.encodeRedirectURL("Organisations.jsp");
		response.sendRedirect(redirectURL);
	}

}
