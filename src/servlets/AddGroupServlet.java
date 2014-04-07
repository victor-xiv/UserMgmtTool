package servlets;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import ldap.LdapTool;

/**
 * Servlet implementation class AddOrganisationServlet
 * 
 * Handle assigning a group to an organization
 * This servlet is refered from Organisations.jsp
 */
public class AddGroupServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	Logger logger = Logger.getRootLogger();
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public AddGroupServlet() {
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
		//Get desired group name
		String group = request.getParameter("groupselect");

		LdapTool lt = null;
		boolean orgAdded = false;
		try {
			lt = new LdapTool();
			//Add organisation as group
			orgAdded = lt.addUserToGroup(lt.getDNFromGroup(orgname), lt.getDNFromGroup(group));
			lt.close();
		} catch (Exception e){
			session.setAttribute("error", 
					"<font color=\"red\">"
						+ "<b>Addition of organisation '"+orgname+"' to group "+group+" has failed.</b>"
						+ "<b>Reason of failure: " + e.getMessage() + "</b>"
					+ "</font>");
			request.getRequestDispatcher("OrganisationDetails.jsp").forward(request, response);
			return;
		}


		//If adding as group successful, print success message
		if( orgAdded ){
			session.setAttribute("error", "<font color=\"green\"><b>Organisation '"+orgname+
					"' has been successfully added to group "+group+".</b></font>");
			logger.info("Organisation has been added to group.");
		//Otherwise, print error message
		}else{
			session.setAttribute("error", "<font color=\"red\"><b>Addition of organisation '"+orgname+
					"' to group "+group+" has failed.</b></font>");
			logger.info("Addition of organisation '"+orgname+"' to group "+group+" has failed.");
		}
		//Forward (redirect with parameters) to OrganisationDetails.jsp
		request.getRequestDispatcher("OrganisationDetails.jsp").forward(request, response);
	}

}

