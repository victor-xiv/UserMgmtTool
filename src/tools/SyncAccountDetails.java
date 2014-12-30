package tools;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.naming.InvalidNameException;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.ldap.Rdn;

import ldap.LdapTool;

import org.apache.log4j.Logger;


public class SyncAccountDetails {
	
	
	// these attributes will not be synced to Ldap account
	final static String[] NOT_TO_BE_UPDATED_ATTRS = {"sAMAccountName", "positionCodeId", "page", "dn", "userDN",
														"recordStatus", "userPrivilegeId", "receiveSms", "staffId",
														"clientId", "contactPersonPager", "active", "clientAccountId",
														"streetAddress", "c"}; 
						
	
	/**
	 * Use the given oldUserDN to get the information from the Support Tracker DB and use this information
	 * to update the Ldap account. Once the process finished it will return a new value of the distinguished name, if it has been modified.
	 * If it has not been modified it will return the old one.
	 * 
	 * @param oldUserDN is the distinguishedName of for the Ldap account. It must have not been escaped the reserved chars.
	 * 
	 * @return Once the process finished it will return a new value of the distinguished name, if it has been modified.
	 * If it has not been modified it will return the old one.
	 */
	public static String syncAndGetBackNewUserDNForGivenUserDN(String oldUserDN){
		Logger logger = Logger.getRootLogger();
		
		
		LdapTool lt = null;
		try {
			lt = new LdapTool();
		} catch (FileNotFoundException | NamingException e) {
			logger.error("Couldn't initiate or connect to Ldap server.", e);
			return oldUserDN;
		}
		String clientName = lt.getUserCompany(oldUserDN);
		String clientDN = lt.getDNFromOrg(clientName);
		String username = lt.getUsername(oldUserDN);
		
		SyncAccountDetails syncer = new SyncAccountDetails();
		syncer.syncUserDetailsFromSupportTrackerDBtoLdapForTheGivenUserDN(oldUserDN);
		
		
		Attributes attrs = lt.getUserAttributesWhoBelongsToClientAndHasLoginName(clientDN, username);
		if(attrs == null) return oldUserDN;
		
		String newUserDN = null;
		try {
			String escapedNewUserDN = attrs.get("distinguishedName").get().toString();
			newUserDN = (String) Rdn.unescapeValue(escapedNewUserDN);
		} catch (NamingException | NullPointerException e) {
			logger.error("Error occured while getting new userDN after synced.", e);
			return oldUserDN;
		}
		
		return newUserDN;
	}
	
	
	/**
	 * Sync all the details of all the accounts that stored in the given client name.
	 * The method is neither returning any results of the process nor any exceptions/erros.
	 * So, if there are any problems with any account. it will simply go on to the next one.
	 * @param client (must not be unescaped) is just client/company/organisation name (not DN)
	 */
	public static void syncAllUsersThatBelongsToClient(String client){
		Logger logger = Logger.getRootLogger();
		logger.debug("start syncing all the accounts details from ST DB to Ldap for: " + client);
		
		LdapTool lt;
		try {
			lt = new LdapTool();
		} catch (FileNotFoundException | NamingException e1) {
			logger.error("Couldn't connect to Ldap server.", e1);
			return;
			// we are not doing anything if there's an error.
		} 
		
		// get all the accounts that stored under (member of) this client as a list of userDN and enabling status object 
		TreeMap<String, String[]> usersList = lt.getClientUsers(client);

		
		/**
		 * because the whole process for each account is very time consuming
		 * so, we do multi threading here, where we create the number of threads equals to number of processors
		 * and each thread processing an equal number of accounts.
		 */
		int numbCPUs = Runtime.getRuntime().availableProcessors();
		int temp = 0;
		
		// prepare a list of accounts for each thread
		ArrayList<String>[] userDNListForAllThreads = new ArrayList[numbCPUs];
		for(Map.Entry<String, String[]> aUserL : usersList.entrySet()){
			if(userDNListForAllThreads[temp]==null) userDNListForAllThreads[temp] = new ArrayList();
			String unescapedUserDN = aUserL.getValue()[0];
			if(unescapedUserDN != null && !unescapedUserDN.trim().isEmpty()) userDNListForAllThreads[temp].add(unescapedUserDN);
			temp = ++temp % numbCPUs;
		}
		
		// a list that store all the threads that are processing the accounts
		ArrayList<Thread> allThreads = new ArrayList<>();
		// create each thread and pass the list of accounts that it need to handle to its constructor.
		for(final ArrayList<String> userDNListForAThread : userDNListForAllThreads){
			if(userDNListForAThread!=null && userDNListForAThread.size()>0){
				Thread t = new Thread(){
					public void run(){
						for(final String unescapedUserDN : userDNListForAThread){
							SyncAccountDetails syncer = new SyncAccountDetails();
							syncer.syncUserDetailsFromSupportTrackerDBtoLdapForTheGivenUserDN(unescapedUserDN);
						}
					}
				};
				t.start();
				allThreads.add(t);
				
				logger.debug("Thread " + t.getId() + " is syncing accounts: " + userDNListForAThread);
			}
		}
		
		// waiting for all threads to finish together
		for(Thread t : allThreads){
			try {
				t.join();
			} catch (InterruptedException e) {
				// we are not doing anything, because we just want to get only any successful threads
			}
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	ConcertoAPI concerto = null;
	Logger logger = Logger.getRootLogger();
	
	public SyncAccountDetails(){
		try {
			concerto = new ConcertoAPI();
		} catch (MalformedURLException e) {
		}
	}
	
	/**
	 * Use the given userDN to get the information from the Support Tracker DB and use this information
	 * to update the Ldap account.
	 * 
	 * @param userDN is the distinguishedName of for the Ldap account. It must have not been escaped the reserved chars.
	 */
	public void syncUserDetailsFromSupportTrackerDBtoLdapForTheGivenUserDN(String userDN){
		logger.debug("Start processing syncing of the user details of: " + userDN);
		
		
		LdapTool lt = null;
		try {
			lt = new LdapTool();
		} catch (FileNotFoundException | NamingException e) {
			logger.error("Couldn't initiate or connect to Ldap server.", e);
			return;
		}
		
		Attributes attrs = lt.getUserAttributes(userDN);
		String company = lt.getUserCompany(userDN);
		String username = lt.getUsername(userDN);
		
		boolean concertoHasBeenDisabled = false;
		
		try{
			// if any account (Ldap account or Support Tracker account) is disabled
			// we will disable the other accounts
			if(lt.isAccountDisabled(userDN)){
				SupportTrackerJDBC.disableClientAccount(username);
				SupportTrackerJDBC.disableStaffAccount(username);
				
				concertoHasBeenDisabled = true;
				concerto.deleteAccountOfGivenUser(username);
			}
		} catch (Exception e){
			logger.error("Exception while disable account for: " + username, e);
		}
		
		
		// If there's no id stored in the Ldap client account
		// we will not process further. because we link Ldap and ST DB client account using id
		// in ST DB, we use clientAccountId column. in Ldap, we use "Info" attribute
		String id;
		try {
			id = attrs.get("Info")==null ? null : attrs.get("Info").get().toString();
		} catch (NamingException e) {
			logger.error("An unknown exception thrown while getting Info attribute from Ldap account.", e);
			return;
		}
		if(id == null && !company.equalsIgnoreCase(lt.ORION_HEALTH_NAME)){
			logger.debug("user: " + userDN + " is not storing the clientAccountId/staffId in the Info attribute of its Ldap account.");
			return;
		}
		
		
		// if the given userDN is Orion Staff, getting the detail from Staff table
		// otherwise, getting the detail from ClientAccount table
		Map<String, String> userDetails = new HashMap<String,String>();
		boolean enabled = false;
		try{
			if(company.equalsIgnoreCase(lt.ORION_HEALTH_NAME)){
				userDetails = SupportTrackerJDBC.getOrionHealthStaffDetails(username);
				enabled = SupportTrackerJDBC.isStaffAccountEnabled(username);
				
			} else {
				userDetails = SupportTrackerJDBC.getUserDetails(username, id);
				enabled = SupportTrackerJDBC.isClientAccountEnabled(username, id);
			}
		} catch (SQLException e){
			logger.error("Exception while disable account for: " + username, e);
			return;
		}
		
		
		// if there's any problem, and the userDetails is null or empty
		// we will not process further, because we can't find anything to update
		if(userDetails==null || userDetails.isEmpty()){
			return;
		}
		
		// if any account (Ldap account or Support Tracker account) is disabled
		// we will disable the other accounts
		if(!enabled){
			lt.disableUser(userDN);
			try {
				if(!concertoHasBeenDisabled){
					concerto.deleteAccountOfGivenUser(username);
				}
			} catch (Exception e) {
				logger.error("Exception while disable account for: " + username, e);
			}
		}

		
		// preparing a map to update
		Map<String, String[]> updateDetails = new HashMap<>();
		for(Map.Entry<String, String> entry: userDetails.entrySet()){
			if(entry.getValue() != null && !entry.getValue().trim().isEmpty()){
				updateDetails.put(entry.getKey(), new String[]{entry.getValue()});
			}
		}
		// remove those key/values that we dont' want to sync
		for(String attr : NOT_TO_BE_UPDATED_ATTRS){
			updateDetails.remove(attr);
		}
		
		// lt.updateUser() require the map to have "dn" key/value pair
		updateDetails.put("dn", new String[]{userDN});
		
		lt.updateUser(updateDetails);
		logger.debug("Finished syncing for: " + userDN);
		
		lt.close();
	}

}
