package tools;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.namespace.QName;
import javax.xml.rpc.ServiceException;

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

//import com.concerto.webservice.user.UserManagerServiceSEI;
//import com.concerto.webservice.user.UserManagerServiceSEIServiceLocator;
//import com.concerto.webservice.user.dto.UserAttributeDTO;
//import com.concerto.webservice.user.dto.UserDTO;
//import com.concerto.webservice.user.exception.UserManagerServiceException;
//import com.orchestral.webservice.handler.PasswordCallbackHandler;

public class ConcertoAPI {
	private static Logger logger = Logger.getRootLogger();

	
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
		

		// if we don't want to get content directly from web url
		// we can set it to get from wsdl file that we have saved into hdd. 
		// below is the code to do so
//		URL wsdlURL;
//		File wsdlFile = new File(
//				"./concerto.wsdl");
//		if (wsdlFile.exists()) {
//			wsdlURL = wsdlFile.toURL();
//		} else {
//			wsdlURL = new URL(
//					"https://192.168.21.69/portal/ws/com.orchestral.portal.webservice.api_7_2.user.UserManagementService?wsdl");
//		}
		
		logger.info("Trying to connect to web service with wsdl at: " + wsdlURL);
		UserManagementService ss = new UserManagementService(wsdlURL,
				SERVICE_NAME);
		ComOrchestralPortalWebserviceApi72UserUserManagementService port = ss
				.getComOrchestralPortalWebserviceApi72UserUserManagementServicePort();
		
		return port;
	}
	
	/**
	 * test the connection to Concerto with the given username
	 * @param username
	 * @throws ServiceException if there is an exception during the connection or during the updating.
	 */
	public static void testGetClientUser(String username) throws ServiceException{
//		final EngineConfiguration config = new FileProvider( "client-deploy.wsdd" );
//		final UserManagerServiceSEIServiceLocator locator = new UserManagerServiceSEIServiceLocator( config );
//		String concertoUrl = LdapProperty.getProperty(UserMgmtConstants.CONCERTO_URL);
//		locator.setUserManagerServiceEndpointAddress(concertoUrl+"/services/UserManagerService");
//		try {
//			final UserManagerServiceSEI userManager = locator.getUserManagerService();
//			UserDTO user = userManager.getUser(username);
//			logger.info("JM> "+user.getAccountType());
//			
//		} catch (ServiceException e) {
//			logger.error(ErrorConstants.FAIL_UPDATE_CONCERTO, e);
//			throw new ServiceException(ErrorConstants.FAIL_UPDATE_CONCERTO);
//			
//		} catch (UserManagerServiceException e) {
//			logger.error(ErrorConstants.FAIL_UPDATE_CONCERTO, e);
//			throw new ServiceException(ErrorConstants.FAIL_UPDATE_CONCERTO);
//			
//		} catch (RemoteException e) {
//			logger.error(ErrorConstants.FAIL_UPDATE_CONCERTO, e);
//			throw new ServiceException(ErrorConstants.FAIL_UPDATE_CONCERTO);
//		}

		throw new ServiceException("This function has not been implemented.");
	}
	
	
	/**
	 * check if the username exist in Concerto
	 * @param username
	 * @return true (if username exist in Concerto) false (otherwise)
	 */
	public static boolean doesClientUserExist(String username){
		// TODO need to implement before closing ticket SPT-609
		return false;
	}
	
	
	
	/**
	 * set the user's (that given by username) accountType (in concerto database) to LDAP
	 * @param username
	 * @throws ServiceException if there is an exception during the connection or during the updating.
	 */
	public static void enableNT(String username) throws ServiceException{
//		final EngineConfiguration config = new FileProvider( "client-deploy.wsdd" );
//		final UserManagerServiceSEIServiceLocator locator = new UserManagerServiceSEIServiceLocator( config );
//		String concertoUrl = LdapProperty.getProperty(UserMgmtConstants.CONCERTO_URL);
//		locator.setUserManagerServiceEndpointAddress(concertoUrl+"/services/UserManagerService");
//		try {
//			final UserManagerServiceSEI userManager = locator.getUserManagerService();
//			UserDTO user = userManager.getUser(username);
//			user.setAccountType("LDAP");
//			userManager.updateUser(user);
//		} catch (ServiceException e) {
//			logger.error(ErrorConstants.FAIL_UPDATE_CONCERTO, e);
//			throw new ServiceException(ErrorConstants.FAIL_UPDATE_CONCERTO);
//			
//		} catch (UserManagerServiceException e) {
//			logger.error(ErrorConstants.FAIL_UPDATE_CONCERTO, e);
//			throw new ServiceException(ErrorConstants.FAIL_UPDATE_CONCERTO);
//			
//		} catch (RemoteException e) {
//			logger.error(ErrorConstants.FAIL_UPDATE_CONCERTO, e);
//			throw new ServiceException(ErrorConstants.FAIL_UPDATE_CONCERTO);
//		}
		throw new ServiceException("This function has not been implemented.");
	}
	
	
	
	/**
	 * add a user to Concerto. The user detail given by the parameters.
	 * @param username
	 * @param clientID
	 * @param fullName
	 * @param description
	 * @param email
	 * @throws Exception 
	 * @throws ServiceException if there is an exception during the connection or during the updating.
	 */
					   
	public static void addClientUser(String userName, String firstName, String lastName, String fullname, String description, String mail, String clientAccountId) throws Exception{
		
		ComOrchestralPortalWebserviceApi72UserUserManagementService port = null;
		
		logger.info("About to get endpoint object from portal's webservice");
		
		try {
			port = getConcertoServicePort();
		} catch (MalformedURLException e) {
			new MalformedURLException("Given wsdl url is not correct.");
		}
		
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
		try {
			port.createUser(user);
		} catch (Exception e) {
			throw e; 
		}
	}
}
