package tools;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.xml.rpc.ServiceException;

import ldap.ErrorConstants;
import ldap.LdapProperty;
import ldap.UserMgmtConstants;

import org.apache.axis.EngineConfiguration;
import org.apache.axis.configuration.FileProvider;
import org.apache.log4j.Logger;

import com.concerto.webservice.user.UserManagerServiceSEI;
import com.concerto.webservice.user.UserManagerServiceSEIServiceLocator;
import com.concerto.webservice.user.dto.UserAttributeDTO;
import com.concerto.webservice.user.dto.UserDTO;
import com.concerto.webservice.user.exception.UserManagerServiceException;

public class ConcertoAPI {
	private static Logger logger = Logger.getRootLogger();

	
	
	/**
	 * test the connection to Concerto with the given username
	 * @param username
	 * @throws ServiceException if there is an exception during the connection or during the updating.
	 */
	public static void testGetClientUser(String username) throws ServiceException{
		final EngineConfiguration config = new FileProvider( "client-deploy.wsdd" );
		final UserManagerServiceSEIServiceLocator locator = new UserManagerServiceSEIServiceLocator( config );
		String concertoUrl = LdapProperty.getProperty(UserMgmtConstants.CONCERTO_URL);
		locator.setUserManagerServiceEndpointAddress(concertoUrl+"/services/UserManagerService");
		try {
			final UserManagerServiceSEI userManager = locator.getUserManagerService();
			UserDTO user = userManager.getUser(username);
			logger.info("JM> "+user.getAccountType());
			
		} catch (ServiceException e) {
			logger.error(ErrorConstants.FAIL_UPDATE_CONCERTO, e);
			throw new ServiceException(ErrorConstants.FAIL_UPDATE_CONCERTO);
			
		} catch (UserManagerServiceException e) {
			logger.error(ErrorConstants.FAIL_UPDATE_CONCERTO, e);
			throw new ServiceException(ErrorConstants.FAIL_UPDATE_CONCERTO);
			
		} catch (RemoteException e) {
			logger.error(ErrorConstants.FAIL_UPDATE_CONCERTO, e);
			throw new ServiceException(ErrorConstants.FAIL_UPDATE_CONCERTO);
		}
	}
	
	
	
	/**
	 * set the user's (that given by username) accountType (in concerto database) to LDAP
	 * @param username
	 * @throws ServiceException if there is an exception during the connection or during the updating.
	 */
	public static void enableNT(String username) throws ServiceException{
		final EngineConfiguration config = new FileProvider( "client-deploy.wsdd" );
		final UserManagerServiceSEIServiceLocator locator = new UserManagerServiceSEIServiceLocator( config );
		String concertoUrl = LdapProperty.getProperty(UserMgmtConstants.CONCERTO_URL);
		locator.setUserManagerServiceEndpointAddress(concertoUrl+"/services/UserManagerService");
		try {
			final UserManagerServiceSEI userManager = locator.getUserManagerService();
			UserDTO user = userManager.getUser(username);
			user.setAccountType("LDAP");
			userManager.updateUser(user);
		} catch (ServiceException e) {
			logger.error(ErrorConstants.FAIL_UPDATE_CONCERTO, e);
			throw new ServiceException(ErrorConstants.FAIL_UPDATE_CONCERTO);
			
		} catch (UserManagerServiceException e) {
			logger.error(ErrorConstants.FAIL_UPDATE_CONCERTO, e);
			throw new ServiceException(ErrorConstants.FAIL_UPDATE_CONCERTO);
			
		} catch (RemoteException e) {
			logger.error(ErrorConstants.FAIL_UPDATE_CONCERTO, e);
			throw new ServiceException(ErrorConstants.FAIL_UPDATE_CONCERTO);
		}
	}
	
	
	
	/**
	 * add a user to Concerto. The user detail given by the parameters.
	 * @param username
	 * @param clientID
	 * @param fullName
	 * @param description
	 * @param email
	 * @throws ServiceException if there is an exception during the connection or during the updating.
	 */
	public static void addClientUser(String username, String clientID, String fullName, String description, String email) throws ServiceException{
		final EngineConfiguration config = new FileProvider( "client-deploy.wsdd" );
		final UserManagerServiceSEIServiceLocator locator = new UserManagerServiceSEIServiceLocator( config );
		String concertoUrl = LdapProperty.getProperty(UserMgmtConstants.CONCERTO_URL);
		locator.setUserManagerServiceEndpointAddress(concertoUrl+"/services/UserManagerService");
		try {
			final UserManagerServiceSEI userManager = locator.getUserManagerService();
			UserDTO user = userManager.addUser(username);
			String[] groupMemberships = user.getGroupMemberships();
			ArrayList<String> groupList = new ArrayList<String>(Arrays.asList(groupMemberships));
			groupList.add("Clients");
			groupMemberships = groupList.toArray(groupMemberships);
			user.setGroupMemberships(groupMemberships);
			userManager.updateUser(user);
			
			user = userManager.getUser(username);
			UserAttributeDTO[] userAttributes = user.getUserAttributes();
			for(int i = 0; i < userAttributes.length; i++){
				UserAttributeDTO userAttribute = userAttributes[i];
				if(userAttribute.getDisplayName().equals("ClientID") &&
						userAttribute.getGroup().equals("Clients")){
					userAttribute.setValue(clientID);
					userAttributes[i] = userAttribute;
				}else if(userAttribute.getDisplayName().equals("Full Name") &&
						userAttribute.getGroup().equals("Users")){
					userAttribute.setValue(fullName);
					userAttributes[i] = userAttribute;
				}else if(userAttribute.getDisplayName().equals("Description") &&
						userAttribute.getGroup().equals("Users")){
					userAttribute.setValue(description);
					userAttributes[i] = userAttribute;
				}else if(userAttribute.getDisplayName().equals("E-mail") &&
						userAttribute.getGroup().equals("Users")){
					userAttribute.setValue(email);
					userAttributes[i] = userAttribute;
				}
			}
			user.setUserAttributes(userAttributes);
			user.setAccountType("LDAP");
			userManager.updateUser(user);
			
		} catch (ServiceException e) {
			logger.error(ErrorConstants.FAIL_UPDATE_CONCERTO, e);
			throw new ServiceException(ErrorConstants.FAIL_UPDATE_CONCERTO);
			
		} catch (UserManagerServiceException e) {
			logger.error(ErrorConstants.FAIL_UPDATE_CONCERTO, e);
			throw new ServiceException(ErrorConstants.FAIL_UPDATE_CONCERTO);
			
		} catch (RemoteException e) {
			logger.error(ErrorConstants.FAIL_UPDATE_CONCERTO, e);
			throw new ServiceException(ErrorConstants.FAIL_UPDATE_CONCERTO);
		}
	}
}
