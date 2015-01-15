package servlets;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.ldap.Rdn;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ldap.LdapConstants;
import ldap.LdapProperty;
import ldap.LdapTool;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.log4j.Logger;

import tools.ConcertoAPI;
import tools.SupportTrackerJDBC;
import tools.SyncAccountDetails;



/**
 * This class is not providing any functions at all.
 * It just provides a way to show all the constants of the content of the consistency-issues contents.
 */
class Issues {
	public static final String ISSUE_1A = "1-a: Disable the account in Support Tracker DB.";
	public static final String ISSUE_1B = "1-b: Disable the account in Concerto.";
	public static final String ISSUE_2 = "2: OrionStaffs LDAP group is no longer required.";
	public static final String ISSUE_3 = "3: LdapUsers is required for all LDAP accounts.";
	public static final String ISSUE_4 = "4: Clients should never be a member of an Orion LDAP group.";
	public static final String ISSUE_5A = "5-a: Orion staff should have an Orion LDAP role.";
	public static final String ISSUE_5B = "5-b: Orion staff should not have more than one Orion LDAP roles.";
	public static final String ISSUE_6 = "6: Orion Staff should not be a member of LdapClients.";
	public static final String ISSUE_7 = "7: For most users only Administrators should be allowed Domain Admins.";
	public static final String ISSUE_8 = "8: Users should be a member of their organisations group in LDAP.";
	public static final String ISSUE_9 = "9: User has an administrator group but does not have an administrator role.";
	public static final String ISSUE_10 = "10 MANUAL: User is still using their concerto password."+
										  " Contact the user and ask them to change their password to swap it to LDAP.";
	public static final String ISSUE_11A = "11-a: Client already has support tracker access but is not a member of LdapClients.";
	public static final String ISSUE_11B = "11-b: Clients group is no longer required.";
	public static final String ISSUE_12 = "12: Orion Staff should not belong to a client group.";
	public static final String ISSUE_13 = "13: Missing Support Tracker Client account.";
	public static final String ISSUE_14 = "14: Missing Support Tracker Staff account.";
	public static final String ISSUE_15A = "15-a: Staff has matching active client account(s).";
	public static final String ISSUE_15B = "15-b: Client has matching active staff account.";
	public static final String ISSUE_16A = "16-a: Client has multiple matching active accounts.";
	public static final String ISSUE_16B = "16-b: Client Id is not set correctly"; 
	public static final String ISSUE_16C = "16-c: MANUAL: Unable to find a matching account for this user, probably as the client name doesn't match."
										 + " Please raise a CSSIR ticket with the organisation and user name.";
	public static final String ISSUE_16D = "16-d: MANUAL: This organisation doesn't exist in Support Tracker Database. "
											+ "Please raise a CSSIR ticket with the organisation and user name.";
	public static final String ISSUE_17A = "17-a: Activate this user's Support Tracker account.";
	public static final String ISSUE_17B = "17-b: Activate this staff's Support Tracker account.";
	public static final String ISSUE_17C = "17-c: Activate Concerto account for this user.";
	public static final String ISSUE_18A = "18-a UPGRADE: Create a Support Tracker account for this user.";
	public static final String ISSUE_18B = "18-b UPGRADE: Create a Concerto account for this user.";
	public static final String ISSUE_18C = "18-c UPGRADE: Add LdapClients role for this user's Ldap  account.";
	public static final String ISSUE_19 = "19 MANUAL: The Limited Account cannot be upgraded, "
												+ "because this organisation doesn't exist in the Support Tracker DB. "
												+ "Please raise a CSSIR ticket with the organisation and user name.";
}



/**
 * this servlet used to define and fix broken accounts, limited accounts and disabled accounts for a given organisation/company
 * serving ticket: SPT-1272
 * All conditions to defined the account type (broken, limited or disabled) are described at: http://woki/display/~jordans/User+Account+Management+Pseudo+Code+for+SPT-1272
 *
 */
public class AccountStatusDetailsServlet extends HttpServlet{
	
