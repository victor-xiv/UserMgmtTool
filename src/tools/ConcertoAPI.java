package tools;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Arrays;

import javax.xml.rpc.ServiceException;

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

	public static void testGetClientUser(String username){
		final EngineConfiguration config = new FileProvider( "client-deploy.wsdd" );
		final UserManagerServiceSEIServiceLocator locator = new UserManagerServiceSEIServiceLocator( config );
		String concertoUrl = LdapProperty.getProperty(UserMgmtConstants.CONCERTO_URL);
		locator.setUserManagerServiceEndpointAddress(concertoUrl+"/services/UserManagerService");
		try {
			final UserManagerServiceSEI userManager = locator.getUserManagerService();
			UserDTO user = userManager.getUser(username);
			logger.info("JM> "+user.getAccountType());
		} catch (ServiceException e) {
			logger.error(e.toString());
			e.printStackTrace();
		} catch (UserManagerServiceException e) {
			logger.error(e.toString());
			e.printStackTrace();
		} catch (RemoteException e) {
			logger.error(e.toString());
			e.printStackTrace();
		}
	}
	
	public static void enableNT(String username){
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
			logger.error(e.toString());
			e.printStackTrace();
		} catch (UserManagerServiceException e) {
			logger.error(e.toString());
			e.printStackTrace();
		} catch (RemoteException e) {
			logger.error(e.toString());
			e.printStackTrace();
		}
	}
	
	public static void addClientUser(String username, String clientID, String fullName, String description, String email){
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
			logger.error(e.toString());
			e.printStackTrace();
		} catch (UserManagerServiceException e) {
			logger.error(e.toString());
			e.printStackTrace();
		} catch (RemoteException e) {
			logger.error(e.toString());
			e.printStackTrace();
		}
	}
}
