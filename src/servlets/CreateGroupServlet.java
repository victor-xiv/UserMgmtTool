package servlets;

import java.io.IOException;

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
public class CreateGroupServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	Logger logger = Logger.getRootLogger();
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public CreateGroupServlet() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		//Redirect to 'OrganisationDetails.jsp'
		String redirectURL = response.encodeRedirectURL("OrganisationDetails.jsp");
		response.sendRedirect(redirectURL);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		HttpSession session = request.getSession(true);
		//Get organisation name
		String orgname = request.getParameter("name");
		LdapTool lt = new LdapTool();
		//Add organisation as group
		boolean orgAdded = lt.addCompanyAsGroup(orgname);
		lt.close();
		//If adding as group successful, print success message
		if( orgAdded ){
			session.setAttribute("error", "<font color=\"green\"><b>Organisation '"+orgname+"' has been added successfully.</b></font>");
			logger.info("Organisation has been added successfully.");
		//Otherwise, print error message
		}else{
			session.setAttribute("error", "<font color=\"red\"><b>Addition of organisation '"+orgname+"' has failed.</b></font>");
			logger.info("Addition of organisation '"+orgname+"' has failed.");
		}
		//Forward (redirect with parameters) to OrganisationDetails.jsp
		request.getRequestDispatcher("OrganisationDetails.jsp").forward(request, response);
	}

}