	// set up logger
	private Logger logger = Logger.getRootLogger();
	private LdapTool lt = null;
	
	
	public static final String RETURN_CHAR = "\r\n";
	
		
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) {
		
	} 
	
	//All conditions to defined the account type (broken, limited or disabled) are described at: 
	// http://woki/display/~jordans/User+Account+Management+Pseudo+Code+for+SPT-1272
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String rqst = request.getParameter("rqst");
		String rslt = "";
		
		logger.debug("AccountStatusDetailsServlet about to process Post request for request: " + rqst);
		logger.debug("Session: " + request.getSession(true) + " is about to process Post request: " + rqst);
		
		try {
			lt = new LdapTool();
		} catch (NamingException e) {
			// Don't need to log because it has been logged in LdapTool();
			response.getWriter().write("Couldnot connect to Ldap Server.");
			response.getWriter().flush();
			response.getWriter().close();
			return;
		}
		
		switch (rqst) {
		case "getAllUsersOfOrganisation" :
			String clientSimpleName = request.getParameter("orgSimpleName");
			SyncAccountDetails.syncAllUsersThatBelongsToClient(clientSimpleName);
			
			try {
				rslt = getAllUsersOfClientInXMLString(clientSimpleName);
			} catch (NamingException e) {
				// we are not logging, because it has been logged in that method
				rslt = "Could not process the request, because: " + e.getMessage();
			}
			break;
			
		case "getAUserStatusDetails" : 
			String unescapedUserDN = request.getParameter("unescapedUserDN");
			rslt = getAUserStatusDetailsInXMLString(unescapedUserDN);
			break;
		
		case "fixUser" :
			String userDN = request.getParameter("userDN");
			try {
				rslt = fixAccountForGivenUser(userDN);
			} catch (NamingException e) {
				// we are not logging, because it has been logged in that method
				rslt = "Could not process the request, because: " + e.getMessage();
			}
			break;
			
		default:
		}
		
		logger.debug("AccountStatusDetailsServlet finished process Post request. here's the result: \n" + rslt);
		
		response.getWriter().write(rslt);
		response.getWriter().flush();
		response.getWriter().close();
	}
	
	
	
	
	
	/**
	 * this method perform similarly to getAllUsersOfClientInXMLString() method. The difference is only
	 * getAllUsersOfClientInXMLString() is checking all the users that are the members of the given client (or organisation/company). and
	 * this method is checking for the account status of a user account (by the given userDN). 
	 * 
	 * the account status of this account can only be one of the below possibilities
	 * 1). Enabled: nothing wrong with the account, 
	 * 2). Limited: nothing wrong with the account, but the account doesn't have corresponding Support Tracker account. Which means the user cannot has limitation in access to others features.
	 * 3). Disabled: account has been disabled in Ldap, Support Tracker DB and Concerto Portal. Because it has been disabled, so it doesn't mean that this account has no problem. 
	 * 			It means that if we enable this account back, and put this account through this method again, then we might found some broken issues.
	 * 4). Disabled Broken: this account has been disabled in Ldap server, but it has not been disabled in Support Tracker DB and/or it has not been disabled in Concerto Portal.
	 * 			It similar to "Disabled" accounts for other issues.
	 * 5). Broken: this account is enabled, but there are various issues with the account. However, those issues can be fixed programmatically
	 * 6). Broken and Cannot Be Fixed: this account is enabled, but the issues related this account cannot be fixed programmatically or it requires user intervention.
	 * 
	 *   All conditions to defined the account type (broken, limited or disabled) are described at: 
	 *   http://woki/display/~jordans/User+Account+Management+Pseudo+Code+for+SPT-1272
	 * 
	 * @param userDN is the distinguished name of a user account (that need to be checked). It must have not been escaped the reserved chars
	 * 
	 * @return if there's no disruption in the processing, it will return a string that represent an XML, which its root name <response> and the children 
	 * of this <response> (there are only 1st level children, no deeper level) is only one of these: either <brokenInDisabling>, <disabled>, <brokenCantBeFixed>, <broken>, <limited> or <enabled>.
	 * 
	 * an return status contains:
	 * <brokenInDisabling><name>accountDisplayName</name><dn>unescapedAccountDN</dn><solution>proposingSolutions</solution></brokenInDisabling>
	 * <disabled><name>accountDisplayName</name><dn>unescapedAccountDN</dn> <solution>proposingSolutions</solution></disabled>
	 * <brokenCantBeFixed><name>accountDisplayName</name><dn>unescapedAccountDN</dn><solution>proposingSolutions</solution></brokenCantBeFixed>
	 * <broken><name>accountDisplayName</name><dn>unescapedAccountDN</dn><solution>proposingSolutions</solution></broken>
	 * <limited><name>accountDisplayName</name><dn>unescapedAccountDN</dn><solution>proposingSolutions</solution></limited>
	 * <enabled><name>accountDisplayName</name><dn>unescapedAccountDN</dn></enabled>
	 * 
	 */
	public String getAUserStatusDetailsInXMLString(String userDN){
		logger.debug("start checking the account status details for user: " + userDN);
		
		// create xml string that stores data and has root <response> for returning to the caller of this method
		StringBuffer sfXml = new StringBuffer();
		sfXml.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
		sfXml.append("<response>");
				
		// prepare a thread to process this account.
		// we use this thread because we want to reuse the thread that process account in bulk (a list of Attributes)
		// so, we make a list of a single Attribute (that represents this userDN) and let this thread to process
		Attributes attrs = lt.getUserAttributes(userDN);
		List<Attributes> attrsL = new ArrayList<>();
		attrsL.add(attrs);
		ThreadProcessingAttribute t = new ThreadProcessingAttribute(attrsL);
		t.start();
		try {
			t.join(); //we want main thread to wait for this thread 
		} catch (InterruptedException e) {
		}
		
		String rslt = t.getThisThreadXMLResult();
		sfXml.append(rslt);
		sfXml.append("</response>");
		
		logger.debug("result of user: " + userDN + " is: " + sfXml.toString());
		lt.close();
		
		return sfXml.toString();
	}
	
	/**
	 * fix the given account (give as a unescaped userDN) and return the result of the fixing process.
	 * so, the result will contains the information of the what issues has been fixed and what issues could not be fixed.
	 * @param userDN must has not been escaped (special chars)
	 * @return a result String which represents the XML tree of the response. its root is <response> and there are always
	 * two children which are: 
	 * <name>displayNameOfTheAccountThatHasGoneThroughTheProcess</name>
	 * <dn>dnNameOfTheAccount</dn>. 
	 * Then there are two other optional children which are:
	 * <failedToFix>WhatCouldNotBeFixedWhichAreDelimitedByReturnCharDefinedInThisClass</failedToFix> 
	 * <fixed>WhatHaveBeenFixedWhichAreDelimitedByReturnCharDefinedInThisClass</fixed>
	 * @throws NamingException 
	 */
	public String fixAccountForGivenUser(String userDN) throws NamingException{
		logger.debug("Start fixing the account: " + userDN);
		
		// getting the Attributes object that represent the Ldap account for the given userDN
		Attributes attrs = lt.getUserAttributes(userDN);
		String unescapedUserDN = (String)Rdn.unescapeValue((String)attrs.get("distinguishedname").get());
		String displayName = (String) attrs.get("cn").get();
		String fixedRslts = "";
		String failedRslts = "";
		
		ThreadProcessingAttribute  th = new ThreadProcessingAttribute();
		
		// we have three different types of problems and fixing processes
		// so, we will store them in these rslts array
		String[] rslts = new String[3];
		
		if(lt.isAccountDisabled(unescapedUserDN)){
			logger.debug("about to fix a disabled account");
			// if this account has been disabled.
			// we fix only broken in disabling the account (we are not fixing any other broken issues)
			rslts[0] = th.checkingAndFixingForDisabledLdapAccount(true, attrs);
			rslts[1] = "";
			rslts[2] = "";
			
		} else {
			logger.debug("about to fix an eabled account");
			// if this account is activated
			// we fix all broken issues (because at this stage, we safely assume that there's no broken in disabling (because this account is not disabled))
			rslts[0] = "";
			rslts[1] = th.checkingAndFixingForBrokenAccount(true, attrs);
			rslts[2] = th.checkingAndFixingForLimitedUser(true, attrs);
		}
		
		logger.debug("start preparing the XML result for sending to client");
		
		// convert the rslts (results list) to two different Strings
		// failedRslts : stores what could not be fixed and each issue is delimited by the RETURN_CHAR
		// fixedRslts : stores what have been fixed and each issue is delimited by the RETURN_CHAR
		for(String rslt : rslts){
			String[] indRslts = rslt.split("&&");
			if(rslt.contains("failed")){
				String indFailedRslt = indRslts[indRslts.length - 1].replace("failed:", "");
				failedRslts += indFailedRslt;
			}
			
			if(rslt.contains("passed")){
				String indPassedRslt = indRslts[0].replace("passed:", "");
				fixedRslts += indPassedRslt; 
			}
		}
		
		// create xml string that stores data for responding to the caller of this method
		StringBuffer sfXml = new StringBuffer();
		sfXml.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
		sfXml.append("<response>");
		
		String value = String.format("<name>%s</name><dn>%s</dn>", 
										StringEscapeUtils.escapeXml10(displayName), 
										StringEscapeUtils.escapeXml10(unescapedUserDN));
		sfXml.append(value);
		
		// we create this XML branch <fixed>%s</fixed> only if there are issues that have been fixed.
		if(!fixedRslts.isEmpty()){
			String temp = "Has been fixed: " + RETURN_CHAR + fixedRslts + RETURN_CHAR + RETURN_CHAR + RETURN_CHAR;
			value = String.format("<fixed>%s</fixed>", StringEscapeUtils.escapeXml10(temp));
			sfXml.append(value);
		}
		
		// we create this XML branch <failedToFix>%s</failedToFix> only if there are issues that could not be fixed. 
		if(!failedRslts.isEmpty()){
			String temp = "Failed to fixed: " + RETURN_CHAR + failedRslts;
			value = String.format("<failedToFix>%s</failedToFix>", StringEscapeUtils.escapeXml10(temp));
			sfXml.append(value);
		}
		
		// close the XML tree and response as a string
		sfXml.append("</response>");
		
		logger.debug("fixing account has been completed. the result of the process is: " + sfXml.toString());
		if(lt!=null) lt.close();
		
		return sfXml.toString();
	}
	
	
	
	/**
	 * This method is receiving a client name (client name means: organisation name or company name that stored in "Clients" folder of Ldap server).
	 * Then it iterate through each member that stored in this Client name folder (of Ldap server) and determine the account status of this each account.
	 * The account status can be: 
	 * 1). Enabled: nothing wrong with the account, 
	 * 2). Limited: nothing wrong with the account, but the account doesn't have corresponding Support Tracker account. Which means the user cannot has limitation in access to others features.
	 * 3). Disabled: account has been disabled in Ldap, Support Tracker DB and Concerto Portal. Because it has been disabled, so it doesn't mean that this account has no problem. 
	 * 			It means that if we enable this account back, and put this account through this method again, then we might found some broken issues.
	 * 4). Disabled Broken: this account has been disabled in Ldap server, but it has not been disabled in Support Tracker DB and/or it has not been disabled in Concerto Portal.
	 * 			It similar to "Disabled" accounts for other issues.
	 * 5). Broken: this account is enabled, but there are various issues with the account. However, those issues can be fixed programmatically
	 * 6). Broken and Cannot Be Fixed: this account is enabled, but the issues related this account cannot be fixed programmatically or it requires user intervention.
	 * 
	 *   All conditions to defined the account type (broken, limited or disabled) are described at: 
	 *   http://woki/display/~jordans/User+Account+Management+Pseudo+Code+for+SPT-1272
	 * 
	 * @param client : is the client name (must not have been escaped) or organisation name or company name (that stored in "Client" folder of Ldap server). But, it is not a DN of that client.
	 * 
	 * @return if there's no disruption in the processing, it will return a string that represent an XML, which its root name <response> and the children 
	 * of this <response> (there are only 1st level children, no deeper level) are: <brokenInDisabling>, <disabled>, <brokenCantBeFixed>, <broken>, <limited>, <enabled>.
	 * Those branches can be:
	 * <brokenInDisabling><name>accountDisplayName</name><dn>unescapedAccountDN</dn><solution>proposingSolutions</solution></brokenInDisabling>
	 * <disabled><name>accountDisplayName</name><dn>unescapedAccountDN</dn> <solution>proposingSolutions</solution></disabled>
	 * <brokenCantBeFixed><name>accountDisplayName</name><dn>unescapedAccountDN</dn><solution>proposingSolutions</solution></brokenCantBeFixed>
	 * <broken><name>accountDisplayName</name><dn>unescapedAccountDN</dn><solution>proposingSolutions</solution></broken>
	 * <limited><name>accountDisplayName</name><dn>unescapedAccountDN</dn><solution>proposingSolutions</solution></limited>
	 * <enabled><name>accountDisplayName</name><dn>unescapedAccountDN</dn></enabled>
	 * 
	 * @throws NamingException if there is a name that Ldap cannot validate or work with.
	 */
	public String getAllUsersOfClientInXMLString(String client) throws NamingException {
	
		logger.debug("about to get all the users that are stored in this Ldap's Clients folder: " + client);
		
		// preparing an escaped DN name of this client name
		String name = Rdn.escapeValue(client);
		String clientDN = "OU="+name+","+LdapProperty.getProperty(LdapConstants.CLIENT_DN);

		// get all the accounts that stored under (member of) this client as a list of Attributes object
		// each Attributes object (each element of this list) represents the data of each account 
		List<Attributes> attrsList = lt.getAllUsersAsAttributesListFromClient(clientDN);

		// create xml string that stores data and has root <response> for returning to the caller of this method
		StringBuffer sfXml = new StringBuffer();
		sfXml.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
		sfXml.append("<response>");
		
		
		
		logger.debug("about to determine the account status of all the accounts of client: " + client);
		logger.debug("Creating and preparing multi threads to process all the user accounts.");
		
		/**
		 * because the whole process for each account is very time consuming
		 * and each account is independent from each other and the whole process are read only (no write process)
		 * so, we do multi threading here, where we create the number of threads equals to number of processors
		 * and each thread processing an equal number of accounts.
		 */
		
		int numbCPUs = Runtime.getRuntime().availableProcessors();
		int temp = 0;
		
		// prepare a list of accounts for each thread
		ArrayList<Attributes>[] attrsListForAllThreads = new ArrayList[numbCPUs];
		for(Attributes attr : attrsList){
			if(attrsListForAllThreads[temp]==null) attrsListForAllThreads[temp] = new ArrayList<Attributes>();
			
			attrsListForAllThreads[temp].add(attr);
			temp = ++temp % numbCPUs;
		}
		
		// a list that store all the threads that are processing the accounts
		ArrayList<ThreadProcessingAttribute> allThreads = new ArrayList<ThreadProcessingAttribute>();
		// create each thread and pass the list of accounts that it need to handle to its constructor.
		for(final ArrayList<Attributes> attrsListForAThread : attrsListForAllThreads){
			if(attrsListForAThread!=null && attrsListForAThread.size()>0){
				ThreadProcessingAttribute t = new ThreadProcessingAttribute(attrsListForAThread);
				t.start();
				allThreads.add(t);
				
				
				// prepare log
				String accts = "";
				for(Attributes attrs : attrsListForAThread){
					accts += (String) attrs.get("sAMAccountName").get();
				}
				logger.debug("Thread: " + t.getId() + " is processing accounts: " + accts);
			}
		}
		
		// waiting for all threads to finish together
		for(ThreadProcessingAttribute t : allThreads){
			try {
				t.join();
			} catch (InterruptedException e) {
				// we are not doing anything, because we just want to get only any successful threads
			}
		}
		
		// each thread is holding the result by its own (without sharing)
		// so collect all the results and add into a single "sfXml" result
		for(ThreadProcessingAttribute t : allThreads){
			String s = t.getThisThreadXMLResult();
			sfXml.append(s);
			logger.debug("Thread: " + t.getId() + " prodcued result: " + s);
		}
		
		

		
		// close the <response> root of XML and return it as a string
		sfXml.append("</response>");
		logger.debug("account status determination has been finished: " + sfXml.toString());
		if(lt!=null) lt.close();
		
		return sfXml.toString();
	}
	
	

	
	
	
	/**
	 * check if the Ldap account that is represented by the given Attribtues object, is a memberOf the given escapedGroupDN
	 * @param attributes object that represetns a Ldap account
	 * @param escapedGroupDN: an escaped (special chars) group DN that used to check
	 * @return true if the given Attributes object is a memberOf the given escapedGroupDN. false otherwise
	 * @throws NamingException
	 */
	public static boolean isGivenLdapAttrsHasMemberOf_checkNestedly(Attributes attributes, String escapedGroupDN) {
		ThreadProcessingAttribute t = new ThreadProcessingAttribute();
		try {
			return t.isGivenAttrsHasMemberOf_checkNestedly(attributes, escapedGroupDN);
		} catch (NullPointerException | NamingException e) {
			// NullPointerException thrown when there's no memberOf property
			return false;
		}
	}
}


































/**
 * this class is a helper thread that process all the object in the given Attributes list (passed through the Constructor param).
 * Each Attributes object represent a Ldap account. This thread try to determine the account status of each account and its result
 * is stored into its own "sfxmlOfThisThread" object (StringBuffer object). it also provides a method that return this result as a String.
 */
class ThreadProcessingAttribute extends Thread{
	// a list of Attributes object, where each Attributes object represent an Ldap account.
	List<Attributes> attrsListForThisThread = null;
	// result of the accounts statuses of all the accounts that are stored as Attributes(es) in the attrsListForThisThread 
	StringBuffer sfxmlOfThisThread = new StringBuffer();
	
	LdapTool lt = null;
	ConcertoAPI concerto = null;
	
	Logger logger = Logger.getRootLogger();
	
	
	
	private final String RETURN_CHAR = AccountStatusDetailsServlet.RETURN_CHAR;
	private final String ORIONSTAFFS_DN = "CN=OrionStaffs,OU=Clients,DC=orion,DC=dmz";
	private final String DOMAINADMIN_DN = "CN=Domain Admins,CN=Users,DC=orion,DC=dmz";
	private final String LDAPUSERS_DN = LdapProperty.getProperty(LdapConstants.GROUP_LDAP_USER);
	private final String LDAPCLIENTS_DN = LdapProperty.getProperty(LdapConstants.GROUP_LDAP_CLIENT);
	private final String[] CONCERTO_ADMIN_LIST = new String[] { "Administrators", "Support Tracker Administrators", 
																"Administrators [Migration]", "Records Administrators" };
	private final List<String> ORION_ROLES_LIST = LdapTool.readGroupsAndPermissionLevelFromConfigureFile();
	private final String ORION_HIGHEST_ROLE_DN = "CN=" + ORION_ROLES_LIST.get(0)+",OU=Orion Health,OU=Clients,DC=orion,DC=dmz";
	private final String ORION_LOWEST_ROLE_DN = "CN=" + ORION_ROLES_LIST.get(ORION_ROLES_LIST.size()-1) + ",OU=Orion Health,OU=Clients,DC=orion,DC=dmz";
	private final String CONCERTO_CLIENT = "Clients";
	
	
	
	/**
	 * Constructor
	 * @param attrsList : a list of Attributes object, where each Attributes object represent an Ldap account.
	 */
	public ThreadProcessingAttribute(List<Attributes> attrsList){
		this();
		
		attrsListForThisThread = attrsList;
	}
	
	
	public ThreadProcessingAttribute(){
		attrsListForThisThread = new ArrayList<Attributes>();
		try {
			lt = new LdapTool();
		} catch (FileNotFoundException | NamingException e) {
			// we are not doing anything because we no that it would not happened
			// if it would happen, it should already happened before this method is called 
		}
		
		try{
			concerto = new ConcertoAPI();
		} catch (MalformedURLException e){
			// we are not doing anything because we no that it would not happened
			// if it would happen, it should already happened before this method is called 
		}
	}
	
	
	/**
	 * @return the String result of the accounts statuses of all the accounts that this thread processing.
	 * Each result (for each account) is given as an XML branch which contains the <branchName>innerValue</branchName>.
	 * Those branches can be:
	 * <brokenInDisabling><name>accountDisplayName</name><dn>unescapedAccountDN</dn><solution>proposingSolutions</solution></brokenInDisabling>
	 * <disabled><name>accountDisplayName</name><dn>unescapedAccountDN</dn> <solution>proposingSolutions</solution></disabled>
	 * <brokenCantBeFixed><name>accountDisplayName</name><dn>unescapedAccountDN</dn><solution>proposingSolutions</solution></brokenCantBeFixed>
	 * <broken><name>accountDisplayName</name><dn>unescapedAccountDN</dn><solution>proposingSolutions</solution></broken>
	 * <limited><name>accountDisplayName</name><dn>unescapedAccountDN</dn><solution>proposingSolutions</solution></limited>
	 * <enabled><name>accountDisplayName</name><dn>unescapedAccountDN</dn></enabled>
	 */
	public String getThisThreadXMLResult(){
		return sfxmlOfThisThread.toString();
	}
	
