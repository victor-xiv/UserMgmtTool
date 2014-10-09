package beans;

import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.SortedSet;

import javax.naming.NamingException;

import org.apache.log4j.Logger;

import ldap.ErrorConstants;
import ldap.LdapTool;

public class LdapUserGroups {
	private SortedSet<String> userGroups;
	
	
	
	/**
	 * setter method of this.userGroups
	 * @param userGroups
	 */
	public void setUserGroups(SortedSet<String> userGroups){
		this.userGroups = userGroups;
	}
	
	/*
	 * public SortedSet<String> getUserGroups(){
		return this.userGroups;
	}
	*/
	
	
	
	/**
	 * @return this.userGroups as String[]
	 */
	public String[] getUserGroups(){
		String[] output = new String[userGroups.size()];
		output = (String[])userGroups.toArray(output);
		return output;
	}
	
	
	
	/**
	 * connect to LDAP server and get the user groups from the LDAP server.
	 * @throws ConnectException if there is an exception during the connection to LDAP server or accessing LDAP server data.
	 */
	public void refreshGetUserGroup() throws ConnectException{
		Logger logger = Logger.getRootLogger();
		logger.debug("Querying for all the organisations that are stored in Clients folder");
		
		LdapTool lt = null;
		try {
			lt = new LdapTool();
		} catch (FileNotFoundException fe){
			throw new ConnectException(fe.getMessage());
			// we are not logging this error, because it has been logged in LdapTool()
		} catch (NamingException e) {
			throw new ConnectException(e.getMessage());
			// we are not logging this error, because it has been logged in LdapTool()
		}
		
		if( lt != null){
			userGroups = lt.getUserGroups();
			lt.close();
		} else {
			throw new ConnectException(ErrorConstants.UNKNOWN_ERR);
		}
		
		logger.debug("Finished for all the organisations that are stored in Clients folder");
	}
}
