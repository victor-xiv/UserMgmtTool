package servlets;

import java.io.IOException;
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

import org.apache.log4j.Logger;

import ldap.LdapTool;

/**
 * Servlet implementation class AddOrganisationServlet
 * 
 * Handle assigning a group to an organization
 * This servlet is refered from Organisations.jsp
 */
public class RemoveAGroupFromUserServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	Logger logger = Logger.getRootLogger();
       

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
		
		StringBuffer sfXml = new StringBuffer();
		response.setContentType("text/xml");
	    sfXml.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n");
	    sfXml.append("<response>\n");
	    
	    
	    int test = (int)(Math.random() * 50);
		
		try {
			
			if(test % 2 == 0) throw new Exception("my test");
			
			
			lt = new LdapTool();
			//remove group from user
			lt.removeUserFromAGroup(userDN, groupDN);
			
			Attributes attrs = lt.getUserAttributes(userDN);
			Attribute attr = attrs.get("memberOf");
			namingEnum = attr.getAll();
			baseGroups = lt.getBaseGroups();
			lt.close();
			
			while(namingEnum.hasMore()){
				String thisDN = (String) namingEnum.next();
				String name = thisDN.split(",")[0].split("=")[1];
				baseGroups.remove(name);
			}
			
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