	/**
	 * Invoke the thread to run and process all the accounts that stored as Attributes object attrsListForThisThread. Each result of each account is attached to sfxmlOfThisThread
	 * The status of each account can be: 
	 * <brokenInDisabling><name>accountDisplayName</name><dn>unescapedAccountDN</dn><solution>proposingSolutions</solution></brokenInDisabling>
	 * <disabled><name>accountDisplayName</name><dn>unescapedAccountDN</dn> <solution>proposingSolutions</solution></disabled>
	 * <brokenCantBeFixed><name>accountDisplayName</name><dn>unescapedAccountDN</dn><solution>proposingSolutions</solution></brokenCantBeFixed>
	 * <broken><name>accountDisplayName</name><dn>unescapedAccountDN</dn><solution>proposingSolutions</solution></broken>
	 * <limited><name>accountDisplayName</name><dn>unescapedAccountDN</dn><solution>proposingSolutions</solution></limited>
	 * <enabled><name>accountDisplayName</name><dn>unescapedAccountDN</dn></enabled>
	 */
	public void run(){
		// iterate through each account
		for(Attributes attrs : attrsListForThisThread){
			String unescapedUserDN;  // the userDN (that doesn't have escaped char for sepcial chars) of this account
			String displayName;		 // the display name of this account
			try {
				unescapedUserDN = (String)Rdn.unescapeValue((String)attrs.get("distinguishedname").get());
				displayName = (String) attrs.get("cn").get();
			} catch (NamingException e1) {
				// we are not doing anything because if there's no "CN" then this attr cannot be processed anyway
				continue;
			}
			
			// checking whether this account has been disbaled
			String proposingSolution = checkingAndFixingForDisabledLdapAccount(false, attrs);
			
			// if this account has been disabled and not a broken account, the proposingSolution must be an empty StringBuffer
			// if this account has not been disabled (account is enabled), the proposingSolution must be also an empty StringBuffer
			
			// so, if this proposingSolution is not empty, means this account is a disabled but broken 
			// (either support tracker account has not been disabled or concerto portal account has not been disabled, or both have not been disabled)
			// if this account is broken while it has been disabled, we dt need to check further (the further checks are checking only the broken in enabled account)
			if(!proposingSolution.isEmpty()){
				String value = String.format("<brokenInDisabling><name>%s</name><dn>%s</dn><solution>%s</solution></brokenInDisabling>",
						StringEscapeUtils.escapeXml10(displayName),
						StringEscapeUtils.escapeXml10(unescapedUserDN), 
						StringEscapeUtils.escapeXml10(proposingSolution));
				sfxmlOfThisThread.append(value);
			

			} else { // if this account a disabled and not broken  or  this account is enabled (we need to do further check)

				// if this account has been disabled, and the proposingSolution (so far) is an empty String buffer
				// means it is a disabled and good account (no issues).
				if(lt.isAccountDisabled(unescapedUserDN)){ 
					// account has been disabled properly 
					//(no other broken in disabling, 
					// but there can be other broken if it is re-activated, 
					// due to the broken status that have not been fixed, before it is disabled)
					String value = String.format("<disabled><name>%s</name><dn>%s</dn> <solution>%s</solution></disabled>",
							StringEscapeUtils.escapeXml10(displayName),
							StringEscapeUtils.escapeXml10(unescapedUserDN), 
							StringEscapeUtils.escapeXml10(proposingSolution) );
					sfxmlOfThisThread.append(value);
					
				
				// if this account is enabled and proposingSolution is empty, 
				// then we need to look for other issues that related to enabled account
				} else {
					
					proposingSolution += checkingAndFixingForBrokenAccount(false, attrs);
					
					// if proposingSolution is not empty, it means there are some broken issues
					if(!proposingSolution.isEmpty()){
						
						// if ISSUES_16D found, then d't need to check limited account. becuase it is duplicated
						if(proposingSolution.contains(Issues.ISSUE_16D)){
							
						} else {
							// If this account is broken, then we dont' care about the status of 'limited user'. 
							// It means that those limited users has to be counted as broken as well.
							proposingSolution += checkingAndFixingForLimitedUser(false, attrs);
						}
						
						
						if(proposingSolution.toLowerCase().contains("manual") || proposingSolution.toLowerCase().contains("can't be fixed")){
							String value = String.format("<brokenCantBeFixed><name>%s</name><dn>%s</dn><solution>%s</solution></brokenCantBeFixed>",
									StringEscapeUtils.escapeXml10(displayName),
									StringEscapeUtils.escapeXml10(unescapedUserDN), 
									StringEscapeUtils.escapeXml10(proposingSolution));
							sfxmlOfThisThread.append(value);
							
						} else {
							String value = String.format("<broken><name>%s</name><dn>%s</dn><solution>%s</solution></broken>",
									StringEscapeUtils.escapeXml10(displayName),
									StringEscapeUtils.escapeXml10(unescapedUserDN), 
									StringEscapeUtils.escapeXml10(proposingSolution));
							sfxmlOfThisThread.append(value);
						}
						
						
						
					// if proposingSolution is empty means, there's no broken issues. 
					}else {
						// so, we distinguish this account to be either: <limited> account or <enabled> account
						proposingSolution = checkingAndFixingForLimitedUser(false, attrs);
						
						// if proposingSolution is not empty here, means this account his a <limtied> account
						if(!proposingSolution.isEmpty()){
							String value = String.format("<limited><name>%s</name><dn>%s</dn><solution>%s</solution></limited>",
													StringEscapeUtils.escapeXml10(displayName),
													StringEscapeUtils.escapeXml10(unescapedUserDN), 
													StringEscapeUtils.escapeXml10(proposingSolution));
							sfxmlOfThisThread.append(value);
							
						// otherwise, its a good account (no broken or limited)
						} else {
							String value = String.format("<enabled><name>%s</name><dn>%s</dn></enabled>", 
													StringEscapeUtils.escapeXml10(displayName),
													StringEscapeUtils.escapeXml10(unescapedUserDN));
							sfxmlOfThisThread.append(value);
						}
					}
				}
			}
		}
	}
	
	
	
	
	/**
	 * A helper method that will check (or check and fix) the issues related to disabled Ldap account. 
	 * 
	 * @param fixing define whether or not the method should fix the broken issues of the account (given by account Attributes)
	 * or it should just check for the broken issues.
	 * 
	 * If fixing==true, then it will check and fix the broken issues that related to disabled accounts, and return the result of the process.
	 * If fixing==false, then it will only check for the broken issues that related to disabled accounts, and return the what need to be fixed.
	 *  
	 * Each issue in the return result is delimited by RETURN_CHAR defined in this class.
	 *  
	 * @param attrs: an Attributes object that represents the Ldap account for a user.
	 * 
	 * @return
	 * If fixing==true, return the result of the process.
	 * 		it will return a String that contains: 
	 * 		First part: it starts with "passed: " + " all the issues that have been fixed" (if there are issues that have been fixed). (if there is no issue that has been fixed, this part will be empty). each issue is delimited by RETURN_CHAR
	 * 		Middle part: contains "&&". this "&&" is used as the separator between issues that have been fixed and issues that couldn't bee fixed. (there will be always a separator here regardless of the first part (issues have been fixed) and second part (issues that couldn't be fixed)
	 * 		Second part: it starts with "failed: " + " all the issues that could not be fixed"  (if there are issues that have been fixed). (if there is no issue that has been fixed, this part will be empty). each issue is delimited by RETURN_CHAR
	 * 	
	 * If fixing==false, return what need to be fixed. Each issue in the return result is delimited by RETURN_CHAR defined in this class. 
	 *  
	 */
	public String checkingAndFixingForDisabledLdapAccount(boolean fixing, Attributes attrs) {
		//All conditions to defined the account type (broken, limited or disabled) are described at: 
		// http://woki/display/~jordans/User+Account+Management+Pseudo+Code+for+SPT-1272
		
		StringBuffer proposingSolution = new StringBuffer();
		ArrayList<String> fixedRslts = new ArrayList<>();
		ArrayList<String> failedToFixRslts = new ArrayList<>();
		
//		try{
//			// getting username (sAMAccountName Ldap field) and userDN
//			String username = (String) attrs.get("sAMAccountName").get();
//			String escapedUserDN = (String)attrs.get("distinguishedname").get();
//			String unescapedUserDN = (String)Rdn.unescapeValue(escapedUserDN);
//			
//			logger.debug("start checking/fixing 1-a, 1-b condition for account: " + username);
//			
//			// if this account is enabled in Ldap Server, then we are not going further, because in this method
//			// we are concerning only with the disabled account.
//			if(lt.isAccountDisabled(unescapedUserDN)){
//				
//
///**
// * 1-a: if account is disabled in Ldap, but it has not been disabled in Support Tracker DB 
// * 
// * Solution: we disable all the accounts in Support Tracker DB that match to this username 
// */
//				
//				try {
//
//					if(isAnyEnabledAccountsInSupportTrackerDB(username)){
//						proposingSolution.append(Issues.ISSUE_1A + RETURN_CHAR);
//						
//						if(fixing){
//							// disable account in both ClientAccount table and Staff table of support tracker db
//							// we disable all accounts that have match this username
//							// because if there are more than one accounts that match this username, 
//							// then those accounts will become broken. so, we will fix it later 
//							if(disableAllAccountWithGivenUsernameInSupportTrackerDB(username)){
//								fixedRslts.add(Issues.ISSUE_1A);
//							} else {
//								failedToFixRslts.add(Issues.ISSUE_1A);
//							}
//						}
//					}
//				} catch (SQLException e1) {
//					logger.error("exception when checking Support Tracker DB for: " + username);
//				}
//				
//				
///**
// * 1-b: if account is disabled in Ldap, but it has not been disabled in Concerto
// * 
// * Solution: we disable the Concerto account that matches this username
// */
//				try{
//					if(concerto.isAccountEnabled(username)){
//						proposingSolution.append(Issues.ISSUE_1B + RETURN_CHAR);
//						
//						if(fixing){
//						// disable account in concerto
//							try{
//								if(concerto.deleteAccountOfGivenUser(username)){
//									fixedRslts.add(Issues.ISSUE_1B);
//								} else {
//									failedToFixRslts.add(Issues.ISSUE_1B);
//								}
//							} catch (Exception e){
//								failedToFixRslts.add(Issues.ISSUE_1B);
//							}
//						}
//					}
//				}catch(Exception e){
//					logger.error("exception at checking and fixing for disabled ldap acct. " + e.getMessage(), e);
//				}
//			}
//		} catch (NamingException e){
//			logger.error("Unpredicted exception at checking for disabled account.", e);
//			proposingSolution.append("There's an error while checking for disbaled account, please look at the log for more detail." + RETURN_CHAR);
//		}
		
		// convert all the issues that have been fixed and the issues that could not be fixed into result string
		String result = convertFixedResultsListAndFailedToFixedResultsListToResultString(fixedRslts, failedToFixRslts);
		
		logger.debug("finished checking/fixing 1-a, 1-b condition.");
		
		// if fixing is true return the results of the fixing process
		// otherwise return only the proposing solution that need to be fixed
		return fixing ? result : proposingSolution.toString();
	}
	
	
	
	
	
