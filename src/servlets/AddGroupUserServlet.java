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
 * Handle assigning a group to a user
 * This servlet is refered from UserDetails.jsp
 */
public class AddGroupUserServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	Logger logger = Logger.getRootLogger();
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public AddGroupUserServlet() {
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
		String dn = request.getParameter("dn").trim();
		//Get desired group name
		String group = request.getParameter("groupselect").trim();


		LdapTool lt = null;
		boolean userAdded = false;
		try {
			lt = new LdapTool();
			//Add organisation as group
			userAdded = lt.addUserToGroup(dn, lt.getDNFromGroup(group));
			lt.close();
		} catch (Exception e){
			session.setAttribute("failed", 
					"<font color=\"red\">"
						+ "<b>Addition of organisation '"+dn+ "' to group "+group+" has failed.</b>"
						+ "<b>Reason of failure: " + e.getMessage() + "</b>"
					+ "</font>");
			request.getRequestDispatcher("UserDetails.jsp").forward(request, response);
			return;
		}

		// If adding as group successful, print success message
		if (userAdded) {
			session.setAttribute("passed", "<font color=\"green\"><b>User '"
					+ dn + "' has been successfully added to group " + group
					+ ".</b></font>");
			logger.info("Organisation has been added to group.");

		// Otherwise, log the error
		} else {
			session.setAttribute("failed",
					"<font color=\"red\"><b>Addition of organisation '" + dn
							+ "' to group " + group + " has failed.</b></font>");
			logger.info("Addition of organisation '" + dn + "' to group "
					+ group + " has failed.");
		}
		// Forward (redirect with parameters) to OrganisationDetails.jsp
		request.getRequestDispatcher("UserDetails.jsp").forward(request,
				response);

	}

}

