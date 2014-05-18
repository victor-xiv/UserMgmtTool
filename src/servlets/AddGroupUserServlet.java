package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import ldap.LdapTool;

import org.apache.log4j.Logger;

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
		//Get organisation name
		String dn = request.getParameter("dn").trim(); 
		//Get desired group name
		String group = request.getParameter("groupselect").trim();
		
		NamingEnumeration namingEnum = null;
		Set<String> baseGroups = null;
		LdapTool lt = null;
		boolean userAdded = false;
		
		
		StringBuffer sfXml = new StringBuffer();
		response.setContentType("text/xml");
	    sfXml.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n");
	    sfXml.append("<response>\n");
	    
	    
		try {
			lt = new LdapTool();
			//Add organisation as group
			userAdded = lt.addUserToGroup(dn, lt.getDNFromGroup(group));
			
			// baseGroups will be used to list all groups that this user doesn't belong to
			// namingEnum will be used to list all groups that this user belong to
			Attributes attrs = lt.getUserAttributes(dn);
			Attribute attr = attrs.get("memberOf");
			namingEnum = attr.getAll();
			baseGroups = lt.getBaseGroups();
			
			lt.close();
		} catch (Exception e){
			String value = String.format("\t<failed>Addition of organisation '%s' to group %s has failed. Reason of the failure: %s.</failed>\n", dn, group, e.getMessage());
		    sfXml.append(value);

			logger.info("Addition of organisation '" + dn + "' to group " + group + " has failed.");
			
			sfXml.append("</response>\n");
		    response.getWriter().write(sfXml.toString());
		    response.getWriter().flush();
		    response.getWriter().close();
			return;
		}

		// If adding as group successful, print success message
		if (userAdded && namingEnum != null && baseGroups != null) {
			try {
				while(namingEnum.hasMore()){
					String thisDN = (String) namingEnum.next();
					String name = thisDN.split(",")[0].split("=")[1];
					baseGroups.remove(name);
					String value = String.format("\t<memberOf> <dn>%s</dn> <name>%s</name> </memberOf>\n", thisDN, name);
					sfXml.append(value);
				}
			} catch (NamingException e) {
				String value = String.format("\t<failed>Addition of organisation '%s' to group %s has been done successfully. But, the groups list cannot be generated because of the groups iteration has failed. Please refresh the page.</failed>\n", dn, group);
			    sfXml.append(value);
			}
					
			for(String bsGroup : baseGroups){
				String value = String.format("\t<notMemberOf> %s </notMemberOf>\n", bsGroup);
				sfXml.append(value);
			}
	
		    
		    String value = String.format("\t<passed>'User %s' has been successfully added to group %s.</passed>\n", dn, group);
		    sfXml.append(value);
		    
			logger.info("Organisation has been added to group.");

		// Otherwise, log the error
		} else {
			String value = String.format("\t<failed>Addition of organisation '%s' to group %s has failed.</failed>\n", dn, group);
		    sfXml.append(value);
		    
			logger.info("Addition of organisation '" + dn + "' to group "
					+ group + " has failed.");
		}
		
		
	    sfXml.append("</response>\n");
	    response.getWriter().write(sfXml.toString());
	    response.getWriter().flush();
	    response.getWriter().close();
	    

		
	}

}