	/**
	 * A helper method that will check (or check and fix) the issues related to enabled Ldap account. 
	 * 
	 * @param fixing define whether or not the method should fix the broken issues of the account (given by account Attributes)
	 * or it should just check for the broken issues.
	 * 
	 * If fixing==true, then it will check and fix the broken issues that related to enabled accounts, and return the result of the process.
	 * If fixing==false, then it will only check for the broken issues that related to enabled accounts, and return the what need to be fixed.
	 *  
	 * Each issue in the return result is delimited by RETURN_CHAR defined in this class.
	 *  
	 * @param attrs: an Attributes object that represents the Ldap account for a user.
	 * 
	 * @return
	 * If fixing==true, return the result of the process.
	 * 		it will return a String that contains: 
	 * 		First part: it starts with "passed: " + " all the issues that have been fixed" (if there are issues that have been fixed). (if there is no issue that has been fixed, this part will be empty). each issue is delimited by RETURN_CHAR
	 * 		Middle part: contains "&&". this "&&" is used as the separator between issues that have been fixed and issues that couldn't bee fixed. (there will be always a separator here regardless of the first part (issues have been fixed) and second part (issues that couldn't be fixed)
	 * 		Second part: it starts with "failed: " + " all the issues that could not be fixed"  (if there are issues that have been fixed). (if there is no issue that has been fixed, this part will be empty). each issue is delimited by RETURN_CHAR
	 * 	
	 * If fixing==false, return what need to be fixed. Each issue in the return result is delimited by RETURN_CHAR defined in this class. 
	 *  
	 */
	public String checkingAndFixingForBrokenAccount(boolean fixing, Attributes attrs){
		//All conditions to defined the account type (broken, limited or disabled) are described at: 
		// http://woki/display/~jordans/User+Account+Management+Pseudo+Code+for+SPT-1272
		
		StringBuffer proposingSolution = new StringBuffer();
		
		ArrayList<String> fixedRslts = new ArrayList<>();
		ArrayList<String> failedToFixRslts = new ArrayList<>();
		
		
		try{
			String username = (String) attrs.get("sAMAccountName").get();
			String escapedUserDN = (String)attrs.get("distinguishedname").get();
			String unescapedUserDN = (String)Rdn.unescapeValue(escapedUserDN);
			String company = lt.getUserCompany(unescapedUserDN);
			
			logger.debug("start checking/fixing 2 condition for account: " + username);
			
			

/** * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *  Checking for LDAP Special Memberships (OrionStaffs, LdapUsers, LdapClients, DomainAdmins)
  * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
			
	
	/**
	 * 2 - if this account is a memberOf OrionStaffs (in Ldap)
	 * 
	 * Solution: we remove the membership of OrionStaffs from this account
	 */
			// checking for OrionStaffs  (CN=OrionStaffs,OU=Clients,DC=orion,DC=dmz)
			logger.debug("start checking/fixing condition 2 for account: " + username);
			if (isGivenAttrsHasMemberOf(attrs,ORIONSTAFFS_DN)) {
				proposingSolution.append(Issues.ISSUE_2 + RETURN_CHAR);
					
				if(fixing){
					// remove 'CN=OrionStaffs,OU=Clients,DC=orion,DC=dmz'
					if(lt.removeUserFromAGroup(unescapedUserDN, ORIONSTAFFS_DN)){
						fixedRslts.add(Issues.ISSUE_2);
					} else {
						failedToFixRslts.add(Issues.ISSUE_2);
					}
				}
			}
			logger.debug("finished checking/fixing condition 2 for account: " + username);

	/**
	 * 3 - if this account is not a member of LdapUsers
	 * 
	 * Solution: we add the membership of LdapUsers to this account
	 */
			logger.debug("start checking/fixing condition 3 for account: " + username);
			if (!isGivenAttrsHasMemberOf_checkNestedly(attrs, LDAPUSERS_DN)) {
				proposingSolution.append(Issues.ISSUE_3  + RETURN_CHAR);

				if(fixing){
				// add 'LdapUsers
					String companyGroupDN = lt.getDNFromGroup(company);
					if(lt.addGroup1InToGroup2(companyGroupDN, LDAPUSERS_DN)){
						fixedRslts.add(Issues.ISSUE_3);
					} else {
						failedToFixRslts.add(Issues.ISSUE_3);
					}
				}
			}
			logger.debug("finished checking/fixing condition 3 for account: " + username);
			
			
	/**
	 * 4 - If this account is not stored in Orion Health folder of Ldap server (is not an Orion Health staff)
	 * 		but it has a membership of any Orion-% roles (in Ldap server). 
	 * 
	 * Solution: We remove all the memberships of all Orion-% roles from this account
	 */
			logger.debug("start checking/fixing condition 4 for account: " + username);
			if (!isGivenAttrsStoredInOrionHealth(attrs)
					&& isGivenAttrsMemberOfOrionHealthRoles(attrs)) {
				proposingSolution.append(Issues.ISSUE_4  + RETURN_CHAR);

				if(fixing){
				// remove that 'A-Group-Start-With-Orion'
					String baseDN = LdapProperty.getProperty("orionhealthOrganisationBasedDN");
					if(baseDN == null || baseDN.trim().isEmpty()) baseDN = "OU=Orion Health,OU=Clients,DC=orion,DC=dmz";
					
					boolean result = true;
					for (String orionRole : ORION_ROLES_LIST) {
						String orionRoleDN = "CN=" + orionRole + "," + baseDN;
						String escapedOrionRoleDN = "CN=" + Rdn.escapeValue(orionRole) + "," + baseDN; 
						if(isGivenAttrsHasMemberOf(attrs, escapedOrionRoleDN)){
							if(!lt.removeUserFromAGroup(unescapedUserDN, orionRoleDN)){
								result = false;
							}
						}
					}
					if(result){
						fixedRslts.add(Issues.ISSUE_4);
					} else {
						failedToFixRslts.add(Issues.ISSUE_4);
					}
				}
			}
			logger.debug("finished checking/fixing condition 4 for account: " + username);

			
			
	/**
	 * 5-a- if this account is stored in Orion Health folder (is Orion Health Staff) (in Ldap server)
	 * 		but it doesn't have any Orion-% role  (in Ldap server)
	 * 
	 * Solution: we add the lowest power of Orion-% role
	 */
			// ******************************************
			// LdapClients
			logger.debug("start checking/fixing condition 5-a for account: " + username);
			if (isGivenAttrsStoredInOrionHealth(attrs)) {
				if (!isGivenAttrsMemberOfOrionHealthRolesOtherThanSpecialRole(attrs)) {
					proposingSolution.append(Issues.ISSUE_5A + RETURN_CHAR);

					if(fixing){
					// add the lowest power Orion Health role (i.e. Orion Health - User [at the moment])

						if(lt.addUserToGroup(unescapedUserDN, ORION_LOWEST_ROLE_DN)){
							fixedRslts.add(Issues.ISSUE_5A);
						} else {
							failedToFixRslts.add(Issues.ISSUE_5A);
						}
					}
				}
				logger.debug("finished checking/fixing condition 5-a for account: " + username);
			
			
			
			
				logger.debug("start checking/fixing condition 5-b for account: " + username);
				if(isGivenAttrsMemberOfMoreThanOneOrionHealthRoles(attrs)){
					proposingSolution.append(Issues.ISSUE_5B + RETURN_CHAR);
					
					if(fixing){
						// keep the highest power of Orion Health role that this user is a memberOf
						
						if(keepTheHighestOrionRoleAndRemoveOthersExceptSpecialRole(attrs)){
							fixedRslts.add(Issues.ISSUE_5B);
						} else {
							failedToFixRslts.add(Issues.ISSUE_5B);
						}
					}
				}
				logger.debug("finished checking/fixing condition 5-b for account: " + username);
			
			
				
				
	/**
	 * 6- if this account is stored in Orion Health folder (is Orion Health Staff) (in Ldap server)
	 * 		but it has a membership of LdapClients (in Ldap server)
	 * 
	 * Solution: remove the LdapClients membership from this account
	 */
				logger.debug("start checking/fixing condition 6 for account: " + username);
				if (isGivenAttrsHasMemberOf(attrs, LDAPCLIENTS_DN)) {
					proposingSolution.append(Issues.ISSUE_6 + RETURN_CHAR);

					if(fixing){
					// remove 'LdapClients'
						if(lt.removeUserFromAGroup(unescapedUserDN, LDAPCLIENTS_DN)){
							fixedRslts.add(Issues.ISSUE_6);
						} else {
							failedToFixRslts.add(Issues.ISSUE_6);
						}
					}
				}
				logger.debug("finished checking/fixing condition 6 for account: " + username);
			}

			
	
			
	/**
	 * 7 - if this account is a memberOf DomainAdmins (in Ldap server), but its name is neither "Admin" nor "Administrator"
	 * 		and (either it is not stored in Orion Health folder (in Ldap server) or it is not a memberOf the highest power of the Orion-% roles (in Ldap server))
	 * 
	 * Solution: remove DomainAdmins from this account
	 */
			logger.debug("start checking/fixing condition 7 for account: " + username);
			if (isGivenAttrsHasMemberOf(attrs, DOMAINADMIN_DN)
					&& !isGivenAttrsNameAdminOrAdministrators(attrs)
					&& (!isGivenAttrsStoredInOrionHealth(attrs) 
							|| !isGivenAttrsHasMemberOf(attrs,ORION_HIGHEST_ROLE_DN))) {
				proposingSolution.append(Issues.ISSUE_7 + RETURN_CHAR);
				
				if(fixing){
					if(lt.removeUserFromAGroup(unescapedUserDN, DOMAINADMIN_DN)){
						fixedRslts.add(Issues.ISSUE_7);
					} else {
						failedToFixRslts.add(Issues.ISSUE_7);
					}
				}
			}
			logger.debug("finished checking/fixing condition 7 for account: " + username);
			
			
	/**
	 * 8- If this account is not a memberOf a group (Ldap Groups Folder) that has the same name
	 * 		as the Client (Ldap Clients folder) that this account is stored in
	 * 
	 * Solution: add this user to its folder-name group (in Ldap server)
	 */
			logger.debug("start checking/fixing condition 8 for account: " + username);
			if (!isGivenAttrsMemberOfGroupThatHasTheSameNameAsTheFolderItStoredIn(attrs)) {
				proposingSolution.append(Issues.ISSUE_8 + RETURN_CHAR);
				
				if(fixing){
					String groupDN = getGroupDnFromGivenAttributes(attrs);
					groupDN = (String) Rdn.unescapeValue(groupDN);
					
					try{
						if(lt.addUserToGroup(unescapedUserDN, groupDN)){
							fixedRslts.add(Issues.ISSUE_8);
						} else {
							failedToFixRslts.add(Issues.ISSUE_8);
						}
					} catch (NamingException e){
						failedToFixRslts.add(Issues.ISSUE_8);
					}
				}
			}
			logger.debug("finished checking/fixing condition 8 for account: " + username);
			
			
			
			

/** * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * Check Concerto Account (So, we check only if this username does exist in Concerto)
  * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

			if (concerto.doesUserExist(username)) {

				
	/**
	 * 9 - if this account exists in Concerto, 
	 * 		and (either it is not stored in (Ldap) Orion Health folder or it is not a memberOf (Ldap) highest power of Orion-% role)
	 * 		and its name is neither "Admin" nor "Administrator"
	 * 		and it is at least a member of (Concerto): {"Administrators", "Support Tracker Administrators", "Administrators [Migration]", "Records Administrators" }
	 * 
	 * Solution: remove any membership of these groups (Concerto): {"Administrators", "Support Tracker Administrators", "Administrators [Migration]", "Records Administrators" }
	 */
				logger.debug("start checking/fixing condition 9 for account: " + username);
				if (  (!isGivenAttrsStoredInOrionHealth(attrs) 
						|| !isGivenAttrsHasMemberOf(attrs,ORION_HIGHEST_ROLE_DN))
							&& !isGivenAttrsNameAdminOrAdministrators(attrs)
							&& concerto.isUserMemberOfAtLeastOneGroupInGivenGroupsList(username, CONCERTO_ADMIN_LIST)) {
					
					proposingSolution.append(Issues.ISSUE_9 + RETURN_CHAR);
					
					if(fixing){
						try{
							 if(concerto.removeAllGivenGroupsFromGivenUser(username, CONCERTO_ADMIN_LIST)){
								 fixedRslts.add(Issues.ISSUE_9);
							 } else {
								 failedToFixRslts.add(Issues.ISSUE_9);
							 }
						} catch (Exception e){
							 failedToFixRslts.add(Issues.ISSUE_9);
						}
					}
				}
				logger.debug("finished checking/fixing condition 9 for account: " + username);
				
				
	/**
	 * 10 - if this account exists in Concerto,
	 * 		and this account is not using Ldap Password
	 * 
	 * Solution: DO NOTHING: advise admin to contact user and ask them to change their password.
	 */
				logger.debug("start checking/fixing condition 10 for account: " + username);
				if (!concerto.isUserUsingLdapPassword(username)) {

					proposingSolution.append(Issues.ISSUE_10  + RETURN_CHAR);

					failedToFixRslts.add(Issues.ISSUE_10);
					// DO NOTHING: advise admin to contact user and ask them to change their own password
				}
				logger.debug("finished checking/fixing condition 10 for account: " + username);
			
				
				
				
				
	/**
	 * 11
	 */
				logger.debug("start checking/fixing condition 11-a, 11-b for account: " + username);
				if (concerto.isUserMemberOfGivenGroup(username, CONCERTO_CLIENT)
						&& !isGivenAttrsStoredInOrionHealth(attrs)) {
					
	
	/**
	 * 11-a - if this account exists in Concerto and this account is a member of (Concerto) Clients
	 * 			and this account is not a memberOf (Ldap) LdapClients
	 * 
	 * Solution: add LdapClients to Ldap account
	 */
					if(!isGivenAttrsHasMemberOf_checkNestedly(attrs, LDAPCLIENTS_DN)){
						proposingSolution.append(Issues.ISSUE_11A  + RETURN_CHAR);
						
						if(fixing){
							// add 'LdapClients' (if missing)
							
							if(lt.addUserToGroup(unescapedUserDN, LDAPCLIENTS_DN)){
								fixedRslts.add(Issues.ISSUE_11A);
							} else {
								failedToFixRslts.add(Issues.ISSUE_11A);
							}
						}
					}
					
					
					
	/**
	 * 11-b - if this account exists in Concerto and this account is a member of (Concerto) Clients
	 * 
	 * Solution: remove "Clients" from the Concerto account
	 */
					proposingSolution.append(Issues.ISSUE_11B + RETURN_CHAR);
					
					if(fixing){
						// remove 'Clients' (concerto)
						if(concerto.removeGroupFromUser(username, CONCERTO_CLIENT)){
							fixedRslts.add(Issues.ISSUE_11B);
						} else {
							failedToFixRslts.add(Issues.ISSUE_11B);
						}
					}
				}
				logger.debug("finished checking/fixing condition 11-a, 11-b for account: " + username);
				
				
				
	/**
	 * 12 - if this account exists in Concerto
	 * 		 and this account is a memberOf (Concerto) Clients
	 * 		 and this account is stored in (Ldap) Orion Health (Orion Health staff)
	 * 
	 * Solution: remove Cleints membership from Concerto account and remove LdapClients membership from Ldap account
	 */
				logger.debug("start checking/fixing condition 12 for account: " + username);
				if (concerto.isUserMemberOfGivenGroup(username, CONCERTO_CLIENT)
							&& isGivenAttrsStoredInOrionHealth(attrs)) {
					
					proposingSolution.append(Issues.ISSUE_12 + RETURN_CHAR);

					if(fixing){
						// remove 'Clients' (concerto)
						if(concerto.removeGroupFromUser(username, CONCERTO_CLIENT)
								// remove 'LdapClients'
								&& lt.removeUserFromAGroup(unescapedUserDN, LDAPCLIENTS_DN) ){
							fixedRslts.add(Issues.ISSUE_12);
						} else {
							failedToFixRslts.add(Issues.ISSUE_12);
						}

					}

				}
			}
			logger.debug("finished checking/fixing condition 12 for account: " + username);


			
			
			
/** * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * Check Support Tracker Account
  * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
	
	
	/**
	 * 13 - (this account stored in (Ldap) Orion Health folder and there's no support tracker Client account that matches this username)
	 * 		and (either this account is memberOf (Ldap) LdapClients or this account is a memberOf (Concerto) Clients)
	 * 
	 * Solution: add this user into clientAccount of Support Tracker DB  and update info field of this user's Ldap account based on returned clientAccountId.
	 */
			logger.debug("start checking/fixing condition 13 for account: " + username);
			// if u memberof 'ldapclients' && no ST client account)
			if (    (!isGivenAttrsStoredInOrionHealth(attrs) && !SupportTrackerJDBC.isAnySupportTrackerClientAccountMatchUsername(username))
			     && (isGivenAttrsHasMemberOf_checkNestedly(attrs, LDAPCLIENTS_DN) || concerto.isUserMemberOfGivenGroup(username, CONCERTO_CLIENT))) {
				
				proposingSolution.append(Issues.ISSUE_13 + RETURN_CHAR);

				if(fixing){
				// don't care about concerto account
				// add u into clientAccount of Support Tracker DB
				// update u.info (Ldap) using clientAccountId
					
					try{
						String[] results = createSupportTrackerClientAccountAndUpdateInfoFieldOfLdapAccount(attrs);
						if(results[0].equalsIgnoreCase("false")){
							failedToFixRslts.add(Issues.ISSUE_13);
						}else{
							fixedRslts.add(Issues.ISSUE_13);
						}
					} catch (NamingException | SQLException e){
						failedToFixRslts.add(Issues.ISSUE_13);
					}
				}
			}
			logger.debug("finished checking/fixing condition 13 for account: " + username);

			
			
	/**
	 * 14 - if this account is stored in (Ldap) Orion Health folder
	 * 		and there's no Staff account in Support Tracker that matches this username
	 * 
	 * Solution: add this staff into Staff of Support Tracker DB and update info field of this staff's Ldap account based on returned clientAccountId.
	 */
			logger.debug("start checking/fixing condition 14 for account: " + username);
			// if u memberof 'Orion%' && no ST staff account
			if (isGivenAttrsStoredInOrionHealth(attrs)
					&& SupportTrackerJDBC.getOrionHealthStaffDetails(username).isEmpty()) {
				
				proposingSolution.append(Issues.ISSUE_14 + RETURN_CHAR);

				if(fixing){
				// add u into staff of Support Tracker DB
				// update u.info (Ldap) using staffId
					try{
						String[] results = createSupportTrackerStaffAccountAndUpdateInfoFieldOfLdapAccount(attrs);
						if(results[0].equalsIgnoreCase("false")){
							failedToFixRslts.add(Issues.ISSUE_14);
						}else{
							fixedRslts.add(Issues.ISSUE_14);
						}
					} catch (NamingException | SQLException e){
						failedToFixRslts.add(Issues.ISSUE_14);
					}
				}
			}
			logger.debug("finished checking/fixing condition 14 for account: " + username);



			
	/**
	 * 15
	 */
			logger.debug("start checking/fixing condition 15-a, 15-b for account: " + username);
			if (SupportTrackerJDBC.isAnyActiveSupportTrackerClientAccountMatchUsername(username)
					&& SupportTrackerJDBC.isAnyActiveSupportTrackerStaffAccountMatchUsername(username)) {

	/**
	 * 15-a - if at least there is an active Client account of Support Tracker that match this username
	 * 			and at least there is an active Staff account of Support Tracker that match this username
	 * 			and this account is stored in (Ldap) Orion Health folder
	 * 
	 * Solution: disable all ClientAccounts (in Support Tracker DB) that match this username.
	 */
				if(isGivenAttrsStoredInOrionHealth(attrs)){
					proposingSolution.append(Issues.ISSUE_15A + RETURN_CHAR);
					
					if(fixing){
						// if Orion Health => disable all ClientAccount records
						// don't care how many records in ClientAccount table.
						// because if there are more than one, it will get into the below condition
						
						// match this username
						if(SupportTrackerJDBC.disableClientAccount(username)>0){
							fixedRslts.add(Issues.ISSUE_15A);
							
						} else {
							failedToFixRslts.add(Issues.ISSUE_15A);
						}
					}
					
					
					
	/**
	 * 15-B - if at least there is a Client account of Support Tracker that match this username
	 * 			and at least there is a Staff account of Support Tracker that match this username
	 * 			and this account is not stored in (Ldap) Orion Health folder
	 * 
	 * Solution: disable all Staff accounts (in Support Tracker DB) that match this username.
	 */
				} else {
					proposingSolution.append(Issues.ISSUE_15B + RETURN_CHAR);
					
				// if not(Orion Health) => disable all staff records match this username
				
					if(SupportTrackerJDBC.disableStaffAccount(username)>0){
						fixedRslts.add(Issues.ISSUE_15B);
					} else {
						failedToFixRslts.add(Issues.ISSUE_15B);
					}
				}
			}
			logger.debug("finished checking/fixing condition 15-a, 15-b for account: " + username);
			
			
			
			
			
	/**
	 * 16
	 */
			logger.debug("start checking/fixing condition 16-a, 16-b, 16-c for account: " + username);
			// Check for info=clientaccountid
			if (!isGivenAttrsStoredInOrionHealth(attrs)
					&& SupportTrackerJDBC.isAnySupportTrackerClientAccountMatchUsername(username)) {
				
				SortedSet<String> clientAcctIds = getAllClientAccountIdRecordsFromSupportTrackerThatMatch(username, company);

				
	/**
	 * 16-a - If this account not stored in (Ldap) Orion Health folder
	 * 		   and there is at least one Client Accounts in Support Tracker that matches this username
	 * 		   and there are more than one Client Accounts in Support Tracker that match both username and company
	 * 
	 * Solution: 1). pick the last account, update the "Info" field of the Ldap account using this last account's clientAccountId field (of Support Tracker)
	 * 			 2). disable all other accounts  
	 */
				if (clientAcctIds.size() > 1) {
					
					proposingSolution.append(Issues.ISSUE_16A + RETURN_CHAR);
					
					if (fixing) {
						String lastId = clientAcctIds.last();
						// disable all acounts with this username, then enable only the one that has lastId
						// so, it means we disabled all accounts, except the lastId one.
						SupportTrackerJDBC.disableClientAccount(username);
						SupportTrackerJDBC.enableClientAccount(username, lastId);

						// update u.info using lastId
						Map<String, String[]> updateMaps = new HashMap<>();
						updateMaps.put("dn", new String[] { unescapedUserDN });
						updateMaps.put("Info", new String[] { "" + lastId });
						String[] results = lt.updateUser(updateMaps);
						if (results[0].equalsIgnoreCase("false")) {
							failedToFixRslts.add(Issues.ISSUE_16A);
						} else {
							fixedRslts.add(Issues.ISSUE_16A);
						}
					}

					
					
	/**
	 * 16-b - If this account not stored in (Ldap) Orion Health folder
	 * 		   and there is at least one Client Accounts in Support Tracker that matches this username
	 * 		   and there is only one Client Accounts in Support Tracker that matches both username and company
	 * 		   and the clientAccountId field of Support Tracker doesn't match the Info field of Ldap account
	 * 
	 * Solution:  update the "Info" field of the Ldap account using this account's clientAccountId field (of Support Tracker)  
	 */
				} else if (clientAcctIds.size() == 1) {
					if (attrs.get("info") == null || !attrs.get("info").get().toString().equals(clientAcctIds.last())) {
						
						proposingSolution.append(Issues.ISSUE_16B + RETURN_CHAR);

						if(fixing){
							// update u.info using lastId
							Map<String, String[]> updateMaps = new HashMap<>();
							updateMaps.put("dn", new String[] { unescapedUserDN });
							updateMaps.put("Info", new String[] { "" + clientAcctIds.last() });
							String[] results = lt.updateUser(updateMaps);
							if (results[0].equalsIgnoreCase("false")) {
								failedToFixRslts.add(Issues.ISSUE_16B);
							} else {
								fixedRslts.add(Issues.ISSUE_16B);
							}
						}
					}
	/**
	 * 16-c - If this account not stored in (Ldap) Orion Health folder
	 * 		   and there is at least one Client Accounts in Support Tracker that matches this username
	 * 		   and there is no Client Account in Support Tracker that matches both username and company name
	 * 
	 * Solution: Broken and Cannot be fixed programmatically
	 */
				} else {
					
					if(!SupportTrackerJDBC.getCompanyDetails(company).isEmpty()){
						proposingSolution.append(Issues.ISSUE_16C + RETURN_CHAR);
						failedToFixRslts.add(Issues.ISSUE_16C);
					
						
						
						
						
					// we can't match username and company name between ldap and ST DB
					// because there's no this company name in ST DB
	/**
	 * 16-d - If this account not stored in (Ldap) Orion Health folder
	 * 		   and there is at least one Client Accounts in Support Tracker that matches this username
	 * 		   and there is no Client Account in Support Tracker that matches both username and company name
	 * 		   and the reason that we cannot matches both username and company name, because there's no this company name in Suppor Tracker DB
	 * 
	 * Solution: Broken and Cannot be fixed programmatically
	 */
					} else {
						proposingSolution.append(Issues.ISSUE_16D + RETURN_CHAR);
						failedToFixRslts.add(Issues.ISSUE_16D);
					}					
				}
			}
			logger.debug("finished checking/fixing condition 16-a,b,c for account: " + username);

			
			
	/**
 	* 17
 	*/
//			logger.debug("start checking/fixing condition 17-a,b for account: " + username);
//			if (lt.isAccountEnabled(unescapedUserDN)) {
//				try {
//					if (!isGivenAttrsStoredInOrionHealth(attrs)) {
//						try {
//							if (SupportTrackerJDBC.isAnySupportTrackerClientAccountMatchUsername(username)
//								&& SupportTrackerJDBC.isClientAccountDisabled(username, clientAccountId)) {
//	/**
//	 * 17-a - If this account is enabled (in Ldap server)
//	 * 			and this account is not stored in (Ldap) Orion Health folder
//	 * 			and there is at least a client account in Support Tracker match this username
//	 * 			and this Client account is disabled in Support Tracker
//	 * 
//	 * Solution: activate this Client account of Support Tracker that match both username and ST's clientAccountId (with Ldap Info field) 
//	 */
//																
//								proposingSolution.append(Issues.ISSUE_17A + RETURN_CHAR);
//
//								if(fixing){
//									// update 'activate' in ClientAccount to 'Y'
//									if(SupportTrackerJDBC.enableClientAccount(username, clientAccountId)>0){
//										fixedRslts.add(Issues.ISSUE_17A);
//									} else {
//										failedToFixRslts.add(Issues.ISSUE_17A);
//									}
//								}
//
//								
//							}
//						} catch (SQLException sex) {
//							logger.error("Unpredicted exception", sex);
//						}
//								
//					} else {
//						
//						
//						
//	/**
//	 * 17-b - If this account is enabled (in Ldap server)
//	 * 			and this account is stored in (Ldap) Orion Health folder
//	 * 			and there is at least a Staff account in Support Tracker match this username
//	 * 			and this Staff accoutn is disabled in Support Tracker
//	 * 
//	 * Solution: activate this Staff account Support Tracker that match username 
//	 */
//						if (SupportTrackerJDBC.isAnySupportTrackerStaffAccountMatchUsername(username)
//								&& SupportTrackerJDBC.isStaffAccountDisabled(username)) {
//
//							proposingSolution.append(Issues.ISSUE_17B + RETURN_CHAR);
//													
//							if(fixing){
//								// update 'recordStatus' in Staff to 'Y'
//								try{
//									String staffId = (String) attrs.get("Info").get();
//									if(SupportTrackerJDBC.enableStaffAccount(username)>0){
//										fixedRslts.add(Issues.ISSUE_17B);
//									} else {
//										failedToFixRslts.add(Issues.ISSUE_17B);
//									}
//								} catch (NullPointerException e){
//									// exception will thrown when there's no staffId stored in Info field
//									failedToFixRslts.add(Issues.ISSUE_17B);
//								}
//							}
//						}
//						
//					} // close else
//					
//					logger.debug("finished checking/fixing condition 17-a,b for account: " + username);
//				} catch (SQLException e) {
//					logger.error("Unpredicted exception: ", e);
//				}
//
//				
//				
//				
//	/**
//	 * 17-c - If this account is enabled (in Ldap server)
//	 * 		and this account exist in Concerto
//	 * 		and this Concerto account is disabled
//	 * 
//	 * Solution: active this Concerto account
//	 */
//				logger.debug("start checking/fixing condition 17-c for account: " + username);
//				if (concerto.doesUserExist(username)
//						&& concerto.isAccountDisabled(username)) {
//						proposingSolution.append(Issues.ISSUE_17C + RETURN_CHAR);
//						
//					if(fixing){
//						if(concerto.setAccountEnabledForGivenUser(true, username)){
//							fixedRslts.add(Issues.ISSUE_17C);
//						} else {
//							failedToFixRslts.add(Issues.ISSUE_17C);
//						}
//					}
//				}
//				logger.debug("finished checking/fixing condition 17-c for account: " + username);
//				
//			}
			
			
		} catch (Exception e){
			logger.error("Unpredicted exception at checking for broken account.", e);
			proposingSolution.append("There's an error while checking for a broken account, please look at the log for more detail." + RETURN_CHAR);
		}
		
		
		// convert all the issues that have been fixed and the issues that could not be fixed into result string
		String result = convertFixedResultsListAndFailedToFixedResultsListToResultString(fixedRslts, failedToFixRslts);
		
