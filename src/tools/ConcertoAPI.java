package tools;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.namespace.QName;

import ldap.LdapProperty;
import ldap.UserMgmtConstants;

import org.apache.log4j.Logger;

import com.orionhealth.com_orchestral_portal_webservice_api_7_2.User;
import com.orionhealth.com_orchestral_portal_webservice_api_7_2.User.GroupMemberships;
import com.orionhealth.com_orchestral_portal_webservice_api_7_2.User.UserAttributes;
import com.orionhealth.com_orchestral_portal_webservice_api_7_2.UserAttributeDto;
import com.orionhealth.com_orchestral_portal_webservice_api_7_2_user.ComOrchestralPortalWebserviceApi72UserUserManagementService;
import com.orionhealth.com_orchestral_portal_webservice_api_7_2_user.UserManagementService;

public class ConcertoAPI {

	
	/**
	 * Disable the SSL Verification. It is helpful when you need to work with
	 * requests on HTTPS but the server doesn't have a proper signed certificate. (e.g. on Preprod server).
	 */
	public static void disableSslVerification(){
		try
	    {
	        // Create a trust manager that does not validate certificate chains
	        TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
	            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
	                return null;
	            }
	            public void checkClientTrusted(X509Certificate[] certs, String authType) {
	            }
	            public void checkServerTrusted(X509Certificate[] certs, String authType) {
	            }
	        }
	        };

	        // Install the all-trusting trust manager
	        SSLContext sc = SSLContext.getInstance("SSL");
	        sc.init(null, trustAllCerts, new java.security.SecureRandom());
	        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

	        // Create all-trusting host name verifier
	        HostnameVerifier allHostsValid = new HostnameVerifier() {
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
	        };

	        // Install the all-trusting host verifier
	        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
	    } catch (NoSuchAlgorithmException e) {
	        e.printStackTrace();
	    } catch (KeyManagementException e) {
	        e.printStackTrace();
	    }
	}
	
	
	
	/**
	 * Connect to stpreprod with webservice 7.2 and return the port object
	 * @return
	 * @throws MalformedURLException
	 */
	public static ComOrchestralPortalWebserviceApi72UserUserManagementService getConcertoServicePort() throws MalformedURLException{
		Logger logger = Logger.getRootLogger(); // initiate as a default root logger
		
		/**
		 * if you receive SSLHandShakeException or any exception related to SSL connection.
		 * please find the cxf configuration at /WET-INF/classes/cxf.xml
		 */
		
		
		logger.debug("Connecting to concerto portal.");
		
		final QName SERVICE_NAME = new QName(
				"http://www.orionhealth.com/com.orchestral.portal.webservice.api_7_2.user/",
				"UserManagementService");
		
		// get the content of wsdl from web url
		URL wsdlURL = new URL(LdapProperty.getProperty(UserMgmtConstants.CONCERTO_WSDL_URL));
		
		logger.debug("Trying to connect to web service with wsdl at: " + wsdlURL);
		UserManagementService ss = new UserManagementService(wsdlURL,
				SERVICE_NAME);
		ComOrchestralPortalWebserviceApi72UserUserManagementService port = ss
				.getComOrchestralPortalWebserviceApi72UserUserManagementServicePort();
		
		logger.debug("connection to concerto portal established");
		
		return port;
	}
	
	
	
	
	
	
	public static User getUser(String username) throws Exception {
		Logger logger = Logger.getRootLogger(); // initiate as a default root logger
		
		/**
		 * if you receive SSLHandShakeException or any exception related to SSL connection.
		 * please find the cxf configuration at /WET-INF/classes/cxf.xml
		 */
		
		
		ComOrchestralPortalWebserviceApi72UserUserManagementService port = null;
		
		logger.debug("About to get endpoint object from portal's webservice");
		
		try {
			port = getConcertoServicePort();
		} catch (MalformedURLException e) {
			logger.error("Failed ot retreive the endpoint object.",e);
			throw new MalformedURLException("Given wsdl url is not correct.");
		}
		
		logger.debug("Endpoint object is successfully retrieved from webservice");
		
		User user = null;
		try{
			user = port.getUser(username);
		} catch (Exception e){
			// if username doesn't exist, just return false (don't need to throw an exception)
			if(e.getMessage().contains("No user")){
				return null;
			}
			
			logger.error("Failed to retrieve a user" + username + " from webserivce server.", e);
			throw new Exception("Failed to retrieve a user from webserivce server.");
		}
		
		return user;
	}
	
	
	
	
	
	/**
	 * test the connection to Concerto with the given username.
	 * return true if username exist, false if it doesn't exist
	 * @param username : username that need to be enabled (it should be the same as LDAP's sAMAccount)
	 * @throws Exception if there is an exception during the connection or during the updating.
	 * @throws MalformedURLException if wsdl url is not correct.
	 */
	public static boolean testGetClientUser(String username) throws Exception, MalformedURLException{
		// not logging, because it logged in getUser() method
		try{
			User user = getUser(username);
			
			if(user == null) return false;
			else return true;
			
		} catch (Exception e) {
			throw new Exception("Failed to retrieve a user from webserivce server.");
		}
	}
	
	
	/**
	 * check if the username exist in Concerto
	 * return true if username exist, false if it doesn't exist 
	 * @param username : username that need to be enabled (it should be the same as LDAP's sAMAccount)
	 * @return true (if username exist in Concerto) false (otherwise)
	 * @throws Exception if there is an exception during the connection or during the updating.
	 * @throws MalformedURLException if wsdl url is not correct.
	 */
	public static boolean doesUserExist(String username) throws MalformedURLException, Exception{
		return testGetClientUser(username);
	}
	
	
	
	/**
	 * set the user's (that given by username) accountType (in concerto database) to LDAP
	 * It will return true, if it does successfully. otherwise, it will throw some exception.
	 * if the user has been set to LDAP (enableNT), then that user will be required to login to portal using password stored in LDAP server
	 * @param username : username that need to be enabled (it should be the same as LDAP's sAMAccount)
	 * @throws MalformedURLException if wsdl url is not correct. 
	 * @throws Exception if there is an exception during the connection or during the updating.
	 */
	public static boolean enableNT(String username) throws MalformedURLException, Exception{
		Logger logger = Logger.getRootLogger(); // initiate as a default root logger
		
		/**
		 * if you receive SSLHandShakeException or any exception related to SSL connection.
		 * please find the cxf configuration at /WET-INF/classes/cxf.xml
		 */
		
		
		ComOrchestralPortalWebserviceApi72UserUserManagementService port = null;
		
		logger.debug("About to get endpoint object from portal's webservice");
		
		try {
			port = getConcertoServicePort();
		} catch (MalformedURLException e) {
			logger.error("Failed ot retreive the endpoint object.",e);
			throw new MalformedURLException("Given wsdl url is not correct.");
		}
		
		logger.debug("Endpoint object is successfully retrieved from webservice");
		
		User user = null;
		try{
			user = port.getUser(username);
		} catch (Exception e){
			logger.error("Failed to retrieve a user from webserivce server.", e);
			throw new Exception("Failed to retrieve a user"+username+" from webserivce server.");
		}
		
		logger.debug("Working with user: " + username);
		
		user.setAccountType("LDAP");
		port.updateUser(user);
		
		return true;
	}
	
	
	/**
	 * set the user's (that given by username) preference to use password stored in LDAP server
	 * If the method run successfully, then that user will be required to login to portal using password stored in LDAP server
	 * It will return true, if it does successfully. otherwise, it will throw some exception.
	 * @param username : username that need to set his/her preference to login into portal using password stored in LDAP server
	 * 					this username should be the same as LDAP's sAMAccount
	 * @throws MalformedURLException if wsdl url is not correct. 
	 * @throws Exception if there is an exception during the connection or during the updating.
	 */
	public static boolean setUserToUsePasswordStoredInLdap(String username) throws MalformedURLException, Exception{
		return enableNT(username);
	}
	
	
	/**
	 * set the user's (that given by username) preference to use password stored in Concerto DB
	 * If the method run successfully, then that user will be required to login to portal using password stored in Concerto DB
	 * It will return true, if it does successfully. otherwise, it will throw some exception.
	 * @param username : username that need to set his/her preference to login into portal using password stored in Concerto DB
	 * 					this username should be the same as LDAP's sAMAccount
	 * @throws MalformedURLException if wsdl url is not correct. 
	 * @throws Exception if there is an exception during the connection or during the updating.
	 */
	public static boolean setUserToUsePasswordStoredInConcerto(String username) throws MalformedURLException, Exception{
		Logger logger = Logger.getRootLogger(); // initiate as a default root logger
		
		/**
		 * if you receive SSLHandShakeException or any exception related to SSL connection.
		 * please find the cxf configuration at /WET-INF/classes/cxf.xml
		 */
		
		
		ComOrchestralPortalWebserviceApi72UserUserManagementService port = null;
		
		logger.debug("About to get endpoint object from portal's webservice");
		
		try {
			port = getConcertoServicePort();
		} catch (MalformedURLException e) {
			logger.error("Failed ot retreive the endpoint object.",e);
			throw new MalformedURLException("Given wsdl url is not correct.");
		}
		
		logger.debug("Endpoint object is successfully retrieved from webservice");
		
		User user = null;
		try{
			user = port.getUser(username);
		} catch (Exception e){
			logger.error("Failed to retrieve a user from webserivce server.", e);
			throw new Exception("Failed to retrieve a user"+username+" from webserivce server.");
		}
		
		logger.debug("Working with user: " + username);
		
		user.setAccountType("Concerto");
		port.updateUser(user);
		
		return true;
	}
	
	
	
	/**
	 * add a user to Concerto. The user detail given by the parameters.
	 * @param username : account username (it should be the same as LDAP's sAMAccount)
	 * @param firstname: user's firstname
	 * @param lastname: user's lastname
	 * @param fullName : user's fristname + " " + lastname
	 * @param description : user's role/position
	 * @param mail : user's contact email address
	 * @param clientAccoutnId : an id that received from support tracker database
	 * @throws Exception if there is an exception during the connection or during the updating.
	 * @throws MalformedURLException if wsdl url is not correct.
	 */
					   
	public static void addClientUser(String userName, String firstName, String lastName, String fullname, String description, String mail, String clientAccountId) throws Exception, MalformedURLException{
		Logger logger = Logger.getRootLogger(); // initiate as a default root logger
		
		/**
		 * if you receive SSLHandShakeException or any exception related to SSL connection.
		 * please find the cxf configuration at /WET-INF/classes/cxf.xml
		 */
		
		
		ComOrchestralPortalWebserviceApi72UserUserManagementService port = null;
		
		logger.debug("About to get endpoint object from portal's webservice");
		
		try {
			port = getConcertoServicePort();
		} catch (MalformedURLException e) {
			logger.error("Failed ot retreive the endpoint object.",e);
			throw new MalformedURLException("Given wsdl url is not correct.");
		}
		
		logger.debug("Endpoint object is successfully retrieved from webservice");
		
		User user = new User();
		user.setUserId(userName);
		user.setFirstName(firstName);
		user.setLastName(lastName);
		user.setAccountEnabled(true);
		List<String> group = new ArrayList<String>();
		group.add("Clients");
		user.setGroupMemberships(new GroupMembershipsExt(group));
		
		
		
		
		// a list of the attributes that will be added to UserAttributeDto
		String[][] attrs = {
				// group	//name			value				display-value
				{"Clients", "ClientID", 	clientAccountId, 	clientAccountId},
				{"Users", 	"Full Name", 	fullname, 			fullname},
				{"Users", 	"Description", 	description, 		description},
				{"Users", 	"E-mail", 		mail,		 		mail},
		};
		
		
		List<UserAttributeDto> usrAttrList= new ArrayList<UserAttributeDto>();
		
		for(int i=0; i<attrs.length; i++){
			UserAttributeDto attr = new UserAttributeDto();
			attr.setGroup(attrs[i][0]);
			attr.setName(attrs[i][1]);
			attr.setValue(attrs[i][2]);
			attr.setDisplayValue(attrs[i][3]);
			usrAttrList.add(attr);
		}
		
		user.setUserAttributes(new UserAttributesExt(usrAttrList));
		user.setAccountType("LDAP");
		
		logger.debug("About to create a user " + userName + " on webservice server.");
		try {
			port.createUser(user);
		} catch (Exception e) {
			logger.error("User " + userName + " cannot be added to Concerto Portal. An exception is thrown from webservice server.", e);
			throw new Exception("User " + userName + " cannot be added to Concerto Portal. An exception is thrown from webservice server."); 
		}
		
		logger.debug("Creating user: " + userName + " on webservice server has been done.");
	}
}




/**
 * GroupMemerships and UserAttributes are the stub classes that provide by
 * the Portal webservice server. But, these stub classes don't have constructors
 * which we need them in this program. So, I wrote these two classes GrouMembershipsExt 
 * and UserAttributes to extends that two classes and it just provides the
 * constructors.
 */
class GroupMembershipsExt extends GroupMemberships{
	public GroupMembershipsExt(List<String> grp){ group = grp; }
}
class UserAttributesExt extends UserAttributes{ 
	public UserAttributesExt(List<UserAttributeDto> atrs){ attribute = atrs; }
}
