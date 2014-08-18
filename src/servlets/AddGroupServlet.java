package servlets;

import java.io.IOException;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.ldap.Rdn;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ldap.LdapTool;

import org.apache.log4j.Logger;

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
			// Get new group name (this is not a dn-name of this group. its just a name)
			String newGroupName = request.getParameter("newGroupName").trim();
			// Get thisGroupDN (dn-name of this group)
			String thisGroupDN = request.getParameter("thisGroupDN").trim();

			NamingEnumeration namingEnum = null;
			Set<String> baseGroups = null;
			Attribute attr = null;
			LdapTool lt = null;
			boolean orgAdded = false;

			StringBuffer sfXml = new StringBuffer();
			response.setContentType("text/xml");
			sfXml.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n");
			sfXml.append("<response>\n");

			try {
				lt = new LdapTool();
				// Add newGroupName into thisGroupDN
				orgAdded = lt.addGroup1InToGroup2(
						thisGroupDN, lt.getDNFromGroup(newGroupName));

				// baseGroups will be used to list all groups that thisGroupDN doesn't belong to
				// namingEnum will be used to list all groups that thisGroupDN belong to
				Attributes attrs = lt.getGroupAttributes(LdapTool.getCNValueFromDN(thisGroupDN));
				attr = attrs.get("memberOf");
				baseGroups = lt.getBaseGroups(); // get all the groups that
													// contained in LDAP server

				lt.close();
			} catch (Exception e) {
				// prepareing a failed response to client
				String value = String
						.format("\t<failed>Addition of group '%s' to group %s has failed. Reason of the failure: %s.</failed>\n",
								thisGroupDN, newGroupName, e.getMessage());
				sfXml.append(value);

				logger.info("Addition of organisation '" + thisGroupDN
						+ "' to group " + newGroupName + " has failed.");

				sfXml.append("</response>\n");
				response.getWriter().write(sfXml.toString());
				response.getWriter().flush();
				response.getWriter().close();
				return;
			}

			// If adding as group successful
			if (orgAdded && baseGroups != null) {
				try {
					if(attr != null){
						namingEnum = attr.getAll();
						// add memberOf into the xml that will response to client
						while (namingEnum.hasMore()) {
							String thisDN = (String) namingEnum.next();
							thisDN = (String) Rdn.unescapeValue(thisDN);
							String name = LdapTool.getCNValueFromDN(thisDN);
							// remove memberOf from baseGroups (so, after this loop baseGroups cotains only notMemberOf)
							baseGroups.remove(name);
							String value = String
									.format("\t<memberOf> <dn>%s</dn> <name>%s</name> </memberOf>\n",
											thisDN, name);
							sfXml.append(value);
						}
					}
				} catch (NamingException e) {
					// preapring a failed response to client
					String value = String
							.format("\t<failed>Addition of group '%s' to group %s has been done successfully. But, the groups list cannot be generated because of the groups iteration has failed. Please refresh the page.</failed>\n",
									thisGroupDN, newGroupName);
					sfXml.append(value);
				}
				
				// add all notMemberOf into xml that will response to client
				for (String bsGroup : baseGroups) {
					String value = String.format(
							"\t<notMemberOf> %s </notMemberOf>\n", bsGroup);
					sfXml.append(value);
				}

				String value = String
						.format("\t<passed>'User %s' has been successfully added to group %s.</passed>\n",
								thisGroupDN, newGroupName);
				sfXml.append(value);

				logger.info(String.format("Group %s has been added to group %s.",
						thisGroupDN, newGroupName));

				// Otherwise, log the error and preapre a failed response to client
			} else {
				String value = String
						.format("\t<failed>Addition of group '%s' to group %s has failed.</failed>\n",
								thisGroupDN, newGroupName);
				sfXml.append(value);

				logger.info("Addition of group '" + thisGroupDN + "' to group "
						+ newGroupName + " has failed.");
			}

			sfXml.append("</response>\n");
			response.getWriter().write(sfXml.toString());
			response.getWriter().flush();
			response.getWriter().close();
	}

}

