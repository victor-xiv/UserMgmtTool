package servlets;

import java.io.IOException;
import java.util.Set;

import javax.naming.NamingEnumeration;
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
public class RemoveAGroupFromUserServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	Logger logger = Logger.getRootLogger(); // initiate as a default root logger
       

	/**
	 * 
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) {
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		//Get user DN 
		String userDN = request.getParameter("userDN").trim(); 
		//Get desired group DN
		String groupDN = request.getParameter("groupDN").trim();
		
		NamingEnumeration namingEnum = null;
		Set<String> baseGroups = null;
		LdapTool lt = null;
		
		// create xml string that stores data that need to be responded to client
		StringBuffer sfXml = new StringBuffer();
		response.setContentType("text/xml");
	    sfXml.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n");
	    sfXml.append("<response>\n");
		
		try {
			
			lt = new LdapTool();
			//remove group from user
			lt.removeUserFromAGroup(userDN, groupDN);
			
			Attributes attrs = lt.getUserAttributes(userDN); // all the groups that are memberOf userDN
			Attribute attr = attrs.get("memberOf");
			baseGroups = lt.getBaseGroups(); // all the groups stored in LDAP, used to create a list of notMemberOf
			lt.close();
			
			if(attr != null){
				namingEnum = attr.getAll();
				// remove all the memberOf groups from baseGroups
				// so, the result, baseGroups will contains only those groups that userDN is not a memberOf that groups
				while(namingEnum.hasMore()){
					String thisDN = (String) namingEnum.next();
					thisDN = (String) Rdn.unescapeValue(thisDN);
					String name = LdapTool.getCNValueFromDN(thisDN);
					baseGroups.remove(name);
					
					String value = String.format("\t<memberOf> <dn>%s</dn> <name>%s</name> </memberOf>\n", thisDN, name);
					sfXml.append(value);
				}
			}
			
			// assign those notMemberOfGroups into the xml response string
			for(String bsGroup : baseGroups){
				String value = String.format("\t<notMemberOf> %s </notMemberOf>\n", bsGroup);
				sfXml.append(value);
			}
			
			// If removal is successful, response with a "passed" tag.
			String value = String.format("\t<passed></passed>\n");
		    sfXml.append(value);
		    
			logger.info(String.format("Group '%s' has been removed from user '%s' successfully.", groupDN, userDN));
			
		} catch (Exception e){
			// if failed
			String value = String.format("\t<failed>Reason of the failure: %s.</failed>\n", e.getMessage());
			sfXml.append(value);
			
			logger.info("Removal of user: '" + userDN + "' from group " + groupDN + " has failed.", e);
		}

		sfXml.append("</response>\n");
	    response.getWriter().write(sfXml.toString());
	    response.getWriter().flush();
	    response.getWriter().close();
	}

}

