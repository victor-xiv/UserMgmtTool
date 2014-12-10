package tools;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.namespace.QName;
import javax.xml.rpc.soap.SOAPFaultException;

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

	public static ComOrchestralPortalWebserviceApi72UserUserManagementService port = null;
	
	
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
	 * @return the Port object that represent the connection with the web service server
	 * @throws MalformedURLException
	 */
	public static ComOrchestralPortalWebserviceApi72UserUserManagementService getConcertoServicePort() throws MalformedURLException{
		Logger logger = Logger.getRootLogger(); // initiate as a default root logger
		
		if(port == null){
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
			try{
				UserManagementService ss = new UserManagementService(wsdlURL,
						SERVICE_NAME);
				port = ss.getComOrchestralPortalWebserviceApi72UserUserManagementServicePort();
				
				logger.debug("connection to concerto portal established");
			} catch (Exception e){
				logger.error("Failed to created Portal Webservice", e);
				port = null;
			}
		}
		return port;
	}
	
	
	
	
	
	/**
	 * get the User object that contains the Concerto user's properties of
	 * @param username  (it should be the same as LDAP's sAMAccount)
	 * @return a user object if there is a user that match to this username, null otherwise
	 * @throws Exception if it failed to connect or retrieve a user
	 */
	public static User getUser(String username) throws Exception {
		if(username==null || username.trim().isEmpty()) throw new Exception("Given username is null or empty");
		
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
			logger.debug("started working with user: " + username);
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
		if(username==null || username.trim().isEmpty()) throw new Exception("Given username is null or empty");
		
		// not logging, because it logged in getUser() method
		try{
			User user = getUser(username);
			
			if(user == null) return false;
			else return true;
			
		} catch (Exception e) {
			Logger.getRootLogger().error("Failed to create services", e);
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
		if(username==null || username.trim().isEmpty()) throw new Exception("Given username is null or empty");
		
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
		return setUserToUsePasswordStoredInLdap(username);
	}
	
	/**
	 * set the account type for the account of the given username. account type is the field that defines which password the concerto should use.
	 * e.g. if account type is :"LDAP" then Concerto will use LDAP password to check the authentication of the user
	 *      if account type is :"Concerto" then Concerto will use Concerto password to check the authentication of the user
	 * @param username
	 * @param accountType
	 * @return true if process has been completed successfully
	 * @throws Exception
	 */
	public static boolean setAccountTypeForGivenUser(String username, String accountType) throws Exception{
		if(username==null || username.trim().isEmpty()) throw new Exception("Given username is null or empty");
		
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
		user.setUserId(username);
		user.setAccountType(accountType);
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
		return setAccountTypeForGivenUser(username, "LDAP");
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
		return setAccountTypeForGivenUser(username, "Concerto");
	}
	
	/**
	 * check whether this account is enabled (enabled means the account is enabled and is not deleted)
	 * @param username
	 * @return
	 * @throws Exception
	 */
	public static boolean isAccountEnabled(String username) throws Exception{
		if(username==null || username.trim().isEmpty()) throw new Exception("Given username is null or empty");
		
		try{
			User user = getUser(username.trim());
			return user.isAccountEnabled().booleanValue() && !user.isDeleted().booleanValue();
		} catch (NullPointerException e) {
			// NullPointerException thrown when there's no username in the concerto
			throw new NullPointerException("User doesn't exist.");
		}
	}
	
	/**
	 * check whether this account is disabled (or deleted)
	 * @param username
	 * @return
	 * @throws Exception
	 */
	public static boolean isAccountDisabled(String username) throws Exception{
		return !isAccountEnabled(username);
	}
	
	/**
	 * check if the given username is using Ldap password
	 * @param username
	 * @return true if the username account is using Ldap password. false, if it is using other password that is not Ldap password
	 * @throws Exception
	 */
	public static boolean isUserUsingLdapPassword(String username) throws Exception{
		if(username==null || username.trim().isEmpty()) throw new Exception("Given username is null or empty");
		
		try{
			User user = getUser(username.trim());
			return user.getAccountType().equalsIgnoreCase("LDAP");
		} catch (NullPointerException e) {
			// NullPointerException thrown when there's no username in the concerto
			throw new NullPointerException("User doesn't exist.");
		}
	}
	
	/**
	 * check if the username is the member of the groupname (based on GroupMembership not the RoleMembership)
	 * @param username
	 * @param groupname
	 * @return
	 * @throws Exception
	 */
	public static boolean isUserMemberOfGivenGroup(String username, String groupname) throws Exception{
		if(username==null || username.trim().isEmpty()) throw new Exception("Given username is null or empty");
		if(groupname==null || groupname.trim().isEmpty()) throw new Exception("Given username is null or empty");
		
		try{
			User user = getUser(username.trim());
			List<String> members = user.getGroupMemberships().getGroup();
			for(String member : members){
				if(member.trim().equalsIgnoreCase(groupname.trim())) return true;
			}
			return false;
		} catch (NullPointerException e){
			// NullPointerException thrown when there's no username in the concerto
			return false;
		}
	}
	
	/**
	 * Check whether the given username is a memberOf one or more groups from the given groupsList (based on GroupMembership not the RoleMembership)
	 * @param username
	 * @param groupsList
	 * @return true if username is a memberOf at least one group from the given groupsList, false otherwise
	 * @throws Exception
	 */
	public static boolean isUserMemberOfAtLeastOneGroupInGivenGroupsList(String username, String[] groupsList) throws Exception{
		for(String group : groupsList){
			if(isUserMemberOfGivenGroup(username, group))
				return true;
		}
		return false;
	}
	
	/**
	 * check whether the given username is a memberOf all the groups from the given groupsList (based on GroupMembership not the RoleMembership)
	 * @param username
	 * @param groupsList
	 * @return true if username is a memberOf all the groups in the groupsList, if the username is a memberOf only one or two groups from the groupsList => it will return false
	 * @throws Exception
	 */
	public static boolean isUserMemberOfAllGroupsInGivenGroupsList(String username, String[] groupsList) throws Exception{
		boolean result = true;
		for(String group : groupsList){
			result = result && isUserMemberOfGivenGroup(username, group);
		}
		return result;
	}
	
	/**
	 * remove a group (given by groupname) from the account of the given username
	 * @param username
	 * @param groupname
	 * @return true if that groupname has been deleted from the account of the given username
	 * @throws Exception
	 */
	public static boolean removeGroupFromUser(String username, String groupname) throws Exception{
		if(username==null || username.trim().isEmpty()) throw new Exception("Given username is null or empty");
		if(groupname==null || groupname.trim().isEmpty()) throw new Exception("Given username is null or empty");
		
		
		Logger logger = Logger.getRootLogger(); // initiate as a default root logger
		
		ComOrchestralPortalWebserviceApi72UserUserManagementService port = getConcertoServicePort();
		User user = getUser(username);
		
		/**
		// since the updateUser() will update only any elements (properties) that are specified (not those that are not specified or null)
		// for any element, the web service will remove those sub-elements that are not listed in the specified elements
		// e.g. let say user.userID='test user' is a member of groups:  'ldapuser, ldapclients, clients, users'
		// now we want to remove 'clients' and add 'newgrouptest'
		// so, the updatedUser must contains: 'ldapuser, ldapclients, users, newgrouptest' as the groups that it is a member of
		// then call updateUser().
		 */
		 
		
		// set updatedUser's userId
		User updatedUser = new User();
		updatedUser.setUserId(user.getUserId());
		
		// loop through the current groups that this user is a member of. e.g. 'ldapuser, ldapclients, clients, users'
		// add only groups that are not the same to the one that we want to remove (e.g. 'clients').
		// so, after loop, the updatedGrMbsh.getGroup() should be: 'ldapuser, ldapclients' only (without 'clients')
		GroupMemberships updatedGrMbsh = new GroupMemberships();
		for(String gr : user.getGroupMemberships().getGroup()){
			if(!gr.equalsIgnoreCase(groupname)){
				updatedGrMbsh.getGroup().add(gr);
			}
		}
		
		updatedUser.setGroupMemberships(updatedGrMbsh);
		
		try{
			port.updateUser(updatedUser);
		} catch (SOAPFaultException e){
			throw new Exception("Couldn't remove the group " + groupname + 
					" from this user: " + username + " because: " + e.getMessage());
		}
		
		return true;
	}
	
	
	/**
	 * remove all the groups (stored in the given groupsList) from the account of the given username
	 * @param username of the account
	 * @param groupsList is list of the groups that need to be removed from this account
	 * @return true if all accounts have been deleted. false, if there's one or more account couldnot be deleted.
	 * @throws Exception
	 */
	public static boolean removeAllGivenGroupsFromGivenUser(String username, String[] groupsList) throws Exception{
		boolean result = true;
		for(String groupname : groupsList){
			result = result && removeGroupFromUser(username, groupname);
		}
		return result;
	}
	
	
	/**
	 * set the account that match the username to enabled or disabled (enability is different from deletion).
	 * by 2014/12/09, we agree to use setAccountDeletionForGivenUser() for disable the account. 
	 * @param enabled to define what action the method should do. if enabled==true, it will enable the given username; else it will disable the given username
	 * @param username
	 * @return true if the process has been done successfully
	 * @throws Exception
	 */
	public static boolean setAccountEnabledForGivenUser(boolean enabled, String username) throws Exception{
		if(username==null || username.trim().isEmpty()) throw new Exception("Given username is null or empty");
		
		
		Logger logger = Logger.getRootLogger(); // initiate as a default root logger
		
		ComOrchestralPortalWebserviceApi72UserUserManagementService port = getConcertoServicePort();
		
		try{
			if(enabled) port.enableUser(username);
			else port.disableUser(username);
			return true;
		} catch (SOAPFaultException e){
			throw new Exception("Couldnot update user: " + username + " on the webservice.");
		}
	}
	
	
	/**
	 * set "soft" delete field of the Concerto account that match the given username
	 * @param deletion to define what action the method should do. if deletion==true, it will "soft" delete the given username; else it will undelete the given username
	 * @param username
	 * @return true if the process has been done successfully
	 * @throws Exception
	 */
	private static boolean setAccountDeletionForGivenUser(boolean deletion, String username) throws Exception{
		if(username==null || username.trim().isEmpty()) throw new Exception("Given username is null or empty");
		
		
		Logger logger = Logger.getRootLogger(); // initiate as a default root logger
		
		ComOrchestralPortalWebserviceApi72UserUserManagementService port = getConcertoServicePort();
		
		try{
			if(deletion) port.deleteUser(username);
			else port.enableUser(username);
			return true;
		} catch (SOAPFaultException e){
			throw new Exception("Couldnot update user: " + username + " on the webservice.");
		}
	}
	
	/**
	 * soft delete the account that match the username
	 * @param username that need to be deleted. this method is the same as setAccountDeletionForGivenUser(true, username)
	 * @return true if it has been deleted successfully
	 * @throws Exception
	 */
	public static boolean deleteAccountOfGivenUser(String username) throws Exception{
		return setAccountDeletionForGivenUser(true, username);
	}
	/**
	 * undelete the account that match the username
	 * @param username that need to be undeleted. this method is the same as setAccountDeletionForGivenUser(false, username)
	 * @return true if it has been undeleted successfully
	 * @throws Exception
	 */
	public static boolean undeleteAccountOfGivenUser(String username) throws Exception{
		return setAccountDeletionForGivenUser(false, username);
	}
	
	
	
	/**
	 * add a user to Concerto. The user detail given by the parameters.
	 * @param maps: parameters that will be used to build user's attributes. the keys of this maps must have all of the following:
sAMAccountName					: username (login name)
givenName	
sn								: Sure name
displayName
password01						: new password
isLdapClient
company
description						: position
c								: Country Code (e.g. NZ for New Zealand)
department
streetAddress
l								: City
st								: State
postalCode
mail							: email
telephoneNumber					: office phone
facsimileTelephoneNumber		: Fax
mobile
info							: is the unique ID that get from clientAccountID column of the clientAccount table of Support Tracker DB

	 * @throws Exception if there is an exception during the connection or during the updating.
	 * @throws MalformedURLException if wsdl url is not correct.
	 */
					   
	public static void addClientUser(Map<String, String[]> maps) throws Exception, MalformedURLException{
		Logger logger = Logger.getRootLogger(); // initiate as a default root logger
		
		/**
		 * if you receive SSLHandShakeException or any exception related to SSL connection.
		 * please find the cxf configuration at /WET-INF/classes/cxf.xml
		 */
		
		String firstName = maps.get("givenName")[0];
		String lastName = maps.get("sn")[0];
		String fullname = maps.get("displayName")[0];
		String userName = maps.get("sAMAccountName")[0];
		String description = maps.get("description")[0];
		String mail = maps.get("mail")[0];
		
		
		ComOrchestralPortalWebserviceApi72UserUserManagementService port = getConcertoServicePort();
				
		logger.debug("Endpoint object is successfully retrieved from webservice");
		
		User user = new User();
		user.setUserId(userName);
		user.setFirstName(firstName);
		user.setLastName(lastName);
		user.setAccountEnabled(true);

		// a list of the attributes that will be added to UserAttributeDto
		String[][] attrs = 
				{
					// group	//name			value				display-value
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
