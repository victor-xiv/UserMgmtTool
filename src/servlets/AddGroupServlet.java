package servlets;

import java.io.IOException;
import java.util.List;
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
import javax.servlet.http.HttpSession;

import ldap.LdapTool;

import org.apache.commons.lang3.StringEscapeUtils;
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
			logger.debug("AddGroupServlet about to process Post request: " + request.getQueryString());
					
			// Get new group name (this is not a dn-name of this group. its just a name)
			String newGroupName = request.getParameter("newGroupName").trim();
			// Get thisGroupDN (dn-name of this group)
			String thisGroupDN = request.getParameter("thisGroupDN").trim();

			NamingEnumeration namingEnum = null;
			HttpSession session = request.getSession(true);
			List<String> ohGroupsThisUserCanAccess = (List<String>)session.getAttribute(AdminServlet.OHGROUPS_ALLOWED_ACCESSED);
			Set<String> baseGroups = null;
			Attribute attr = null;
			LdapTool lt = null;
			boolean orgAdded = false;

			StringBuffer sfXml = new StringBuffer();
			response.setContentType("text/xml");
			sfXml.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
			sfXml.append("<response>");

			try {
				lt = new LdapTool();
				// Add newGroupName into thisGroupDN
				orgAdded = lt.addGroup1InToGroup2(
						thisGroupDN, lt.getDNFromGroup(newGroupName));

				// baseGroups will be used to list all groups that thisGroupDN doesn't belong to
				// namingEnum will be used to list all groups that thisGroupDN belong to
				Attributes attrs = lt.getGroupAttributes(LdapTool.getCNValueFromDN(thisGroupDN));
				attr = attrs.get("memberOf");
				if(ohGroupsThisUserCanAccess == null){
					baseGroups = lt.getBaseGroups(); // all the groups stored in LDAP, used to create a list of notMemberOf
				} else {
					baseGroups = lt.getBaseGroupsWithGivenOHGroupsAllowedToBeAccessed(ohGroupsThisUserCanAccess);
				}

				lt.close();
			} catch (Exception e) {
				// prepareing a failed response to client
				String value = String
						.format("<failed>Addition of group '%s' to group %s has failed. Reason of the failure: %s.</failed>",
								StringEscapeUtils.escapeXml(thisGroupDN), StringEscapeUtils.escapeXml(newGroupName), StringEscapeUtils.escapeXml(e.getMessage()));
				sfXml.append(value);

				logger.debug("Addition of organisation '" + thisGroupDN
						+ "' to group " + newGroupName + " has failed.");

				sfXml.append("</response>");
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
									.format("<memberOf> <dn>%s</dn> <name>%s</name> </memberOf>",
											StringEscapeUtils.escapeXml(thisDN), StringEscapeUtils.escapeXml(name));
							sfXml.append(value);
						}
					}
				} catch (NamingException e) {
					// preapring a failed response to client
					String value = String
							.format("<failed>Addition of group '%s' to group %s has been done successfully. But, the groups list cannot be generated because of the groups iteration has failed. Please refresh the page.</failed>",
									StringEscapeUtils.escapeXml(thisGroupDN), StringEscapeUtils.escapeXml(newGroupName));
					sfXml.append(value);
				}
				
				// add all notMemberOf into xml that will response to client
				for (String bsGroup : baseGroups) {
					String value = String.format(
							"<notMemberOf> %s </notMemberOf>", StringEscapeUtils.escapeXml(bsGroup));
					sfXml.append(value);
				}

				String value = String
						.format("<passed>'User %s' has been successfully added to group %s.</passed>",
								StringEscapeUtils.escapeXml(thisGroupDN), StringEscapeUtils.escapeXml(newGroupName));
				sfXml.append(value);

				logger.debug(String.format("Group %s has been added to group %s.",
						StringEscapeUtils.escapeXml(thisGroupDN), StringEscapeUtils.escapeXml(newGroupName)));

				// Otherwise, log the error and preapre a failed response to client
			} else {
				String value = String
						.format("<failed>Addition of group '%s' to group %s has failed.</failed>",
								StringEscapeUtils.escapeXml(thisGroupDN), StringEscapeUtils.escapeXml(newGroupName));
				sfXml.append(value);

				logger.debug("Addition of group '" + thisGroupDN + "' to group "
						+ newGroupName + " has failed.");
			}

			sfXml.append("</response>");
			response.getWriter().write(sfXml.toString());
			response.getWriter().flush();
			response.getWriter().close();
	}

}