		// if fixing is true return the results of the fixing process
		// otherwise return only the proposing solution that need to be fixed
		return fixing ? result : proposingSolution.toString();
	}
	
	
	
	
	/**
	 * A helper method that will check (or check and fix) the issues related to limited account. Limited account is the account that doesn't have any issues. but, there is only an account in Ldap, and there's no account in Support Tracker and/or there's no account in Concerto
	 * 
	 * @param fixing define whether or not the method should fix the broken issues of the account (given by account Attributes)
	 * or it should just check for the broken issues.
	 * 
	 * If fixing==true, then it will check and fix the broken issues that related to disabled accounts, and return the result of the process.
	 * If fixing==false, then it will only check for the broken issues that related to disabled accounts, and return the what need to be fixed.
	 *  
	 * Each issue in the return result is delimited by RETURN_CHAR defined in this class.
	 *  
	 * @param attrs: an Attributes object that represents the Ldap account for a user.
	 * 
	 * @return
	 * If fixing==true, return the result of the process.
	 * 		it will return a String that contains: 
	 * 		First part: it starts with "passed: " + " all the issues that have been fixed" (if there are issues that have been fixed). (if there is no issue that has been fixed, this part will be empty). each issue is delimited by RETURN_CHAR
	 * 		Middle part: contains "&&". this "&&" is used as the separator between issues that have been fixed and issues that couldn't bee fixed. (there will be always a separator here regardless of the first part (issues have been fixed) and second part (issues that couldn't be fixed)
	 * 		Second part: it starts with "failed: " + " all the issues that could not be fixed"  (if there are issues that have been fixed). (if there is no issue that has been fixed, this part will be empty). each issue is delimited by RETURN_CHAR
	 * 	
	 * If fixing==false, return what need to be fixed. Each issue in the return result is delimited by RETURN_CHAR defined in this class. 
	 *  
	 */
	public String checkingAndFixingForLimitedUser(boolean fixing, Attributes attrs){
		//All conditions to defined the account type (broken, limited or disabled) are described at: 
				// http://woki/display/~jordans/User+Account+Management+Pseudo+Code+for+SPT-1272
				
				/**
				 * limited user
				 */
				
				ArrayList<String> fixedRslts = new ArrayList<>();
				ArrayList<String> failedToFixRslts = new ArrayList<>();
				
				StringBuffer proposingSolution = new StringBuffer();
				
				try{
					String username = (String) attrs.get("sAMAccountName").get();
					String escapedUserDN = (String)attrs.get("distinguishedname").get();
					String unescapedUserDN = (String)Rdn.unescapeValue(escapedUserDN);
					String company = lt.getUserCompany(unescapedUserDN);

				/**
				 * if this company doesn't exist then we cannot check the Limited Account status
				 */
					if(SupportTrackerJDBC.getCompanyDetails(company).isEmpty()){
						proposingSolution.append(Issues.ISSUE_19 + RETURN_CHAR);
						failedToFixRslts.add(Issues.ISSUE_19);
						
						
					} else {
						logger.debug("start checking/fixing condition 18-a,b for account: " + username);
						if(!isGivenAttrsStoredInOrionHealth(attrs)
								&& !isGivenAttrsHasMemberOf_checkNestedly(attrs, LDAPCLIENTS_DN)
								){
	
							
							if(!SupportTrackerJDBC.isAnySupportTrackerClientAccountMatchUsername(username)){
								/**
								 * Limited user:
								 * 18-a - if this account is not stored in Orion Health folder (Ldap)
								 * 			and it doesn't have a memberOf LdapClients
								 * 			and there is no any Client Account in Support Tracker that match this username
								 * 
								 * Solution: create a support tracker account using the information from Ldap account and update "Info" field of Ldap account using the clientAccountId that just created.
								 */
								proposingSolution.append(Issues.ISSUE_18A + RETURN_CHAR);
								
								if(fixing){
								// add u into clientAccount of ST DB
								// update u.info using clientAccountId that just created
									try{
										if(lt.addUserToGroup(unescapedUserDN, LDAPCLIENTS_DN)){
											String[] results = createSupportTrackerClientAccountAndUpdateInfoFieldOfLdapAccount(attrs);
											if(results[0].equalsIgnoreCase("false")){
												failedToFixRslts.add(Issues.ISSUE_18A);
											}else{
												fixedRslts.add(Issues.ISSUE_18A);
											}
										} else {
											failedToFixRslts.add(Issues.ISSUE_18A);
										}
									} catch (NamingException | SQLException e){
										failedToFixRslts.add(Issues.ISSUE_18A);
									}
								}
							
							
							
							
							
							
								/**
								 * 18-b- continute from 18-a
								 * 			and if there's no Concerto account that match this username
								 * 
								 * Solution: create a Concerto account
								 */
								if(!concerto.doesUserExist(username)){
									proposingSolution.append(Issues.ISSUE_18B + RETURN_CHAR);
									
									if(fixing){
										// if u doesn't exist in concerto => add u into concerto
										Map<String, String[]> maps = convertAttributesToMapObject(attrs);
										try{
											concerto.addClientUser(maps);
											fixedRslts.add(Issues.ISSUE_18B);
										} catch (Exception e){
											failedToFixRslts.add(Issues.ISSUE_18B);
										}
									}
								}
						
						
							} else {
								/**
								 * Limited user:
								 * 18-c - if this account is not stored in Orion Health folder (Ldap)
								 * 			and it doesn't have a memberOf LdapClients
								 * 			and there is a Client Account in Support Tracker that match this username
								 * 
								 * Solution: Add LdapClients role for this user's Ldap  account.
								 */
								
								proposingSolution.append(Issues.ISSUE_18C + RETURN_CHAR);
								
								if(fixing){
									try{
										if(lt.addUserToGroup(unescapedUserDN, LDAPCLIENTS_DN)){
											fixedRslts.add(Issues.ISSUE_18C);
										} else {
											failedToFixRslts.add(Issues.ISSUE_18C);
										}
									} catch (NamingException e){
										failedToFixRslts.add(Issues.ISSUE_18C);
									}
								}
								
							}
						}
						
						logger.debug("finished checking/fixing condition 18-a,b,c for account: " + username);	
					}
				}catch(Exception e){
					logger.error("Unpredicted exception at checking for limited user.", e);
					proposingSolution.append("(18 Limited User) There's an error while checking for limited user, please look at the log for more detail." + RETURN_CHAR);
				}
				
				
				// convert all the issues that have been fixed and the issues that could not be fixed into result string
				String result = convertFixedResultsListAndFailedToFixedResultsListToResultString(fixedRslts, failedToFixRslts);
						
				// if fixing is true return the results of the fixing process
				// otherwise return only the proposing solution that need to be fixed
				return fixing ? result : proposingSolution.toString();
	}
	
	
	
	
	
	
	
	
	
	/**
	 * a helper method that convert the list of results of the issues that have been fixed and the issues that could not be fixed.
	 * @param fixedRslts : results of the issues that have been fixed.
	 * @param failedToFixRslts : issues that could not be fixed.
	 * @return a String that contains: 
	 * First part: it starts with "passed: " + " all the issues that have been fixed" (if there are issues that have been fixed). (if there is no issue that has been fixed, this part will be empty). each issue is delimited by RETURN_CHAR
	 * Middle part: contains "&&". this "&&" is used as the separator between issues that have been fixed and issues that couldn't bee fixed. (there will be always a separator here regardless of the first part (issues have been fixed) and second part (issues that couldn't be fixed)
	 * Second part: it starts with "failed: " + " all the issues that could not be fixed"  (if there are issues that have been fixed). (if there is no issue that has been fixed, this part will be empty). each issue is delimited by RETURN_CHAR
	 */
	private String convertFixedResultsListAndFailedToFixedResultsListToResultString(ArrayList<String> fixedRslts, ArrayList<String> failedToFixRslts){
		String result = "";
		if(fixedRslts.size() > 0){
			result += "passed:";
			for(String rslt : fixedRslts){
				result += rslt + RETURN_CHAR;
			}
		}
		result += "&&";
		if(failedToFixRslts.size() > 0){
			result += "failed:";
			for(String rslt : failedToFixRslts){
				result += rslt + RETURN_CHAR;
			}
		}
		return result;
	}
	
	
	
	
	
	
	
	
	
	
	
	/**
	 * check if the Ldap account that is represented by the given Attribtues object, is a memberOf the given escapedGroupDN
	 * @param attributes object that represetns a Ldap account
	 * @param escapedGroupDN: an escaped (special chars) group DN that used to check
	 * @return true if the given Attributes object is a memberOf the given escapedGroupDN. false otherwise
	 * @throws NamingException
	 */
	public boolean isGivenAttrsHasMemberOf(Attributes attributes, String escapedGroupDN) throws NamingException {
		try {
			ArrayList<String> listMemberOf = (ArrayList<String>) Collections.list(attributes.get("memberOf").getAll());
			return listMemberOf.contains(escapedGroupDN);
		} catch (NullPointerException e) {
			// NullPointerException thrown when there's no memberOf property
			return false;
		}
	}
	
	/**
	 * check if the Ldap account that is represented by the given Attribtues object, is a memberOf the given escapedGroupDN
	 * it will check all the nested groups up to 3 level (e.g. this given acct is a memberOf group {A, B, C} then it will check the groups that A, B, C belongs to and so on).  
	 * @param attributes object that represetns a Ldap account
	 * @param escapedGroupDN: an escaped (special chars) group DN that used to check 
	 * @return true if the given Attributes object is a memberOf the given escapedGroupDN. false otherwise
	 * @throws NamingException
	 */
	public boolean isGivenAttrsHasMemberOf_checkNestedly(Attributes attributes, String escapedGroupDN) throws NamingException{
		try{ 
			ArrayList<String> firstLevelListMemberOf = (ArrayList<String>) Collections.list(attributes.get("memberOf").getAll());
			HashSet<String> listMemberOf = new HashSet<>(firstLevelListMemberOf);
			
			HashSet<String> secondLevelListMemberOf = new HashSet<>();
			for(String escapedGrDN : firstLevelListMemberOf){
				try{ // if there's this group has no nested groups, then NullPointerException will thrown. then move on to the next one
					Attributes thisGroupAttrs = lt.getGroupAttributesOfGivenGroupDN(escapedGrDN);
					ArrayList<String> thisListMemberOf = (ArrayList<String>) Collections.list(thisGroupAttrs.get("memberOf").getAll());
					secondLevelListMemberOf.addAll(thisListMemberOf);
					listMemberOf.addAll(thisListMemberOf);
				} catch (NullPointerException e){}
			}
			
			for(String escapedGrDN : secondLevelListMemberOf){
				try{ // if there's this group has no nested groups, then NullPointerException will thrown. then move on to the next one
					Attributes thisGroupAttrs = lt.getGroupAttributesOfGivenGroupDN(escapedGrDN);
					ArrayList<String> thisListMemberOf = (ArrayList<String>) Collections.list(thisGroupAttrs.get("memberOf").getAll());
					listMemberOf.addAll(thisListMemberOf);
				} catch (NullPointerException e){}
			}
			
			return listMemberOf.contains(escapedGroupDN);
		}catch (NullPointerException e){
			// NullPointerException thrown when there's no memberOf property
			return false;
		}
	}
	
	
	/**
	 * check if the Ldap account that is represented by the given Attribtues object, is a memberOf any Orion-% roles
	 * @param attributes object that represetns a Ldap account
	 * @return true if the given attributes is a memberOf at least one Orion-% role
	 * @throws NamingException
	 */
	private boolean isGivenAttrsMemberOfOrionHealthRoles(Attributes attributes) throws NamingException{
		List<String> orionRoles = LdapTool.readGroupsAndPermissionLevelFromConfigureFile();
		String baseDN = LdapProperty.getProperty("orionhealthOrganisationBasedDN");
		if(baseDN == null) baseDN = "OU=Orion Health,OU=Clients,DC=orion,DC=dmz";
		
		for(String orionRole : orionRoles){
			String orionRoleDN = "CN=" + Rdn.escapeValue(orionRole) + "," +baseDN;
			if(isGivenAttrsHasMemberOf(attributes, orionRoleDN)){
				return true;
			}
		}
		
		return false;
	}
	
	
	/**
	 * check if the Ldap account that is represented by the given Attribtues object, is a memberOf any Orion-% roles, but this checking is not counting the special role.
	 * (special role is a role defined in ldap.proerpties file. for example in the case I'm writing this code, 
	 * this special role is "Orion Health Additional - Reporting"). this means that if this user is a memberOf only this special role, then this method will return false.
	 * this method will return true only if there is at least one Orion-% role, which is not this special role.
	 * @param attributes object that represetns a Ldap account
	 * @return if this user is a memberOf only this special role, then this method will return false.
	 * this method will return true only if there is at least one Orion-% role, which is not this special role.
	 * @throws NamingException
	 */
	private boolean isGivenAttrsMemberOfOrionHealthRolesOtherThanSpecialRole(Attributes attributes) throws NamingException{
		List<String> orionRoles = LdapTool.readGroupsAndPermissionLevelFromConfigureFile();
		String baseDN = LdapProperty.getProperty("orionhealthOrganisationBasedDN");
		if(baseDN == null) baseDN = "OU=Orion Health,OU=Clients,DC=orion,DC=dmz";
		
		String specialOrionRoleThatCanBeKeptWithOtherOrionRoles = LdapProperty.getProperty("special.orionrole.canbekept.withother.orionroles");
		if(specialOrionRoleThatCanBeKeptWithOtherOrionRoles == null) {
			specialOrionRoleThatCanBeKeptWithOtherOrionRoles = "Orion Health Additional - Reporting";
		}
		
		for(String orionRole : orionRoles){
			if(orionRole.equalsIgnoreCase(specialOrionRoleThatCanBeKeptWithOtherOrionRoles)) continue;
			
			String orionRoleDN = "CN=" + Rdn.escapeValue(orionRole) + "," +baseDN;
			if(isGivenAttrsHasMemberOf(attributes, orionRoleDN)){
				return true;
			}
		}
		
		return false;
	}
	
	
	/**
	 * check if the ldap account that is represented by the given Attribute object, is a memberOf more than one Orion-% roles
	 * but this check is not including the special role  (special role is a role defined in ldap.proerpties file. for example in the case I'm writing this code, 
	 * this special role is "Orion Health Additional - Reporting"). This means that if the user is a memberOf one Orion...User role and this special role then the method will return false.
	 * This method will return only true if there are at least two Orion-% roles (and there's no the special role among these two). e.g. the user is a memberOf Orion...User and Orion...Regional.
	 * @param attributes object that represetns a Ldap account
	 * @return This means that if the user is a memberOf one Orion...User role and this special role then the method will return false.
	 * This method will return only true if there are at least two Orion-% roles (and there's no the special role among these two). e.g. the user is a memberOf Orion...User and Orion...Regional. 
	 * @return
	 */
	public boolean isGivenAttrsMemberOfMoreThanOneOrionHealthRoles(Attributes attributes){
		List<String> orionRoles = LdapTool.readGroupsAndPermissionLevelFromConfigureFile();
		
		String specialOrionRoleThatCanBeKeptWithOtherOrionRoles = LdapProperty.getProperty("special.orionrole.canbekept.withother.orionroles");
		if(specialOrionRoleThatCanBeKeptWithOtherOrionRoles == null) {
			specialOrionRoleThatCanBeKeptWithOtherOrionRoles = "Orion Health Additional - Reporting";
		}
		
		List<String> orionRolesWithoutSpecialRole = new ArrayList<>(orionRoles);
		orionRolesWithoutSpecialRole.remove(specialOrionRoleThatCanBeKeptWithOtherOrionRoles);
		
		String baseDN = LdapProperty.getProperty("orionhealthOrganisationBasedDN");
		if(baseDN == null) baseDN = "OU=Orion Health,OU=Clients,DC=orion,DC=dmz";
		
		boolean isMemberOfAnOrionRole = false;
		
		for(String orionRole : orionRolesWithoutSpecialRole){
			String orionRoleDN = "CN=" + Rdn.escapeValue(orionRole) + "," +baseDN;
			try {
				if(isGivenAttrsHasMemberOf(attributes, orionRoleDN)){
					// if isMemberOfAnOrionRole is true, it means that we have already found a memberOf orion role
					// so, now we are finding another one. so, this user has more than one Orion Roles
					if(!isMemberOfAnOrionRole) isMemberOfAnOrionRole = true;
					else return true;
				}
			} catch (NamingException e) {
			}
		}
		
		return false;
	}
	
	
	/**
	 * this method will assume that the given user (represented by the given Attributes object) is a memberOf more than one Orion-% role 
	 * (which is not including special role that defined in the ldap.properties. at the time i'm writing this code, this special role is "Orion Health Additional - Reporting").
	 * So, this method will keep only the higest power Orion-% roles that this user is a memberOf, then it will remove all other oles, except the special role.  
	 * 
	 * @param attributes object that represetns a Ldap account
	 * @return if the method processed successfully
	 */
	public boolean keepTheHighestOrionRoleAndRemoveOthersExceptSpecialRole(Attributes attributes) {
		try{
			String escapedUserDN = (String)attributes.get("distinguishedname").get();
			String unescapedUserDN = (String)Rdn.unescapeValue(escapedUserDN);
			
			List<String> orionRoles = LdapTool.readGroupsAndPermissionLevelFromConfigureFile();
			
			String specialOrionRoleThatCanBeKeptWithOtherOrionRoles = LdapProperty.getProperty("special.orionrole.canbekept.withother.orionroles");
			if(specialOrionRoleThatCanBeKeptWithOtherOrionRoles == null) {
				specialOrionRoleThatCanBeKeptWithOtherOrionRoles = "Orion Health Additional - Reporting";
			}
			
			List<String> orionRolesWithoutSpecialRole = new ArrayList<>(orionRoles);
			orionRolesWithoutSpecialRole.remove(specialOrionRoleThatCanBeKeptWithOtherOrionRoles);
			
			String baseDN = LdapProperty.getProperty("orionhealthOrganisationBasedDN");
			if(baseDN == null) baseDN = "OU=Orion Health,OU=Clients,DC=orion,DC=dmz";
			
			boolean hasHighestRoleBeenKept = false;
			
			for(int i=0; i<orionRolesWithoutSpecialRole.size(); i++){
				String orionRole = orionRolesWithoutSpecialRole.get(i);
				String orionRoleDN = "CN=" + orionRole + "," + baseDN;
	
				if (isGivenAttrsHasMemberOf(attributes, orionRoleDN)) {
					if (hasHighestRoleBeenKept) {
						lt.removeUserFromAGroup(unescapedUserDN, orionRoleDN);
					} else {
						hasHighestRoleBeenKept = true;
					}
	
				}
			}
	
			return true;
		} catch (NamingException e){
			return false;
		}
	}
	
	/**
	 * check if the Ldap account that is represented by the given Attribtues object, stored in Orion Health folder (in Ldap's Clients folder).
	 * if this Ldap account is stored in Orion Health folder, means this account is Orion Health staff account.
	 * @param attributes object that represetns a Ldap account
	 * @return true if the Ldap account taht is represented by the given attributes is stored in Orion Health folder
	 * @throws NamingException
	 */
	private boolean isGivenAttrsStoredInOrionHealth(Attributes attributes) throws NamingException{
		try{
			String escapedUserDN = (String)attributes.get("distinguishedname").get();
			// orionHealthDN should look like this one: OU=Orion Health,OU=Clients,DC=orion,DC=dmz
			String orionHealthDN = LdapProperty.getProperty(LdapConstants.ORION_HEALTH_ORG_NAME);
			
			//if escapedUserDN is in this form "CN=%displayName%,OU=Orion Health,OU=Clients,DC=orion,DC=dmz" 
			//=> it is an account stored in Orion Health
			return escapedUserDN.toLowerCase().contains(orionHealthDN.toLowerCase());
		} catch (NullPointerException e){
			// NullPointerException thrown when there's no "distinguishedname" property
			// if that's the case, this is the unusual case, so throw exception to let the user knows about this
			throw new NamingException("User's attributes are broken or incomplete.");
		}
	}

	
	/**
	 * check if the Ldap account that is represented by the given Attribtues object, is a memberOf a group (A group is stored in "Groups" folder of Ldap server)
	 * that has the same name as the folder (the folder that is stored in "Clients" folder of Ldap server) that it is stored.
	 * @param attributes object that represetns a Ldap account
	 * @return true if the Ldap account taht is represented by the given attributes is a memberOf a group that has the same name as the folder name that it is stored in.
	 * @throws NamingException
	 */
	private boolean isGivenAttrsMemberOfGroupThatHasTheSameNameAsTheFolderItStoredIn(Attributes attributes) throws NamingException{
		String groupDN = getGroupDnFromGivenAttributes(attributes);
		return isGivenAttrsHasMemberOf(attributes, groupDN);
	}
	
	
	
	/**
	 * this method is trying to get the folder name that this account (represented by given attributes object) is stored in
	 * (the folder is the last part of this account dn. e.g. CN=user display name,OU=folder name this user stored,OU=Clients,DC=orion,DC=dmz).
	 * then this method convert that folder name into the group name (which should be: CN=folder name this user stored,OU=Groups,DC=orion,DC=dmz).
	 * because a user is supposed to be a memberOf the group that has the same name as the folder that it is stored in.
	 * @param attributes object that represetns a Ldap account
	 * @return the String that represent the dn name of the group that has the same name as the folder that the given account is stored in.
	 * @throws NamingException
	 */
	private String getGroupDnFromGivenAttributes(Attributes attributes) throws NamingException {
		// getting the folder DN (distinguish name) that this user (represented by attributes param)
		// the user DN name is in the form: CN=user display name,OU=folder name this user stored,OU=Clients,DC=orion,DC=dmz
		// so the folder DN is "OU=folder name this user stored,OU=Clients,DC=orion,DC=dmz"
		// e.g. a user: "CN=Mal Sinyard,OU=Northumbria Healthcare NHS Foundation Trust,OU=Clients,DC=orion,DC=dmz"
		String escapeduserDN = (String) attributes.get("distinguishedname").get();
		// e.g. this folderDN = "OU=Northumbria Healthcare NHS Foundation Trust,OU=Clients,DC=orion,DC=dmz"
		String folderDN = escapeduserDN.substring(escapeduserDN.indexOf("OU="));

		// convert folderDN to group DN
		String groupDN = "CN=" + folderDN.substring("OU=".length());
		// e.g. this groupDN = "CN=Northumbria Healthcare NHS Foundation Trust,OU=Groups,DC=orion,DC=dmz"
		groupDN = groupDN.replace("OU=Clients", "OU=Groups");
		return groupDN;
	}
	
	
	/**
	 * check if the Ldap account's name (that is represented by the given Attribtues object) is "Admin" or "Administrator"
	 * @param attributes object that represetns a Ldap account
	 * @return true if the Ldap account's name is either "Admin" or "Administrator"
	 * @throws NamingException
	 */
	private boolean isGivenAttrsNameAdminOrAdministrators(Attributes attribtues) throws NamingException{
		String username = (String) attribtues.get("sAMAccountName").get();
		String displayName = (String) attribtues.get("cn").get();
		return username.equalsIgnoreCase("Administrator") || username.equalsIgnoreCase("Admin")
		|| displayName.equalsIgnoreCase("Administrator") || displayName.equalsIgnoreCase("Admin");
	}
	
	
	
	/**
	 * check if there is at least one account (either Client account or staff account) in Support Tracker match the given username and is activated.
	 * 
	 * @param username need to be checked
	 * @return true if there is an account (either Client account or staff account) in Support Tracker match the given username and is activated.
	 * @throws SQLException
	 */
	private boolean isAnyEnabledAccountsInSupportTrackerDB(String username) throws SQLException{
		String query = "SELECT *  FROM ClientAccount where loginName = ? and active = 'Y'";
		ResultSet rs = SupportTrackerJDBC.runGivenStatementWithParamsOnSupportTrackerDB(query, new String[]{username});
		if(rs.next()) return true;
		
		query = "SELECT * FROM Staff where loginName = ? and recordStatus = 'Y'";
		rs = SupportTrackerJDBC.runGivenStatementWithParamsOnSupportTrackerDB(query, new String[]{username});
		return rs.next();
	}
	
	
	
	/**
	 * disable all accounts (both client accoutn and staff account) that match the given username
	 * @param username that used to update 
	 * @return true if there is one or more accounts have been updated. false if there's no account has been updated.
	 * @throws SQLException
	 */
	private boolean disableAllAccountWithGivenUsernameInSupportTrackerDB(String username) throws SQLException{
		return SupportTrackerJDBC.disableClientAccount(username) + SupportTrackerJDBC.disableStaffAccount(username) > 0;
	}
	
	
	/**
	 * get all the clientAccountId from support tracker that match the given username and company name
	 * @param username 
	 * @param companyname
	 * @return a sorted list of clientAccountId (s) from support tracker that match the given username and company name
	 * @throws SQLException
	 */
	private SortedSet<String> getAllClientAccountIdRecordsFromSupportTrackerThatMatch(String username, String companyname) throws SQLException{
		String query = "SELECT clientAccountId FROM ClientAccount " +
							"WHERE loginName = ? " +
							"and clientId = (SELECT clientId FROM Client WHERE rtrim(ltrim(companyName)) = ?)";
		ResultSet rs = SupportTrackerJDBC.runGivenStatementWithParamsOnSupportTrackerDB(query, new String[]{username, companyname});
		SortedSet<String> clientAcctIds = new TreeSet<>();
		while(rs!=null && rs.next()){
			clientAcctIds.add(rs.getString("clientAccountId"));
		}
		return clientAcctIds;
	}
	
	
	/**
	 * disable all the accounts that have clientAccountId match with the given clientAccountId list
	 * @param clientAccountIds is a list that contains all the clientAccountId (s) that need to be disabled
	 * @return the number of the accounts that have been updated
	 * @throws SQLException
	 */
	private int disableAllGivenClientAccountIdsFromSupportTracker(Set<String> clientAccountIds) throws SQLException{
		String idsList = clientAccountIds.toString().replace("[", "(").replace("]", ")");
		String query = "UPDATE ClientAccount SET active = 'N' WHERE clientAccountId IN " + idsList;
		return SupportTrackerJDBC.runUpdateOfGivenStatementWithParamsOnSupportTrackerDB(query, new String[]{idsList});
	}
	
	
	/**
	 * convert an Attributes object (that represents an ldap account) to a map that can be used with LdapToo.addUser(map), SupportTracker.addClient(map) and ConcertoAPI.addClientUser(map)
	 * @param attributes object that represetns a Ldap account
	 * @return a Map<String, String[]> object that can be used to add a new account into Ldap server, Support Tracker and Concerto Portal
	 * @throws NamingException
	 */
	private Map<String, String[]> convertAttributesToMapObject(Attributes attributes) throws NamingException{
		final String[] mapKeys = {"sAMAccountName", "givenName", "sn", "displayName", "mobile", "info", 
									"description", "c", "department", "streetAddress", "l", "st",
									"postalCode", "mail", "telephoneNumber", "facsimileTelephoneNumber"};		
		
		Map<String, String[]> maps = new HashMap<>();

		for(String key : mapKeys){
			try{
				String value = (String)attributes.get(key).get();
				maps.put(key, new String[]{value});
			} catch (NullPointerException e){
				maps.put(key, new String[]{""});
			}
		}
		
		if(isGivenAttrsHasMemberOf_checkNestedly(attributes, LDAPCLIENTS_DN)){
			maps.put("isLdapClient", new String[]{"true"});
		} else {
			maps.put("isLdapClient", new String[]{"false"});
		}
		
		String escapedUserDN = (String)attributes.get("distinguishedname").get();
		String unescapedUserDN = (String)Rdn.unescapeValue(escapedUserDN);
		String company = lt.getUserCompany(unescapedUserDN);
		maps.put("company", new String[]{company});
		
		return maps;
	}
	
	
	/**
	 * this method using the given attributes object (represent a Ldap account) to create ClientAccount in Support Tracker,
	 * then it update the "Info" field of the Ldap account using the clientAccountId field that just created in Support Tracker
	 * @param attributes object that represetns a Ldap account
	 * @return a 2 elements array of String  represent the result of the process. the return string will be in two formats: {"false", "the reason of failure goes here"} or {"true", "some notes goes here"}
	 * @throws NamingException
	 * @throws SQLException
	 */
	private String[] createSupportTrackerClientAccountAndUpdateInfoFieldOfLdapAccount(Attributes attributes) throws NamingException, SQLException{
		String escapedUserDN = (String)attributes.get("distinguishedname").get();
		String unescapedUserDN = (String)Rdn.unescapeValue(escapedUserDN);
		
		Map<String, String[]> maps = convertAttributesToMapObject(attributes);
		if(maps==null || maps.isEmpty()){
			return new String[]{"false",
					"Couldn't use user's properties (from Ldap) to create Support Tracker ClientAccount."};
		}
		int newClientAcctId = SupportTrackerJDBC.addClient(maps);
		if(newClientAcctId < 0){
			return new String[]{"false","Couln't create Support Tracker ClientAccount."};
		} else {
			// update u.info (Ldap) using clientAccountId
			Map<String, String[]> updateMaps = new HashMap<>();
			updateMaps.put("dn", new String[]{unescapedUserDN});
			updateMaps.put("Info", new String[]{""+newClientAcctId});
			String[] results = lt.updateUser(updateMaps);
			if(results[0].equalsIgnoreCase("false")){
				return new String[]{"false",
						"A new Support Tracker ClientAccount has been created, here is it's clientId: " + newClientAcctId + ". But, the Info field of Ldap account could not be updated."};
			} else {
				return new String[]{"true",
						"A ClientAccount has been created in Support Tracker, and Info field of Ldap account has been updated."};
			}
		}
	}
	
	
	/**
	 * this method using the given attributes object (represent a Ldap account) to create Staff account in Support Tracker,
	 * then it update the "Info" field of the Ldap account using the staffId field that just created in Support Tracker
	 * @param attributes object that represetns a Ldap account
	 * @return a 2 elements array of String  represent the result of the process. the return string will be in two formats: {"false", "the reason of failure goes here"} or {"true", "some notes goes here"}
	 * @throws NamingException
	 * @throws SQLException
	 */
	private String[] createSupportTrackerStaffAccountAndUpdateInfoFieldOfLdapAccount(Attributes attributes) throws SQLException, NamingException{
		String escapedUserDN = (String)attributes.get("distinguishedname").get();
		String unescapedUserDN = (String)Rdn.unescapeValue(escapedUserDN);
		
		Map<String, String[]> maps = convertAttributesToMapObject(attributes);
		if(maps==null || maps.isEmpty()){
			return new String[]{"false",
					"Couldn't use user's properties (from Ldap) to create Support Tracker Staff account."};
		}
		int newStaffId = SupportTrackerJDBC.addStaffAccount(maps);
		if(newStaffId < 0){
			return new String[]{"false","Couln't create Support Tracker Staff account."};
		} else {
			// update u.info (Ldap) using clientAccountId
			Map<String, String[]> updateMaps = new HashMap<>();
			updateMaps.put("dn", new String[]{unescapedUserDN});
			updateMaps.put("Info", new String[]{""+newStaffId});
			String[] results = lt.updateUser(updateMaps);
			if(results[0].equalsIgnoreCase("false")){
				return new String[]{"false",
						"A new Support Tracker Staff account has been created, here is it's staffId: " + newStaffId + ". But, the Info field of Ldap account could not be updated."};
			} else {
				return new String[]{"true",
						"A Staff account has been created in Support Tracker, and Info field of Ldap account has been updated."};
			}
		}
	}
}