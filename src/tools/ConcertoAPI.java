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
import com.orionhealth.com_orchestral_portal_webservice_api_7_2_user.Exception;
import com.orionhealth.com_orchestral_portal_webservice_api_7_2_user.UserManagementService;

public class ConcertoAPI {
	private static Logger logger = Logger.getRootLogger();

	
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
		logger.info("Connecting to concerto portal.");
		
		// I disable this ssl verification, because the stpreprod server doesn't contain the proper signed certificate (self-signed only)
		// and I can't configure it correctly on my production machine as well.
		// you can remove this disabling, if you know what how to deal with those certificates. :)
		disableSslVerification();
		
		final QName SERVICE_NAME = new QName(
				"http://www.orionhealth.com/com.orchestral.portal.webservice.api_7_2.user/",
				"UserManagementService");
		
		// get the content of wsdl from web url
		URL wsdlURL = new URL(LdapProperty.getProperty(UserMgmtConstants.CONCERTO_WSDL_URL));
		
		logger.info("Trying to connect to web service with wsdl at: " + wsdlURL);
		UserManagementService ss = new UserManagementService(wsdlURL,
				SERVICE_NAME);
		ComOrchestralPortalWebserviceApi72UserUserManagementService port = ss
				.getComOrchestralPortalWebserviceApi72UserUserManagementServicePort();
		
		return port;
	}
	
	
	
	/**
	 * test the connection to Concerto with the given username.
	 * return true if username exist, false if it doesn't exist
	 * @param username : username that need to be enabled
	 * @throws Exception if there is an exception during the connection or during the updating.
	 * @throws MalformedURLException if wsdl url is not correct.
	 */
	public static boolean testGetClientUser(String username) throws Exception, MalformedURLException{
		ComOrchestralPortalWebserviceApi72UserUserManagementService port = null;
		
		logger.info("About to get endpoint object from portal's webservice");
		
		try {
			port = getConcertoServicePort();
		} catch (MalformedURLException e) {
			logger.error("Failed ot retreive the endpoint object.",e);
			throw new MalformedURLException("Given wsdl url is not correct.");
		}
		
		logger.info("Endpoint object is successfully retrieved from webservice");
		
		User user = null;
		try{
			user = port.getUser(username);
		} catch (Exception e){
			// if username doesn't exist, just return false (don't need to throw an exception)
			if(e.getMessage().contains("No user with ID '"+username+"' currently exists")){
				return false;
			}
			
			logger.error("Failed to retrieve a user from webserivce server.", e);
			throw new Exception("Failed to retrieve a user from webserivce server.");
		}
		
		logger.info("Working with user: " + username);
		
		return true;
	}
	
	
	/**
	 * check if the username exist in Concerto
	 * return true if username exist, false if it doesn't exist
	 * @param username : username that need to be enabled
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
	 * @param username : username that need to be enabled
	 * @throws MalformedURLException if wsdl url is not correct. 
	 * @throws Exception if there is an exception during the connection or during the updating.
	 */
	public static boolean enableNT(String username) throws MalformedURLException, Exception{
		ComOrchestralPortalWebserviceApi72UserUserManagementService port = null;
		
		logger.info("About to get endpoint object from portal's webservice");
		
		try {
			port = getConcertoServicePort();
		} catch (MalformedURLException e) {
			logger.error("Failed ot retreive the endpoint object.",e);
			throw new MalformedURLException("Given wsdl url is not correct.");
		}
		
		logger.info("Endpoint object is successfully retrieved from webservice");
		
		User user = null;
		try{
			user = port.getUser(username);
		} catch (Exception e){
			logger.error("Failed to retrieve a user from webserivce server.", e);
			throw new Exception("Failed to retrieve a user from webserivce server.");
		}
		
		logger.info("Working with user: " + username);
		
		user.setAccountType("LDAP");
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
		
		ComOrchestralPortalWebserviceApi72UserUserManagementService port = null;
		
		logger.info("About to get endpoint object from portal's webservice");
		
		try {
			port = getConcertoServicePort();
		} catch (MalformedURLException e) {
			logger.error("Failed ot retreive the endpoint object.",e);
			throw new MalformedURLException("Given wsdl url is not correct.");
		}
		
		logger.info("Endpoint object is successfully retrieved from webservice");
		
		User user = new User();
		user.setUserId(userName);
		user.setFirstName(firstName);
		user.setLastName(lastName);
		user.setAccountEnabled(true);
		List<String> group = new ArrayList<String>();
		group.add("Clients");
		user.setGroupMemberships(new GroupMemberships(group));
		
		
		
		
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
		
		user.setUserAttributes(new UserAttributes(usrAttrList));
		user.setAccountType("LDAP");
		
		logger.info("About to create a user " + userName + " on webservice server.");
		try {
			port.createUser(user);
		} catch (Exception e) {
			logger.error("User " + userName + " cannot be added to Concerto Portal. An exception is thrown from webservice server.", e);
			throw new Exception("User " + userName + " cannot be added to Concerto Portal. An exception is thrown from webservice server."); 
		}
		
		logger.info("Creating user: " + userName + " on webservice server is done successfully.");
	}
}
