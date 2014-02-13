package servlets;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import ldap.LdapTool;

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
		LdapTool lt = new LdapTool();
		//Add company as client and as group
		boolean orgAdded = lt.addCompany(orgname);
		boolean orgGroupAdded = lt.addCompanyAsGroup(orgname);
		lt.close();
		//If successfully added as client
		if( orgAdded ){
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
		//Otherwise error message
		}else{
			session.setAttribute("message", "<font color=\"red\"><b>Addition of organisation '"+orgname+"' has failed.</b></font>");
			logger.info("Addition of organisation '"+orgname+"' has failed.");
		}
		//Redirect to Organisations.jsp
		String redirectURL = response.encodeRedirectURL("Organisations.jsp");
		response.sendRedirect(redirectURL);
	}

}
