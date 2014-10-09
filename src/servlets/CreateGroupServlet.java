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
	 * 
	 * Serve the Post request to add Organisation as a group into Ldap Server
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		logger.debug("CreateGroupServlet about to process Post request: " + request.getQueryString());
		
		HttpSession session = request.getSession(true);
		//Get organisation name
		String orgname = request.getParameter("name");
		
		
		LdapTool lt = null;
		try {
			lt = new LdapTool();
		} catch (FileNotFoundException fe){
			session.setAttribute("error", "<font color=\"red\"><b>Addition of organisation '"+orgname+"' has failed. Reason: " + fe.getMessage() + "</b></font>");
			request.getRequestDispatcher("OrganisationDetails.jsp").forward(request, response);				
		} catch (NamingException e) {
			session.setAttribute("error", "<font color=\"red\"><b>Addition of organisation '"+orgname+"' has failed. Reason: " + e.getMessage() + "</b></font>");
			request.getRequestDispatcher("OrganisationDetails.jsp").forward(request, response);
		}

		if( lt != null){
			//Add organisation as group
			boolean orgAdded;
			try {
				orgAdded = lt.addCompanyAsGroup(orgname);
			
				lt.close();
				//If adding as group successful, print success message
				if( orgAdded ){
					session.setAttribute("pass", "<font color=\"green\"><b>Organisation '"+orgname+"' has been added successfully.</b></font>");
					logger.debug("Organisation has been added successfully.");
				//Otherwise, print error message
				}else{
					session.setAttribute("error", "<font color=\"red\"><b>Addition of organisation '"+orgname+"' has failed.</b></font>");
					logger.debug("Addition of organisation '"+orgname+"' has failed.");
				}
			} catch (NamingException e) {
				// dt need to log, it has been logged in lt.addCompanyAsGroup();
				session.setAttribute("error", "<font color=\"red\"><b>Addition of organisation '"+orgname+"' has failed." + e.getMessage() + "</b></font>");
			}
			
			//Forward (redirect with parameters) to OrganisationDetails.jsp
			request.getRequestDispatcher("OrganisationDetails.jsp").forward(request, response);
		}
	}
}

