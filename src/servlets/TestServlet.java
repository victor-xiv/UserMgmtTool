package servlets;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.Security;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ldap.LdapConstants;
import ldap.LdapProperty;
import ldap.LdapTool;

import org.apache.log4j.Logger;

import tools.ConcertoAPI;
import tools.EmailClient;
import tools.SupportTrackerJDBC;

@SuppressWarnings("serial")
public class TestServlet extends HttpServlet {
	
	// set up logger
	private Logger logger = Logger.getRootLogger();
	
	public void doGet(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException
	{
	}
	
	
	
	public void doPost(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException
	{	
		String rqst = request.getParameter("rqst");
		String rslt = "";
		
		logger.debug("Session: " + request.getSession(true) + " is about to test: " + rqst);
		
		// switch the request according to what the user wants to do
		switch (rqst) {
			case "securityProvider" :  // test the Security Provider (Bouncy Castle)
				boolean result = testSecurityProviderOnHostMachine();
				logger.debug("Security Provider Test has been done. Result is: " + result);
				if(result){
					response.getWriter().write("Successfully installed.");
				} else {
					response.getWriter().write("Could not be found. Please follow this instruction to setup Bouncy Castle: http://woki/display/ServiceDvry/Setup+a+Development+Environment+%28Eclipse+IDE%29+for+UsrMgmt+on+a+Clean+Environment" );
				}
				break;
				
			case "ldapConnection" : 	// test Ldap Connection
				rslt = testLdapConnection();
				logger.debug("Ldap Test has been done. The result is: " + rslt);
				response.getWriter().write(rslt);
				break;
				
			case "portalConnection" :	// test Portal connection
				rslt = testPortalConnection();
				logger.debug("Portal Test has been done. The result is: " + rslt);
				response.getWriter().write(rslt);
				break;
				
			case "supportTrackerDBConnection" :	// test Support Tracker DB connection
				rslt = testSupportTrackerDBConnection();
				logger.debug("Support Tracker DB Test has been done. The result is: " + rslt);
				response.getWriter().write(rslt );
				break;
				
			case "UserMgmt-Version" :		// Getting the UserMgmt-Version (development or built version)
				response.getWriter().write(getUserMgmtVersion());
				break;
				
			case "emailSending" :
				String mailTo = request.getParameter("mailTo");
				response.getWriter().write(testSendingEmailTo(mailTo));
				break;
				
			case "smsSending" :
				String mobile = request.getParameter("mobile");
				mobile = SupportTrackerJDBC.cleanUpAndValidateMobilePhone(mobile);
				if(mobile == null){
					response.getWriter().write("Failed to send an sms because mobile number is not valid.");
				} else {
					response.getWriter().write(testSendingSMS(mobile));
				}
				break;
				
			default:
				response.getWriter().write("Your requested test cannot be understood.");
		}
	}
	
	/**
	 * test sending a txt sms to the given "mobile"
	 * @param mobile : mobile number that this method will send to as a test
	 * @return "Passed message" if the sending is successfull, "Failed message" otherwis or if the given mobile number is invalid
	 */
	public String testSendingSMS(String mobile) {
		logger.debug("about to test sending an sms to: " + mobile);
	
		mobile = SupportTrackerJDBC.cleanUpAndValidateMobilePhone(mobile);
		if(mobile == null)
			return "Failed to send an sms to " + mobile + " because the given mobile number is invalid"; 
		
		try{
			EmailClient.sendSMSto(mobile, "test receipient", "test sms text.");
		} catch (Exception e){
			return "Failed to send an sms to " + mobile + " because: " + e.getMessage();
		}
		return "An sms was sent, please check your mobile phone.";
	}


	/**
	 * test sending an email to the given "mailTo"
	 * @param mailTo : email address that this method will send to as a test
	 * @return "Passed message" if the sending is successfull, "Failed message" otherwise
	 */
	public String testSendingEmailTo(String mailTo){
		logger.debug("about to test sending email to: " + mailTo);
		try{
			EmailClient.sendEmailForApprovedRequestWithManualPsw(mailTo, "Test Reception Name", "Test User Name");
		} catch (Exception e){
			return "Failed to send an emai to " + mailTo + " because: " + e.getMessage();
		}
		logger.debug("finished testing sending email");
		return "An email was sent, please check your email.";
	}
	
	
	
	/**
	 * a helper method help to connect to Support Tracker DB connection
	 * query to get user detail of a userName
	 * the userName is defined in ldap.properties file
	 * @return the result as a string
	 */
	public String testSupportTrackerDBConnection() {
		logger.debug("about to test Connection to Support Tracker DB");
		
		if(LdapProperty.getProperty("error") != null){
			logger.error("ldap.properties file is not found.");
			return "Config file cannot be found";
		}
		
		String userName = LdapProperty.getProperty("spt.searchFor.user");
		try {
			// if the connection was done successful, it will only return user detail as a Map object
			// so, we don't care.
			// we care only if it's throwing an except, which means that the connection is failed.
			List<String> organisations = SupportTrackerJDBC.getOrganisations();
			if(organisations.size() <= 0){
				throw new SQLException();
			}
			
			logger.debug("finished testing Connection to Support Tracker DB");
			
			return "Spt.DB has been done successfully.";
		} catch (SQLException e) {
			return "Spt.DB could not be connected because: " + e.getMessage();
		}
	}
	
	
	
	/**
	 * a helper method to test the Security Provider (Bouncy Castle)
	 * @return true if connection has been done successfully, false otherwise
	 */
	public boolean testSecurityProviderOnHostMachine(){
		logger.debug("about to test Security Provider");
		
		if(Security.getProvider("BC") == null){
			logger.debug("finished testing Security Provider");
			
			return false;
		}
		return true;
	}
	
	
	/**
	 * helper method for test the Ldap Connection.
	 * This method will try to create a test company into Clients and Groups
	 * Then it creates a test user who belongs to this test company.
	 * If the steps have been successfully executed, it will delete the company and the user.
	 * @return: "Ldap was successful connected" if all the adding and deleting processes were done successfully.
	 * Otherwise, it will return the state where the code has been successfully executed and which process
	 * has been failed.
	 */
	public String testLdapConnection(){
		logger.debug("about to test Ldap Connection");
		
		if(LdapProperty.getProperty("error") != null){
			logger.error("ldap.properties file is not found.");
			return "Config file cannot be found";
		}
		
		// build attributes for a tested user
		Map<String, String[]> maps = new HashMap<String, String[]>();
		maps.put("givenName", new String[]{LdapProperty.getProperty("ldap.test.givenName")});
		maps.put("company", new String[]{LdapProperty.getProperty("ldap.test.company")});
		maps.put("sAMAccountName", new String[]{LdapProperty.getProperty("ldap.test.sAMAccountName")});
		maps.put("sn", new String[]{ LdapProperty.getProperty("ldap.test.sn")  });
		maps.put("displayName", new String[]{LdapProperty.getProperty("ldap.test.displayName")});
		maps.put("description", new String[]{ LdapProperty.getProperty("ldap.test.description") });
		maps.put("department", new String[]{ LdapProperty.getProperty("ldap.test.department") });
		maps.put("streetAddress", new String[]{ LdapProperty.getProperty("ldap.test.streetAddress") });
		maps.put("l", new String[]{ LdapProperty.getProperty("ldap.test.l") });
		maps.put("st", new String[]{ LdapProperty.getProperty("ldap.test.st") });
		maps.put("postalCode", new String[]{ LdapProperty.getProperty("ldap.test.postalCode") });
		maps.put("c", new String[]{ LdapProperty.getProperty("ldap.test.c") });
		maps.put("telephoneNumber", new String[]{ LdapProperty.getProperty("ldap.test.telephoneNumber") });
		maps.put("facsimileTelephoneNumber", new String[]{ LdapProperty.getProperty("ldap.test.facsimileTelephoneNumber") });
		maps.put("mobile", new String[]{ LdapProperty.getProperty("ldap.test.mobile") });
		maps.put("mail", new String[]{LdapProperty.getProperty("ldap.test.mail")});
		maps.put("password01", new String[]{"password"});
		maps.put("isLdapClient", new String[]{"true"});
		
		String result = "";
		
		// connectin to ldap
		LdapTool lt = null;
		try {
			lt = new LdapTool();
		} catch (FileNotFoundException | NamingException e) {
			result += " Ldap cannot be connected because: " + e.getMessage();
			if(lt!=null) lt.close();
			return result;
		}
		
		
		
		String userDN = "CN=" + maps.get("displayName")[0] + ",OU=" + maps.get("company")[0] + ",OU=Clients,DC=orion,DC=dmz";
		String baseDN = LdapProperty.getProperty(LdapConstants.BASEGROUP_DN);
		if (baseDN==null) baseDN = "OU=Groups,DN=orion,DN=dmz";
		String companyGroupDN = "CN="+ maps.get("company")[0] +","+baseDN;
		baseDN = LdapProperty.getProperty(LdapConstants.CLIENT_DN);
		if (baseDN==null) baseDN = "OU=Clients,DC=orion,DC=dmz";
		String companyDN = "ou="+ maps.get("company")[0] +","+baseDN;
		
		
		try{// try to delete everything before start testing
			
			lt.deleteUser(userDN);
			lt.deleteGroupCompany(companyGroupDN);
			lt.deleteCompany(companyDN);
		} catch (Exception e){
			// we are not testing the deletion. so, we are not doing anything with this exception
		}
			
		
		result += " Ldap is connected successfully.";
		
		// try to check company exist and add it (if it doesn't exist)
		if(!lt.companyExists(maps.get("company")[0])){
			try {
				if(!lt.addCompany(maps.get("company")[0])){
					result += " The tested company: \"" + maps.get("company")[0]  + "\" cannot be added.";
					return result;
				}
			} catch (NamingException e) {
				result += " The tested company: \"" + maps.get("company")[0]  + "\" cannot be added because: " + e.getMessage();
				if(lt!=null) lt.close();
				return result;
			}
		
			result += " The tested company was added successfully.";
		}

		// try to check company exist in in Groups and add it (if it doesn't exist)
		if (!lt.companyExistsAsGroup(maps.get("company")[0])) {
			try {
				if (!lt.addCompanyAsGroup(maps.get("company")[0])) {
					result += " But cannot be added into groups.";
					if(lt!=null) lt.close();
					return result;
				}
			} catch (NamingException e) {
				result += " But cannot be added into groups because: " + e.getMessage();
				if(lt!=null) lt.close();
				return result;
			}
			result += " And was added successfully into groups.";
		}
		
		// adding the test user into LDAP
		try {
			if(!lt.addUser(maps)){
				result += " the tested user: \"" + maps.get("sAMAccountName")[0] + "\" couldnot be added.";
				if(lt!=null) lt.close();
				return result;
			}
		} catch (Exception e) {
			result += " the tested user: \"" + maps.get("sAMAccountName")[0] + "\" couldnot be added because: " + e.getMessage();
			if(lt!=null) lt.close();
			return result;
		}
		
		// If adding user and company processes were successful.
		
		// delete the user
		if(!lt.deleteUser(userDN)){
			result = "Ldap is succesfully connected. But, the tested user and company cannot be deleted. Please delete them manually (for next time test). "
					+ "Username is: \"" + maps.get("sAMAccountName")[0] + "\" and company name is: \"" + maps.get("company")[0] + "\"";
			if(lt!=null) lt.close();
			return result;
		}
		
		// delete the company from the Groups folder
		if(!lt.deleteGroupCompany(companyGroupDN)){
			result = "Ldap is sucessfully connected. But, the tested group cannot be deleted. Please delete it manually (for next time test). That group name is: \"" + maps.get("company")[0] + "\"";
			if(lt!=null) lt.close();
			return result;
		}
		
		// delete the company from the Clients folder
		if(!lt.deleteCompany(companyDN)){
			result = "Ldap is sucessfully connected. But, the tested group cannot be deleted. Please delete it manually (for next time test). That group name is: \"" + maps.get("company")[0] + "\"";
			if(lt!=null) lt.close();
			return result;
		}
		
		if(lt!=null) lt.close();
		return "Ldap was successfully connected.";
	}
	
	
	
	
	/**
	 * A helper method that test portal connection.
	 * It'll try connecting to Portal and search for a username.
	 * the username is defined in the ldap.properties file
	 * @return the result as a string
	 */
	public String testPortalConnection(){
		logger.debug("about to test Portal Connection");
		
		if(LdapProperty.getProperty("error") != null){
			logger.error("ldap.properties file is not found.");
			return "Config file cannot be found";
		}
		
		try {
			// if the connection was done successful, it will only return true or false.
			// so, we don't care.
			// we care only if it's throwing an except, which means that the connection is failed.
			boolean result = new ConcertoAPI().testGetClientUser(LdapProperty.getProperty("portal.searchFor.user"));
			return "Portal has been connected successfully.";

		} catch (Exception e) {
			logger.error("Portal cannot be connected", e);
			return "Portal could not be connected because: " + e.getMessage();
		}
	}
	
	
	/**
	 * a helper method that look at file /META-INF/MANIFEST.MF
	 * and return the version value of the attribute "UserMgmt-Version"
	 * @return the version value of the attribute "UserMgmt-Version" from /META-INF/MANIFEST.MF file
	 */
	public String getUserMgmtVersion(){
		logger.debug("about to read User Mgmt Version from MANIFEST file");
		ServletContext application = getServletConfig().getServletContext();
		InputStream inputStream = application.getResourceAsStream("/META-INF/MANIFEST.MF");
		Manifest manifest;
		try {
			manifest = new Manifest(inputStream);
			Attributes attributes = manifest.getMainAttributes();
	        String version = attributes.getValue("UserMgmt-Version");
	        logger.debug("finished reading User Mgmt Version from MANIFEST file. This version is: " + version);
	        return version;
		} catch (IOException e) {
			return "Version is not found.";
		}
		
	}
}