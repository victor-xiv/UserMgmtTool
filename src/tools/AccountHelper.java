package tools;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.Map;

import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;

import ldap.ErrorConstants;
import ldap.LdapTool;

import org.apache.log4j.Logger;

import beans.AccountRequestsBean;

public class AccountHelper {
	/**
	 * a helper used to create a new account. The properties of the new account is given as the "maps" parameter
	 * 1). it will check login name first (sAMAccountName) before it continue processing 
	 * (look at this method validateLoginName()) of how it check the login name
	 * 2). Check the given properties (maps). it must have at least 17 properties
	 *  look at LdapTool.addUser(maps) for more detail about those properties
	 * 2). It checks the number of characters of fullname (firstname + " " + lastname).
	 * fullname must be less than the max number of characters configured in ldap.properties
	 * 3). It checks if the company of this new user has already been created in Ldap server as the "Clients"
	 * if it has not, then it will try to create
	 * 4). It checks if the company of this new user has already been created in Ldap server as the "Groups"
	 * if it has not, then it will try to create
	 * 5). It checks if this user has already been created in the Ldap server
	 * if it has already been created, it will treat as failed.
	 * 6). check if this user does exist in the Concerto Portal
	 * if it exist, treat as failed
	 * 7). add the user into support tracker db
	 * 8). add the user into ldap server
	 * 9). add the user into concerto portal
	 * 10). enabltNT(), set the user to login to support tracker using LDAP server
	 * 11). Send out an appropriate email to the user telling about the result of the account creation
	 *    11a). if isPswGenerated==true and user's mobile phone is valid (when password was generated programmatically) then send the new password to user's mobile phone via sms 
	 *    11b). otherwise, just send an email to the user telling user to contact support tracker
	 * @param maps of the properties for the new account. there are at least 16 properties (key/value pairs) in this maps object
	 * Those keys of those properties are:
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
	 * @param isPswGenerated : true if the password (stored in maps param) was programmatically generated. false otherwise 
	 * @return the result string, that start with:
	 * * "false|message goes here" if the process fail
	 * * "true|message goes here" if the process is successful
	 */
	public static String createAccount(Map<String, String[]> maps, boolean isPswGenerated){
		Logger logger = Logger.getRootLogger();
		logger.debug("Creating an account for username: "+maps.get("sAMAccountName")[0]);
		
		try{ // check whether login name/username is valid
			String sAMAccountName = maps.get("sAMAccountName")[0];
			if(validateLoginName(sAMAccountName).contains("false|"))
				return "false|username (i.e.sAMAccountName) in the given properties is not valid";
		} catch (NullPointerException | IndexOutOfBoundsException e){
			return "false|Given properties doesn't contain a username (i.e. sAMAccountName)";
		}

		
		if(maps == null || maps.size() < 16){
			return "false|Given properties for creating this user: " +maps.get("sAMAccountName")[0]+" are not complete";
		}
		
		// check the displayName length
		// support tracker database has a limit size (e.g. 20 chars) for this column
		// so, if the displayName is greater than this limit size, we are not processing further
		// SPT-839
		String displayName = maps.get("displayName")[0];
		int sizeLimit = AccountRequestsBean.getDisplayNameSizeLimit();
		if(displayName.length() > sizeLimit){
			logger.error("The display name: " + displayName + " is too long. The allowed size is: " + sizeLimit + " chars.");
			return "false|The display name: " + displayName + " is too long. The allowed size is: " + sizeLimit + " chars.";
		}
		
		
		// connecting to LDAP server
		LdapTool lt = null;
		try {
			lt = new LdapTool();
		} catch (NamingException | FileNotFoundException e){	
			return "false|Could not connect to LDAP server due to: "+ e.getMessage();
			//no need to log, the error has been logged in LdapTool()
		}
		if( lt == null){
			return "false|Unknown Error while connecting to LDAP server";
		}
		
		
		ConcertoAPI concerto = null;
		try{
			concerto = new ConcertoAPI();
		} catch (MalformedURLException e){
			logger.error("Couldn't create Concerto Portal Webservice.", e);
			return "false|Could not connect to Concerto Portal Webservice due to: "+ e.getMessage();
		}
		if(concerto == null){
			return "false|Could not connect to Concerto Portal Webservice.";
		}
		
		// if company doesn't exist in LDAP's "Client" => add the company into "Client"
		if(!lt.companyExists(maps.get("company")[0])){
			try {
				if(!lt.addCompany(maps.get("company")[0]))
					new NamingException(); 					// if companyName doesn't exist in "Client" and can't be added, just return false;
			} catch (NamingException e) {
				return "false|The organisation of requesting user doesn't exist and couldn't be added into LDAP's Clients.";
			}
		}
		// if company doesn't exist in LDAP's "Groups"
		if(!lt.companyExistsAsGroup(maps.get("company")[0])){
			try {
				//  add the company into "Groups"
				if(!lt.addCompanyAsGroup(maps.get("company")[0])) 
					new NamingException();  // if adding company into group failed
			} catch (NamingException e) {
				return "false|The organisation of requesting user doesn't exist and couldn't be added into LDAP's Groups.";
			}
		}
		
		// fullname is used to check whether this name exist in LDAP and concerto.
		// and used to add into concerto
		String fullname = "";
		if(maps.get("displayName")[0] != null) 	fullname = maps.get("displayName")[0];
		else 	fullname = maps.get("givenName")[0] + " " + maps.get("sn")[0];
		
		// these variable used for adding a user into ConcertoAPI
		String userName = maps.get("sAMAccountName")[0];

		// check if username exist in LDAP or Concerto
		boolean usrExistsInLDAP = lt.usernameExists(fullname, maps.get("company")[0]);
		boolean usrExistsInConcerto = false;
		try {
			usrExistsInConcerto = concerto.doesUserExist(userName);
		} catch (Exception e) {
			return "false|Cannot connect to Concerto server. Reason: " + e.getMessage();
		}
		
		if(usrExistsInLDAP){
			return "false|Requesting user already exists in LDAP server";
		} else if(usrExistsInConcerto){
			return "false|Requesting user already exists in Concerto server";
		}
		
		// we are not allowing Support Tracker DB (in ClientAccount table and Staff table) to have more than one username
		boolean usrExistsInSupporTrackerDB = false;
		try{
			usrExistsInSupporTrackerDB = SupportTrackerJDBC.isAnySupportTrackerClientAccountMatchUsername(userName)
										|| SupportTrackerJDBC.isAnySupportTrackerStaffAccountMatchUsername(userName);
		} catch (SQLException e){
			return "false|Couldnot connection to Support Tracker DB";
		}
		if(usrExistsInSupporTrackerDB){
			return "false|Requesting username already exists in Support Tracker DB.";
		}
		
		
		
		// ADDING USER ACCOUNT CODE STARTS FROM HERE \\
		
		int clientAccountId = -1;
		try {
			clientAccountId = SupportTrackerJDBC.addClient(maps);
		} catch (SQLException e1) {
			return "false|User cannot be added to Supprt Tracker Database, due to: "+e1.getMessage();
			//no need to log, the error has been logged in SupportTrackerJDBC.addClient(maps)
		}
		
		// if add a user into Supprt Tracker successfully
		if( clientAccountId > 0 ){
			maps.put("info", new String[]{Integer.toString(clientAccountId)});
			
			// add user into LDAP server
			boolean addStatus = false;
			
			try{
				addStatus = lt.addUser(maps);
			} catch (Exception e){
				deletePreviouslyAddedClientFromSupportTracker(clientAccountId);
				return "false|User "+maps.get("displayName")[0]+" has been added into Support Tracker DB. But, it was not added to LDAP, due to: " + e.getMessage();
			}
			
			if( addStatus ){ // add a user into Ldap successfully
				try {
					concerto.addClientUser(maps);
				} catch (Exception e) {
					// remove the previous added user from Support Tracker DB
					lt.deleteUser(maps.get("displayName")[0], maps.get("company")[0]);
					deletePreviouslyAddedClientFromSupportTracker(clientAccountId);
					return "false|User "+maps.get("displayName")[0]+" was added to LDAP and Support Tracker. But it couldn't be added to Concerto Portal.";
				}
				
				
				
				
				
				
				// inform the user about their request
				try {
					String mobile = (maps.get("mobile") != null) ? maps.get("mobile")[0] : null;
					mobile = SupportTrackerJDBC.cleanUpAndValidateMobilePhone(mobile);

					if (mobile != null && isPswGenerated) {
						// if mobile phone is valid => send psw to to mobile phone number's sms
						EmailClient.sendEmailForApprovedRequestWithGeneratedPsw(
								maps.get("mail")[0], maps.get("displayName")[0],
								maps.get("sAMAccountName")[0], mobile,
								maps.get("password01")[0]);
					} else {
						// if a password has been manually assigned by the admin user
						// or there's no valid mobile phone number
						// => just send an email to the user telling them to call support team for their password
						EmailClient.sendEmailForApprovedRequestWithManualPsw(
								maps.get("mail")[0], maps.get("displayName")[0],
								maps.get("sAMAccountName")[0]);
					}

				} catch (Exception e) {
					String result = "true|User "+maps.get("displayName")[0]+" was added successfully with user id: "+maps.get("sAMAccountName")[0] + ". " +
							"But, it couldn't send out an approval email to: " + maps.get("mail")[0] +". Because: " + e.getMessage();
					logger.error(result, e);
					return result;
				}
				
				
				
				return "true|User "+maps.get("displayName")[0]+" was added successfully with user id: "+maps.get("sAMAccountName")[0];
			
			}else{ // add a user into Ldap is not successful
				
				// remove the previous added user from Support Tracker DB
				deletePreviouslyAddedClientFromSupportTracker(clientAccountId);
				
				return "false|User "+maps.get("displayName")[0]+" was not added, due to the failure in adding the user into LDAP server.";
			}
		}else{
			return "false|User "+maps.get("displayName")[0]+" was not added, due to the failure in adding the user into Support Tracker DB.";
		}
	}
	
	
	/**
	 * a helper to get and validate the login name (sAMAccountName) from the HttpServletRequest
	 * @param request
	 * @return the login name, if this login name is valid. otherwise, return a message "false|reason of the failure in getting that login name"
	 */
	public static String getLoginNameFromRequest(HttpServletRequest request){
		// sAMAccountName used as LDAP logon (i.e username)
		// it is allowed to have only these special chars:  ( ) . - _ ` ~ @ $ ^  and other normal chars [a-zA-Z0-9]
		String sAMAccountName = request.getParameter("username");
		
		return validateLoginName(sAMAccountName);
	}
	
	
	/**
	 * validate the given login name (sAMAccountName)
	 * @param sAMAccountName given login name
	 * @return the login name, if this login name is valid. otherwise, return a message "false|reason of the failure in getting that login name"
	 */
	public static String validateLoginName(String sAMAccountName){
		// sAMAccountName used as LDAP logon (i.e username)
		// it is allowed to have only these special chars:  ( ) . - _ ` ~ @ $ ^  and other normal chars [a-zA-Z0-9]
		
		//MODIFIED CODE - SPT-447
		//Handle code for null and blank usernames SEPARATELY
		//Previously handled together risking Null Pointer Exception
		// validate the sAMAccountName
		if( sAMAccountName == null || sAMAccountName.trim().isEmpty()){
			return "false|User was not added with invalid username.";
		}
		//MODIFIED CODE ENDS
		
		// check if sAMAccountName contains any prohibited chars
		String temp = sAMAccountName.replaceAll("[\\,\\<\\>\\;\\=\\*\\[\\]\\|\\:\\~\\#\\+\\&\\%\\{\\}\\?\\'\\\"]", "");
		if(temp.length() < sAMAccountName.length()){
			return "false|Username contains some forbid speical characters. The special characters allowed to have in username are: ( ) . - _ ` ~ @ $ ^";
		}
		
		return sAMAccountName;
	}
	
	
	
	
	/**
	 * a helper method to help createAccount() to delete clientAccountId from the Support Tracker DB. (this method is used to avoid duplicate code only)
	 * It was successful to add a user into support tracker (and the clientAccountId was returned from support tracker).
	 * But, it was unsuccessful to add that user to LDAP. So, we need to delete this newly added clientAccountId from Support Tracker.
	 * @param clientAccountId
	 */
	public static void deletePreviouslyAddedClientFromSupportTracker(int clientAccountId){
		Logger logger = Logger.getRootLogger(); // initiate as a default root logger
		
		// remove the previous added user from Support Tracker DB
		try {
			SupportTrackerJDBC.deleteClient(clientAccountId);
		} catch (SQLException e) {
			logger.error("An exception occured while deleting this clientAccountId: " + clientAccountId);
		}
	}
	
	
	
}
