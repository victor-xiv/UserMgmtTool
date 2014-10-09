package ldap;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.ServiceUnavailableException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.apache.log4j.Logger;

public class LdapTool {
	private DirContext ctx;
	private Hashtable<String, String> env;
	
	Logger logger = Logger.getRootLogger(); // initiate as a default root logger
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	//
	// codes dealing with Orion Health Groups Permission
	//
	
	
	// this list stored, the groups that are stored in OU=Orion Health,OU=Clients,DC=orion,DC=dmz
	// the list is sorted based on its permission level
	// the group at index 0 is the highest power (can access to any other groups that have lower power)
	// the group at the end of the list (i.e. index size()-1) is the lowest power (cann't access to any other groups.
	// the permission level is configured in ldap.properties file
	private static ArrayList<String> orionHealthGroupsOrderedByPermissionLevel = null;
	
	
	/**
	 * 1). It read the entire configuration file (ldap.properties) and search for the attributes
	 *  ldap.group.permission.level.i : where i is the number from 0 to something and i determines the
	 *  the permission level. 
	 *  i.e. 
	 *  the lowest i (e.g. 0) is the one that has the highest power. so, any user that is a memberOf of this group, 
	 *      has the access right to any other groups that defined by lower i (e.g. i > 0)
	 *  the bigger of i means the lower power group. e.g. any user that is a memberOf of the group defined by i=3
	 *      cannot access to any groups that defined by i=0, 1 and 2
	 *  e.g. if a user that is a memberOf of multiple orion health groups (e.g. a memberOf i=2 and i=4) then that
	 *  user has the access right of the lowest power group (e.g. the group defined by i=4 if it is a memberOf groups that defined by i=2 and i=4)
	 * 2). then it creates an ArrayList<String> that store the name of that groups based on is hierarchy of i.
	 * which means that the i is used as the index of list.
	 * e.g. the group that is the value of attribute ldap.group.permission.level.3 is stored in the list at index 3
	 * 3). then this ArrayList<String> is assigned to the static field orionHealthGroupsOrderedByPermissionLevel for the later use.
	 */
	public static void readGroupsAndPermissionLevelFromConfigureFile(){
		Logger.getRootLogger().debug("About to read the groups with permission level from ldap.properties file");
		
		orionHealthGroupsOrderedByPermissionLevel = new ArrayList<String>();
		
		// this is the attribute name that is used in ldap.properties. please do not change this value (either here or in ldap.properties)
		final String GrpPermsnAttrName = "ldap.group.permission.level.";
		
		String prop="";
		HashMap<Integer, String> prmsLvlMap = new HashMap<Integer, String>();
		for(Enumeration<?> e = LdapProperty.propertyNames(); e.hasMoreElements(); prop=(String)e.nextElement()){
			if(prop.contains(GrpPermsnAttrName)){
				// permission level come with the form of:  "ldap.group.permission.level.3"
				// replace "ldap.group.permission.level" with "" so, we have "3" left
				int permissionLevel = Integer.parseInt(prop.replace(GrpPermsnAttrName, ""));
				String groupName = LdapProperty.getProperty(prop);
				prmsLvlMap.put(permissionLevel, groupName);
			}
		}
		
		//After completing adding the for loop
		// we should have prmsLvlMap = {1:"group1", 5:"group5", ...}
		// So, we sort its key to get [0, 1, 2, 3,...]
		// then we use this key to get a sorted list based on permission level, which should be: [group0, group1, group2, ...]
		SortedSet<Integer> sortedPrmsLvl = new TreeSet<Integer>(prmsLvlMap.keySet());
		for(int i : sortedPrmsLvl){
			String groupName = prmsLvlMap.get(i);
			orionHealthGroupsOrderedByPermissionLevel.add(groupName);
		}
		
		Logger.getRootLogger().debug("Finished reading the groups with permission level.");
	}
	
	
	/**
	 * This method look at the memberOf attributes (stored in Ldap server) of the given userDN
	 * and determine whether this given user is a memberOf of any orion health groups.
	 * 
	 * and it will return the list of orion health groups that this user has access right to access to those groups.
	 * 
	 * if it is not a memberOf of any orion health groups, then it will return an empty list.
	 * 
	 * e.g. if there are 10 orion health groups in total (this groups stored in orionHealthGroupsOrderedByPermissionLevel)
	 *    where the group at index 0 is the highest power who can access to any others groups that below it
	 *    and the group at the biggest index is the lowest power who cannot access to any groups at all.
	 *    
	 *    1). if it is a memberOf of the group at index i, then it will have access right to any groups that are at index >= i
	 *    so, the method will return a list that has the groups which is a sublist of orionHealthGroupsOrderedByPermissionLevel
	 *    from i to the last group.
	 *    
	 *    2). if it is not a memberOf of any group, it will return an empty list
	 *    
	 *    3). if it is a memberOf of many groups, then the highest power group will be used (or the group at the lowest index is used)
	 *    so, the method will return a list that has the groups which is a sublist of orionHealthGroupsOrderedByPermissionLevel
	 *    from i (where i is the lowest index that store the group name that this user is a memberOf) to the last group
	 *   
	 * @param unescappedUserDN: is the dn of the user and it must has not been escaped any chars at all 
	 * (e.g.     unescappedUserDN="CN=Mike+Jr,OU=Group, I,OU=Clients,DC=orion,DC=dmz"
	 *     not   unescappedUserDN="CN=Mike\\+Jr,OU=Group, I,OU=Clients,DC=orion,DC=dmz"
	 *     not   unescappedUserDN="Mike+Jr")  
	 * @return
	 */
	public List<String> getOrionHealthGroupsThisUserAllowToAccess(String unescappedUserDN){
		logger.debug("about to process the groups that this user: " + unescappedUserDN + " has the access right on.");
		if (unescappedUserDN != null && !unescappedUserDN.trim().isEmpty()) {
			try {
				// get all the attributes of the given unescappedUserDN
				// process attribute that has key "memberOf"
				// add the CN value of each group (each value of the memberOf attribute) to memberOfGroups
				Attributes attrs = getUserAttributes(unescappedUserDN);
				NamingEnumeration values = attrs.get("memberOf").getAll();
				ArrayList<String> memberOfGroups = new ArrayList<String>();
				while (values.hasMore()) {
					Object obj = values.next();
					if(obj instanceof String){
						String groupDN = (String)obj;
						memberOfGroups.add(LdapTool.getCNValueFromDN(groupDN));
					}
				}
				// when finishing this while loop
				// memberOfGroups should be s.th like this: ["ldap"]
				
				// d't want to modify value of  orionHealthGroupsOrderedByPermissionLevel
				// so, create a hard copy of orionHealthGroupsOrderedByPermissionLevel
				ArrayList<String> ohGroupsPermissionLevel = new ArrayList<String>(LdapTool.orionHealthGroupsOrderedByPermissionLevel);
				ArrayList<String> groupsThisUserAllowedToAccess = new ArrayList<String>();
				// iterate through the orionHealthGroupsOrderedByPermissionLevel
				// we are not iterating through the memberOfGroups list,
				// because we don't kn the size of memberOfGroups list. if the memberOfGroups list is too big, then it will slow
				// and we know orionHealthGroupsOrderedByPermissionLevel list is fix, thats why we are iterating through this list (not memberOfGroups)
				for(int i=0; i<ohGroupsPermissionLevel.size(); i++){
					String groupPermissionAtI = ohGroupsPermissionLevel.get(i);
					if(memberOfGroups.contains(groupPermissionAtI)){
						groupsThisUserAllowedToAccess = new ArrayList<String>(ohGroupsPermissionLevel.subList(i, ohGroupsPermissionLevel.size()));
						break;
					}
				}
				
				return groupsThisUserAllowedToAccess;
				
			} catch (Exception e) {
				logger.error("Error while processing Orion Health Groups that the user " + unescappedUserDN + " has access right on", e);
			}
		}
		
		logger.debug("finished the process of the groups that the given user has access rigth.");
		return new ArrayList<String>();
	}
	
	
	/**
	 * We want to enforce the permission level. Orion Health has a few groups that have different permission levels
	 * the groups sorted by permission level are stored in this orionHealthGroupsOrderedByPermissionLevel field
	 * e.g. in group at index 0 in orionHealthGroupsOrderedByPermissionLevel, has the highest power compare to those that are at other indexes.
	 * So, the given groups (in the list) are the groups that the user has the access rights on.
	 * 
	 * by using getBaseGroups() we will get all the groups that stored in Ldap servers and all the Orion Health groups.
	 * so, we will use getbaseGroups() and remove those Orion Health Groups that are not listed in the given groups (ohGroupsAllowedToBeAccessed)

	 * @param groupName that used to match to the list orionHealthGroupsOrderedByPermissionLevel.
	 * 		groupName is the name of the group, it is not the DN of the group
	 * (e.g. groupName is XX, but it is not CN=XX,OU=Client,DC=orion,DC=dmz)
	 * 		groupName is also a simple name without any escaped chars (e.g. it should "Group #1" not "Group \\#1"
	 * @return all groups that are stored in Groups folder (in Ldap server) 
	 * 			+ the groups in the list given as parameter
	 * 			+ those Orion Health groups that have lower power than those in the given list
	 */
	public Set<String> getBaseGroupsWithGivenOHGroupsAllowedToBeAccessed(List<String> ohGroupsAllowedToBeAccessed){
		logger.debug("about to get the base groups and the Orion Health groups");
		// get all groups that are stored in Groups folder (of LDAP server)
		ArrayList<String> allGroups = new ArrayList<String>(getBaseGroups());
		
		// remove all the Orion Health Groups (from the allGroups) that have the higher power than the groups in the given list
		ArrayList<String> ohGroupsHaveHigherPower = new ArrayList<String>(orionHealthGroupsOrderedByPermissionLevel);
		ohGroupsHaveHigherPower.removeAll(ohGroupsAllowedToBeAccessed);
		ArrayList<String> ohGroupsNotAllowedToBeAccessed = ohGroupsHaveHigherPower;
		allGroups.removeAll(ohGroupsNotAllowedToBeAccessed);

		logger.debug("finished getting the base groups and the Orion Health groups");
		return new LinkedHashSet<String>(allGroups);
	}
	
	
	
	//
	// codes dealing with Orion Health Groups Permission ends here
	//
	
	
	
	
	
	
	
	
	
	
	// no reserved chars in this method
	/**
	 * LdapTool represent the connection with LDAP server.
	 * The LdapTool object is successfully constructed if and only if it successfully connects to LDAP server.
	 * Otherwise, it will throw either FileNotFoundException or NamingException.
	 * 
	 * @throws FileNotFoundException when ldap.properties configuration file is nout found
	 * @throws NamingException when connection with LDAP server failed
	 */
	public LdapTool() throws FileNotFoundException, NamingException{
		if(orionHealthGroupsOrderedByPermissionLevel == null){
			readGroupsAndPermissionLevelFromConfigureFile();
		}
		
		
		logger.debug("About to connect to LDAP server");
		// if LDAP config file is not found
		// the props will contain an "error" key
		if(LdapProperty.getProperty("error") != null){
			logger.error("ldap.properties file is not found.");
			throw new FileNotFoundException("LDAP " + ErrorConstants.CONFIG_FILE_NOTFOUND);
			// don't need to log, because it has been logged in LdapProperty.getConfiguration()
		}
		
		// setup environment map to connect to ldap server
		env = new Hashtable<String, String>();
		env.put(Context.INITIAL_CONTEXT_FACTORY, LdapProperty.getProperty(LdapConstants.LDAP_CLASS));
		env.put(Context.PROVIDER_URL, LdapProperty.getProperty(LdapConstants.LDAP_URL));
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		env.put(Context.SECURITY_PRINCIPAL, LdapProperty.getProperty(LdapConstants.ADMIN_DN));
		env.put(Context.SECURITY_CREDENTIALS, LdapProperty.getProperty(LdapConstants.ADMIN_PWD));
		
		// if the config file declared to connect ldap server through ssl
		boolean sslEnabled = LdapProperty.getProperty(LdapConstants.SSL_ENABLED).equals("true");
		if( sslEnabled ){
			env.put(Context.SECURITY_PROTOCOL, "ssl" ); // SSL
			System.setProperty("javax.net.debug", "ssl");
			System.setProperty("javax.net.ssl.trustStore", LdapProperty.getProperty(LdapConstants.SSL_CERT_LOC));
			System.setProperty( "javax.net.ssl.trustStorePassword", LdapProperty.getProperty(LdapConstants.SSL_CERT_PWD));
			logger.debug("Connecting Ldap on SSL, using keystore: " + LdapProperty.getProperty(LdapConstants.SSL_CERT_LOC));
			logger.debug("Connecting Ldap on SSL, keystore password: " + LdapProperty.getProperty(LdapConstants.SSL_CERT_PWD));
		}
		
		try{
			ctx = new InitialDirContext(env);
			logger.debug("Connecting to Ldap server successfully");
		}catch(ServiceUnavailableException se){
			se.printStackTrace();
			logger.error("Connecting to LDAP server", se);
			throw new ServiceUnavailableException(ErrorConstants.LDAP_PORT_CLOSED);
			
		}catch(CommunicationException ce){
			String errorMessage = ErrorConstants.FAIL_CONNECTING_LDAP;
			logger.error("Connecting to LDAP server", ce);

			// Fail because the configured port in ldap.properties is incorrect
			if (ce.getRootCause().getMessage().contains("Connection reset")){
				errorMessage = ErrorConstants.INCORRECT_LDAP_PORT;

			// Fail because of SSL validation is incorrect			
			}else if(ce.getRootCause().getMessage().contains("PKIX path validation failed")){
				errorMessage = ErrorConstants.LDAP_SSL_HANDSHAKE_FAIL;
			}
			
			ce.printStackTrace();
			throw new CommunicationException(errorMessage);
			
		}catch(NamingException ex){
			logger.error("Connecting to LDAP server", ex);
			ex.printStackTrace();
			throw new NamingException(ErrorConstants.FAIL_CONNECTING_LDAP);
		}
	}
	

	// had escaped reserved chars
	/**
	 * update the Ldap's user with the given attributes (paramMaps). The userDN is the index 0 element of the value that mapped with key "dn"
	 * @param paramMaps: given attributes that need to be updated or deleted.
	 * all DN values in paramMaps,must have not been escaped value at all. (they will be escaped in this method body)
	 * @return: {"false","Failed to update details for user: "+userDN} if it failed to update.
	 *          {"true", userDN} or {"true", newUserDN} otherwise.
	 */
	public String[] updateUser(Map<String,String[]> paramMaps){
		logger.debug("About to update user with the value: ");
		for(Map.Entry<String, String[]> es : paramMaps.entrySet()){
			logger.debug(es.getKey() + " : " + es.getValue()[0]);
		}
		
		// userDN has not been escaped any reserved chars
		String userDN = paramMaps.get("dn")[0];
		
		Attributes attrs = getUserAttributes(userDN);
		// escaped the reserved chars.
		userDN = LdapTool.escapedCharsOnCompleteUserDN(userDN);
		
		if(attrs == null){
			return new String[]{"false","Get null value for the user's exisiting attributes."};
		}
		
		// replaceMap entries will be add to ModificationItem obj (the object that used to update Ldap server user's properties)
		// replaceMap stores only attributes that need to be replaced (updated)
		HashMap<String, String[]> replaceMap = new HashMap<String, String[]>();
		// removeList elements will be add to ModificationItem
		// removeList stores only elements that need to be removed from the Ldap user's properties
		ArrayList<String> removeList = new ArrayList<String>();
		
		try{
			// Iterate through paramMaps
			// if the value of each map entry is empty String "" => add its correspond key into removeList
			// if the value of a map entry equals to the existValue of attr => add that map entry into replaceMap
			for(Map.Entry<String, String[]>entry:paramMaps.entrySet()){
				try{
					String existValue = (attrs.get(entry.getKey())!=null ? attrs.get(entry.getKey()).get().toString() : "");
					if(entry.getValue()[0].equals("")){
						if(!existValue.equals("")){
							removeList.add(entry.getKey());
						}
					}else if(!entry.getValue()[0].equals(existValue)){
						replaceMap.put(entry.getKey(), entry.getValue());
					}
				} catch (NullPointerException e){
					logger.error("Null result from the given updating attributes.", e);
				}
			}
		}catch(NamingException ex){
			logger.error("Exception while iterating the given updating attributes.", ex);
			return new String[]{"false","Failed to extract information from the given attributes."};
		}
		
		// remove the password attribute from replaceMap, 
		// because we are not updating password with this replaceMap
		// but we are updating password in changePassword() method
		String password = "";
		if(replaceMap.get("password01") != null){
			password = replaceMap.get("password01")[0];
			replaceMap.remove("password01");
			replaceMap.remove("password02");
		}
		//String userDN = replaceMap.get("dn")[0];
		replaceMap.remove("dn");
		String newUserDN = "";
		if(replaceMap.containsKey("displayName")){
			String fullname = replaceMap.get("displayName")[0];
			String groupBaseDN = LdapProperty.getProperty(LdapConstants.GROUP_DN);
			
			// if replaceMap contains "company" key, then use this new key to update user
			if(replaceMap.containsKey("company")){
				newUserDN = "CN="+Rdn.escapeValue(fullname)+",OU="+Rdn.escapeValue(replaceMap.get("company")[0])+","+groupBaseDN;
			
			// if replaceMap doesn't contains "company" key, then use the previous one.
			} else {
				// used to find index of "OU=" and convert it in case it is written in lowercase
				String tempDN = userDN.toUpperCase();
				// userDN has already been escaped the reserved chars
				// so, we don't need to escape any reserved chars in companyDN once more
				int startIndexOfCompanyDN = tempDN.indexOf("OU=") + "OU=".length();
				int endIndexOfCompanyDN = tempDN.indexOf(",OU=", startIndexOfCompanyDN);
				String companyDN = userDN.substring(startIndexOfCompanyDN, endIndexOfCompanyDN);
				newUserDN = "CN=" + Rdn.escapeValue(fullname) + ",OU=" + companyDN + "," + groupBaseDN;
			} 
			
		}

		// This object is used to modify the user object in Ldap Server
		ModificationItem[] mods = new ModificationItem[replaceMap.size()+removeList.size()];

		// mods has length 0, means nothing to update
		if( mods.length == 0 ){
			return new String[]{"true",(String)Rdn.unescapeValue(userDN)};
		}
		
		// add every replaceMap's map entry into mods as the replace_attribute
		int i = 0;
		for(Map.Entry<String, String[]>entry:replaceMap.entrySet()){
			mods[i++] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(entry.getKey(), entry.getValue()[0]));
		}
		
		// add every removeList into mods as the remove_attribute
		for(Iterator<String> it = removeList.iterator(); it.hasNext(); ){
			String entry = it.next();
			mods[i++] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, new BasicAttribute(entry));
		}
		
		// try to update the userDN with the mods attributes
		try {
			if(!newUserDN.equals("")){
				ctx.rename(new LdapName(userDN), new LdapName(newUserDN));
				ctx.modifyAttributes(new LdapName(newUserDN), mods);
			}else{
				ctx.modifyAttributes(new LdapName(userDN), mods);
			}
			logger.debug("Updated details for user: "+userDN);
		} catch (NamingException e) {
			logger.error("Exception while updating the Ldap user's attribute", e);
			return new String[]{"false","Failed to update details for user: "+userDN + " into Ldap Server."};
		}
		
		// updating password => this.changePassword() will handle this
		if(!password.equals("")){
			if(!changePassword((String)Rdn.unescapeValue(userDN), password)){
				logger.debug("Failed to update password for user: "+userDN);
				return new String[]{"false","Failed to update password for user: "+userDN};
			}
		}
		if(!newUserDN.equals(""))
			return new String[]{"true",(String)Rdn.unescapeValue(newUserDN)};
		else
			return new String[]{"true",(String)Rdn.unescapeValue(userDN)};
	}
	
	
	
	
	/**
	 * add a user into organisational. i.e. in Orion LDAP, it will add a user to Clients directory (not groups)
	 * Generally, we use addUserToGroup() to add user into Group.
	 * Generally, we use addUser() first, then followed by addUserToGroup()
	 * @param paramMaps - parameters that will be used to build user's attributes
	 * @return
	 * @throws Exception 
	 */
	public boolean addUser(Map<String,String[]> paramMaps) throws Exception{		
		try{
			logger.debug("About to add a user with the value: ");
			for(Map.Entry<String, String[]> es : paramMaps.entrySet()){
				logger.debug(es.getKey() + " : " + es.getValue()[0]);
			}
			
			// http://forums.sun.com/thread.jspa?threadID=582103
			// http://msdn.microsoft.com/en-us/library/ms675090(VS.85).aspx
			String fullname = "";
			if(paramMaps.get("displayName")[0] != null){
				fullname = paramMaps.get("displayName")[0];
			}else{
				fullname = paramMaps.get("givenName")[0] + " " + paramMaps.get("sn")[0];
			}
			
			String companyName = paramMaps.get("company")[0];
			// if companyName doesn't exist in "Client" add the companyName into "Client"
			if(!companyExists(companyName)){
				if(!addCompany(companyName)){
					// if companyName doesn't exist in "Client" and can't be added, just return false;
					// throw new exception because the caller of this method will use this message (of this exception) as the result to inform to the user.
					throw new Exception("The company: " + companyName + " doesn't exist in LDAP's CLIENT directory and it can't be added into LDAP server's CLIENT directory.");
				}
			}
			// if company doesn't exist in LDAP's "Groups" add the company into "Groups"
			if (!companyExistsAsGroup(companyName)) {
				if (!addCompanyAsGroup(companyName)) {
					// if companyName doesn't exist in "Groups" and can't be added, just return false;
					// throw new exception because the caller of this method will use this message (of this exception) as the result to inform to the user.
					throw new Exception("The company: " + companyName + " doesn't exist in LDAP's GROUP directory and it can't be added into LDAP server's GROUP directory.");
				}
			}
			
			String groupBaseDN = LdapProperty.getProperty(LdapConstants.GROUP_DN);
			String unescapedValuecompanyDN = LdapProperty.getProperty(LdapConstants.GROUP_ATTR)+"="+companyName+","+groupBaseDN;
			String unescapedValueUserDN = "CN="+fullname+","+unescapedValuecompanyDN;
			String escapedValueCompanyDN = LdapProperty.getProperty(LdapConstants.GROUP_ATTR)+"="+Rdn.escapeValue(companyName)+","+groupBaseDN;
			String escapedValueUserDN = "CN="+Rdn.escapeValue(fullname)+","+escapedValueCompanyDN;
			Attributes attributes = new BasicAttributes(true);
			//attributes.put("distinguishedName", userDN);
			attributes.put("objectClass", "top");
			attributes.put("objectClass", "person");
			attributes.put("objectClass", "organizationalPerson");
			attributes.put("objectClass", "user");
	// sAMAccountName attribute allowed to have only these chars:  ( ) . - _ ` ~ @ $ ^
			// check if sAMAccountName contains any prohibited chars\
			String sAMAccountName = paramMaps.get("sAMAccountName")[0];
			String temp = sAMAccountName.replaceAll("[\\,\\<\\>\\;\\=\\*\\[\\]\\|\\:\\~\\#\\+\\&\\%\\{\\}\\?]", "");
			if(temp.length() < sAMAccountName.length()){
				throw new NamingException("Username contains some forbid speical characters. The special characters allowed to have in username are: ( ) . - _ ` ~ @ $ ^");
			}
			attributes.put("sAMAccountName", paramMaps.get("sAMAccountName")[0]);
			
	// all other attributes allowed to have these chars , < > . ; = * ( ) [ ] - _ ` ~ | @ $ ^ : ~ # + & % { } ?
	// and they don't have to be escaped here
			attributes.put("cn", fullname); 
			attributes.put("givenName", paramMaps.get("givenName")[0]);
			attributes.put("sn", paramMaps.get("sn")[0]);
			attributes.put("displayName", fullname);
			attributes.put("userPrincipalName",paramMaps.get("sAMAccountName")[0] + "@" +
							LdapProperty.getProperty(LdapConstants.LDAP_DOMAIN));
			attributes.put("description", paramMaps.get("description")[0]);
			attributes.put("department", paramMaps.get("department")[0]);
			attributes.put("company", paramMaps.get("company")[0]);
			if(paramMaps.get("info") != null && !paramMaps.get("info")[0].trim().equals(""))
				attributes.put("info", paramMaps.get("info")[0]);
			if(paramMaps.get("streetAddress") != null && !paramMaps.get("streetAddress")[0].trim().equals(""))
				attributes.put("streetAddress", paramMaps.get("streetAddress")[0]);
			if(paramMaps.get("l") != null && !paramMaps.get("l")[0].trim().equals(""))
				attributes.put("l", paramMaps.get("l")[0]);
			if(paramMaps.get("st") != null && !paramMaps.get("st")[0].trim().equals(""))
				attributes.put("st", paramMaps.get("st")[0]);
			if(paramMaps.get("postalCode") != null && !paramMaps.get("postalCode")[0].trim().equals(""))
				attributes.put("postalCode", paramMaps.get("postalCode")[0]);
			attributes.put("c", paramMaps.get("c")[0]);
			attributes.put("telephoneNumber", paramMaps.get("telephoneNumber")[0]);
			if(paramMaps.get("facsimileTelephoneNumber") != null && !paramMaps.get("facsimileTelephoneNumber")[0].trim().equals(""))
				attributes.put("facsimileTelephoneNumber", paramMaps.get("facsimileTelephoneNumber")[0]);
			if(paramMaps.get("mobile") != null && !paramMaps.get("mobile")[0].trim().equals(""))
				attributes.put("mobile", paramMaps.get("mobile")[0]);
			attributes.put("mail", paramMaps.get("mail")[0]);
			attributes.put("pwdLastSet", "0");
			//ADDITIONAL CODE
			/*//Get list of member organizations
			 * String[] organizations = paramMaps.get("orgs");
			//Iterate through list adding to attributes
			for (String org : organizations) {
				attributes.put("MemberOf", org);
			}*/
			//ADDITIONAL CODE ENDS

			// http://msdn.microsoft.com/en-us/library/aa772300.aspx
			int UF_PASSWD_NOTREQD = 0x0020;
			int UF_DONT_EXPIRE_PASSWD = 0x10000;
			int UF_NORMAL_ACCOUNT = 0x0200;
			attributes.put("userAccountControl",Integer.toString(UF_NORMAL_ACCOUNT +
												UF_PASSWD_NOTREQD + UF_DONT_EXPIRE_PASSWD));
			
			logger.debug("About to create User: " + escapedValueUserDN);

			// add userDN into LDAP
			// we need to escape reserved chars for the ldap name that we put in .createSubcontext() method.
			LdapName ldn = new LdapName(escapedValueUserDN);
			ctx.createSubcontext(ldn, attributes);
			
			//ADDITIONAL CODE
			//Get 'Groups' base DN
			String baseDN = LdapProperty.getProperty(LdapConstants.BASEGROUP_DN);
			if (baseDN==null)
				baseDN = "OU=Groups,DN=orion,DN=dmz";
			//Set DN for company organisational group
			String orgGroupDN = "CN="+companyName+","+baseDN;
			//Add user to company organistion group
			addUserToGroup(unescapedValueUserDN, orgGroupDN);
			//ADDITIONAL CODE ENDS
			logger.debug("Successfully created User: " + escapedValueUserDN);
			String password = paramMaps.get("password01")[0];
			if(!changePassword(unescapedValueUserDN, password)){
				deleteUser(unescapedValueUserDN);
				return false;
			}
			
			// add user into the default groups (default groups specified in ldap.properties config file)
			String defaultGroupID = LdapConstants.GROUP_DEFAULT;
			for(Enumeration<?> e = LdapProperty.propertyNames(); e.hasMoreElements(); ){
				String key = (String)e.nextElement();
				// looking for key: "group.default" and defaultGroupID: "group.default.1"
				if (key.length() >= defaultGroupID.length() && key.indexOf(defaultGroupID)==0){
					if(!addUserToGroup(unescapedValueUserDN, LdapProperty.getProperty(key))){
						deleteUser(unescapedValueUserDN);
						return false;
					}
				}
			}
			
			if(paramMaps.get("isLdapClient") != null){
				String groupDN = LdapProperty.getProperty(LdapConstants.GROUP_LDAP_CLIENT);
				if(!addUserToGroup(unescapedValueUserDN, groupDN)){
					deleteUser(unescapedValueUserDN);
					return false;
				}
			}
		
			return true;
		} catch(NamingException ex){
			//ADDED LINE: print stack trace, not just error string
			logger.error(ex);
			return false;
		}
	}
	
	
	// had escaped reserved chars
	/**
	 * Update LDAP server: add a user (who represented by userDN) to a group (represented by groupDN)
	 * This method can be used only when the given userDN exist in the LDAP. Generally, it should be
	 * generally, used after addUser() 
	 * both userDN and groupDN values must have not been escaped value at all. (they will be escaped in this method body)
	 * (e.g. userDN="CN=Mike+Jr,OU=Group, I,OU=Clients,DC=orion,DC=dmz")
	 * @param userDN representing user who needed to be assigned to the group (must have not been escaped value at all)
	 * @param groupDN representing group that needed to be assigned to the user (must have not been escaped value at all)
	 * @return true if the Adding process completed successfully
	 * @throws NamingException if an exception thrown during the process
	 */
	public boolean addUserToGroup(String userDN, String groupDN) throws NamingException{
		logger.debug("about to add user: " + userDN + " to group: " + groupDN);
		userDN = LdapTool.escapedCharsOnCompleteUserDN(userDN);
		groupDN = LdapTool.escapedCharsOnCompleteGroupDN(groupDN);
		try	{
			ModificationItem member[] = new ModificationItem[1];
			member[0]= new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute("member", userDN)); 
			ctx.modifyAttributes(new LdapName(groupDN),member);
			logger.debug("Added user "+userDN+" to group: " + groupDN);
			return true;
		}
		catch (NamingException e) {
			 logger.error("Problem adding user: "+userDN+" to group: " + groupDN, e);
			 throw new NamingException("Adding user to a group: " + ErrorConstants.FAIL_UPDATE_LDAP);
		}
	}
	
	
	
	// had escaped reserved chars
	/**
	 * Update LDAP server: add a group (who represented by groupDN1) into another group (represented by groupDN2)
	 * This method can be used only when the given groupDN1 and groupDN2 exist in the LDAP.
	 * both groupDN1 and groupDN2 values must have not been escaped value at all. (they will be escaped in this method body)
	 * e.g. given groupDN="cn=Associated, I,OU=Groups,DC=orion,DC=dmz"
	 * @param groupDN1 representing group which needed to be assigned to another group (must have not been escaped value at all)
	 * @param groupDN2 representing group that another group will be assigned into (must have not been escaped value at all)
	 * @return true if the Adding process completed successfully
	 * @throws NamingException if an exception thrown during the process
	 */
	public boolean addGroup1InToGroup2(String groupDN1, String groupDN2) throws NamingException{
		logger.debug("about to add group "+groupDN1+" to group: " + groupDN2);
		groupDN1 = LdapTool.escapedCharsOnCompleteGroupDN(groupDN1);
		groupDN2 = LdapTool.escapedCharsOnCompleteGroupDN(groupDN2);

		try	{
			ModificationItem member[] = new ModificationItem[1];
			member[0]= new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute("member", groupDN1)); 
			ctx.modifyAttributes(new LdapName(groupDN2),member);
			logger.debug("Added group "+groupDN1+" to group: " + groupDN2);
			return true;
		}
		catch (NamingException e) {
			 logger.error("Problem adding group: "+groupDN1+" to group: " + groupDN2, e);
			 throw new NamingException("Adding a group to another group: " + ErrorConstants.FAIL_UPDATE_LDAP);
		}
	}
	
	
	// had escaped reserved chars
	/**
	 * remove the given userDN from given groupDN  
	 * both userDN and groupDN values must have not been escaped value at all. (they will be escaped in this method body)
	 * (e.g. userDN="CN=Mike+Jr,OU=Group, I,OU=Clients,DC=orion,DC=dmz")
	 * @param userDN - ldap's user DN (it's not just a name, it is a DN of that user)
	 * @param groupDN - ldap's group DN (it's not just a name, it is a DN of that group)
	 * @return true if the removing successfully
	 * @throws NamingException if it cannot remove this userDN from the groupDN
	 */
	public boolean removeUserFromAGroup(String userDN, String groupDN) throws NamingException{
		logger.debug("about to remove user "+userDN+" from group: " + groupDN);
		
		userDN = LdapTool.escapedCharsOnCompleteUserDN(userDN);
		groupDN = LdapTool.escapedCharsOnCompleteGroupDN(groupDN);
		try	{
			// create a remove attribute (groupDN is an attribute of userDN)
			ModificationItem member[] = new ModificationItem[1];
			member[0]= new ModificationItem(DirContext.REMOVE_ATTRIBUTE, new BasicAttribute("member", userDN)); 
			// apply the remove attribute
			ctx.modifyAttributes(new LdapName(groupDN),member);
			logger.debug("Removed user "+userDN+" from group: " + groupDN);
			return true;
		}
		catch (NamingException e) {
			 logger.error("Problem in removing user: "+userDN+" from group: " + groupDN, e);
			 throw new NamingException("Removing user from a group: " + ErrorConstants.FAIL_UPDATE_LDAP);
		}
	}

	
	// had escaped reserved chars
	/**
	 * remove the given groupDN1 from given groupDN2 both groupDN1 and groupDN2 values
	 * must have not been escaped value at all. (they will be escaped in this method body) (e.g.
	 * e.g. given groupDN="cn=Associated, I,OU=Groups,DC=orion,DC=dmz"
	 * 
	 * @param groupDN1 - ldap's group DN (it's not just a name, it is a DN of that group)
	 * @param groupDN2 - ldap's group DN (it's not just a name, it is a DN of that group)
	 * @return true if the removing successfully
	 * @throws NamingException if it cannot remove this userDN from the groupDN
	 */
	public boolean removeGroup1FromGroup2(String groupDN1, String groupDN2)
			throws NamingException {
		logger.debug("about to remove group " + groupDN1 + " from group: " + groupDN2);
		
		groupDN1 = LdapTool.escapedCharsOnCompleteGroupDN(groupDN1);
		groupDN2 = LdapTool.escapedCharsOnCompleteGroupDN(groupDN2);
		try {
			// create a remove attribute (groupDN is an attribute of userDN)
			ModificationItem member[] = new ModificationItem[1];
			member[0] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE,
					new BasicAttribute("member", groupDN1));
			// apply the remove attribute
			ctx.modifyAttributes(new LdapName(groupDN2), member);
			logger.debug("removed group " + groupDN1 + " from group: " + groupDN2);
			return true;
		} catch (NamingException e) {
			logger.error("Problem in removing group: " + groupDN1 + " from group: " + groupDN2, e);
			throw new NamingException("Removing a group from another group: " + ErrorConstants.FAIL_UPDATE_LDAP);
		}
	}
	
	
	// had escaped reserved chars, not yet tested
	/**
	 * Change the unicode password of give userDN with the given password
	 * 
	 * Note: The method can be run successfully only on the SSL or TSL connection.
	 * If you are running on the normal connection, it will throw the "UNWILLING TO PERFORM" error message.
	 * 
	 * @param userDN: Ldap user whose password need to be changed 
	 * (userDN must hasnot been escaped any reserved chars)
	 * (e.g. userDN="CN=Mike+Jr,OU=Group, I,OU=Clients,DC=orion,DC=dmz")
	 * @param password: new password
	 * @return true if the modification is successful. false otherwise.
	 */
	public boolean changePassword(String userDN, String password){
		logger.debug("about to update password for user: " + userDN + " with new psw: " + password);
		
		userDN = LdapTool.escapedCharsOnCompleteUserDN(userDN);
		try{
			String quotedPwd = "\""+password+"\"";
			byte encodedPwd[] = quotedPwd.getBytes( "UTF-16LE" );
			ModificationItem[] mods = new ModificationItem[2];
			mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("pwdLastSet", "-1"));
			mods[1] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("unicodePwd", encodedPwd));
			// this method must be performed on SSL or TSL connection
			ctx.modifyAttributes(new LdapName(userDN), mods);
			logger.debug("Updated password for user: "+userDN);
			return true;
		}catch(NamingException ex){
			logger.error(String.format("Exception while modifying user's password, (userDN, psw) = (%s, %s)", userDN, password), ex);
		}catch(UnsupportedEncodingException ex){
			logger.error("Exception while encoding the given password: " + userDN,ex);
		}
		return false;
	}
	
	
	// had escaped reserved chars, not yet tested
	/**
	 * delete the user (defined by given userDN)
	 * userDN must has not been escaped value at all. (they will be escaped in this method body)
	 * (e.g. userDN="CN=Mike+Jr,OU=Group, I,OU=Clients,DC=orion,DC=dmz")
	 * @param userDN: given userDN that has not been escaped value at all. (they will be escaped in this method body) (e.g. userDN="CN=Mike+Jr,OU=Group, I,OU=Clients,DC=orion,DC=dmz")
	 * @return true, if the deletion successful, false otherwise.
	 */
	public boolean deleteUser(String userDN){
		logger.debug("about to delete user: " + userDN);
		
		userDN = LdapTool.escapedCharsOnCompleteUserDN(userDN);
		try{
			ctx.destroySubcontext(new LdapName(userDN));
			logger.debug("Deleted user: " + userDN + "successfully");
			return true;
		}catch(NamingException ex){
			logger.error("Exception while deleting a give userDN: " + userDN, ex);
			return false;
		}
	}
	
	
	
	
	/**
	 * return a sorted set of all organisations that are stored in Ldap's Clients
	 * @return
	 */
	public SortedSet<String> getUserGroups(){
		logger.debug("about to search for all the organisations that are stored in Clients folder");
		
		String baseDN = LdapProperty.getProperty(LdapConstants.GROUP_DN);
		String groupAttr = LdapProperty.getProperty(LdapConstants.GROUP_ATTR);
		String filter = "("+groupAttr+"=*)";
		SortedSet<String> output = new TreeSet<String>();
		try{
			logger.debug("LdapTool is about searching for groups of: " + baseDN);
			NamingEnumeration<SearchResult> e = ctx.search(baseDN, filter, null);
			while(e.hasMore()){
				SearchResult results = (SearchResult)e.next();
				Attributes attributes = results.getAttributes();
				output.add((String)attributes.get(groupAttr).get());
			}
			logger.debug("searching is completed successfully.");
		}catch(NamingException ex){
			logger.error(ex.toString());
			ex.printStackTrace();
		}catch(NullPointerException ex){
			logger.error(ex.toString());
			ex.printStackTrace();
		}
		return output; 
	}
	
	
	
	
	//ADDITIONAL FUNCTION
	/**
	 * Get a list of all groups (not organizations in Clients) stored in Groups folder + groups that stored in Orion Health folder
	 * The group name in this list doesn't contains any escaped chars. So, please escape the reserve chars if you need to use these names agains the LDAP server.
	 * @return
	 */
	public Set<String> getBaseGroups(){
		logger.debug("about to search for all groups that are stored in Orion Health group");
		Set<String> output = new LinkedHashSet<String>();
		try {
			// query all the groups that are stored in "Orion Health"
			// organisation
			SortedSet<String> orionHealthGroups = new TreeSet<String>();
			String orionHealthBasedDN = LdapProperty.getProperty("orionhealthOrganisationBasedDN");
			if (orionHealthBasedDN == null) {
				orionHealthBasedDN = "OU=Orion Health,OU=Clients,DC=orion,DC=dmz";
			}
			String orionHealthGroupsFilter = LdapProperty.getProperty("orionhealthOrganisationBasedAttribute");
			if (orionHealthGroupsFilter == null) {
				orionHealthGroupsFilter = "(CN=*)";
			} else {
				// if it is not null, it should be "orionHealthGroupsFilter = CN" so need to add bracelet
				orionHealthGroupsFilter = "(" + orionHealthGroupsFilter + "=*)";
			}
			NamingEnumeration<SearchResult> e = ctx.search(orionHealthBasedDN, orionHealthGroupsFilter, null);
			while (e.hasMore()) { // the result from the search contains both
									// Users and Groups
				SearchResult results = (SearchResult) e.next();
				Attributes attributes = results.getAttributes();
				if (attributes.get("grouptype") != null) { // add only groups
															// into output
															// object
					orionHealthGroups.add((String) attributes.get("cn").get());
				}
			}
			logger.debug("Finished searching for groups that are stored in Orion Health groups");

			logger.debug("about to search for all groups that are stored in Clients folder");
			// query all the groups that are stored in "Groups" folder
			SortedSet<String> generalGroups = new TreeSet<String>();
			String baseDN = LdapProperty.getProperty(LdapConstants.BASEGROUP_DN);
			if (baseDN == null)
				baseDN = "OU=Groups,DC=orion,DC=dmz";
			String groupAttr = LdapProperty.getProperty(LdapConstants.BASEGROUP_ATTR);
			if (groupAttr == null)
				groupAttr = "cn";
			String filter = "("+groupAttr+"=*)";
			e = ctx.search(baseDN, filter, null);
			while(e.hasMore()){
				SearchResult results = (SearchResult)e.next();
				Attributes attributes = results.getAttributes();
				generalGroups.add((String)attributes.get(groupAttr).get());
			}
			
			
			output.addAll(orionHealthGroups);
			output.addAll(generalGroups);
			logger.debug("finished searching for all groups that are stored in Clients folder");

		}catch(NamingException ex){
			logger.error(ex.toString());
			ex.printStackTrace();
		}catch(NullPointerException ex){
			logger.error(ex.toString());
			ex.printStackTrace();
		}
		return output; 
	}
	
	
	// had escaped reserved chars
	/**
	 * return a map of Group Users from Ldap. The key of the map is the display name of that group (it is not Distinguished name).
	 * The value is an array, the first element (at index 0) is the distinguished name of this group and second element (at index 1) is the word "enabled"
	 * @param name: is just a simple name (it is not a Distinguished name). The method will build a DN name. Please don't provide a DN name and Please don't escape any character in the name (just leave it as it is). (e.g. correct argument is: "Health, Bot"  not "Health\, Bot"
	 * @return: all the dn in the Map were unescaped the reserved ldap chars (because it is the original value returning from Ldap) (e.g. dn="CN=Mike+Jr,OU=Group, I,OU=Clients,DC=orion,DC=dmz"
	 */
	public TreeMap<String,String[]> getGroupUsers(String name){
		logger.debug("about to search for the DN of the group: " + name);
		
		// escape all the reserve key words.
		name = Rdn.escapeValue(name);
		TreeMap<String,String[]> users = new TreeMap<String,String[]>();
		String baseDN = "OU="+name+","+LdapProperty.getProperty(LdapConstants.GROUP_DN);
		String userAttr = LdapProperty.getProperty(LdapConstants.USER_ATTR);
		String filter = "("+userAttr+"=*)";
		String dn = "";
		try{
			NamingEnumeration<SearchResult> e = ctx.search(new LdapName(baseDN), filter, null);
			while(e.hasMore()){
				try{
					SearchResult results = (SearchResult)e.next();
					Attributes attributes = results.getAttributes();
					String cn = attributes.get("cn").get().toString(); // displayName
					dn = attributes.get("distinguishedName").get().toString();
					dn = (String) Rdn.unescapeValue(dn);
					int userAccountControl = Integer.parseInt(attributes.get("userAccountControl").get().toString());
					String disabled = "enabled";
					if( (userAccountControl & 2) > 0 ){
						disabled = "disabled";
					}
					users.put(cn, new String[]{dn, "enabled"});
					
					logger.debug("search for the DN of the group " + name + " finished");
				} catch (NullPointerException ne){
					logger.error("Exception while querying dn: "+ dn + " from " + baseDN, ne);
				}
			}
		}catch(NamingException ex){
			logger.error("Exception while querying for: " + baseDN + " from Ldap server.", ex);
		}
		return users;
	}
	
	
	
	// had escaped reserved chars
	/**
	 * return all the attribute of the given ldap organisation name
	 * @param name: is just a simple name (it is not a Distinguished name). The method will build a DN name. Please don't provide a DN name and Please don't escape any character in the name (just leave it as it is) (e.g. correct argument is: "Health, Bot"  not "Health\, Bot").
	 * @return
	 */
	public Attributes getOrganisationAttributes(String name){
		logger.debug("about to search for the attributes of the organisation: " + name);
		// escape all the reserve key words.
		name = Rdn.escapeValue(name);
		
		try{
			String baseDN = "OU="+name+","+LdapProperty.getProperty(LdapConstants.GROUP_DN);
			Attributes attrs = ctx.getAttributes(new LdapName(baseDN));
			
			logger.debug("finished searching for the attributes of the organisation: " + name);
			
			return attrs;
		}catch(NamingException ex){
			logger.error(ex.toString());
			ex.printStackTrace();
		}
		return null;
	}
	
	
	// had escaped reserved chars
	/**
	 * get attributes of a Ldap user who has userDN.
	 * @note the return Attributes contains the "memberOf" Attribute. Each element of "memberOf"
	 * is a DN with the escaped chars. If you using "memberOf" attribute. you need to deal with
	 * that escaped chars. 
	 * @param userDN: of the Ldap user (userDN must not has been escaped the reserved chars) (e.g. dn="CN=Mike+Jr,OU=Group, I,OU=Clients,DC=orion,DC=dmz")
	 * @return an Attributes object that stores all the current attributes belong to this userDN.
	 *         null otherwise.
	 */
	public Attributes getUserAttributes(String userDN){
		logger.debug("about to search for the attributes of the user: " + userDN);
		
		userDN = LdapTool.escapedCharsOnCompleteUserDN(userDN);
		try{
			LdapName ldapUserDN = new LdapName(userDN);
			Attributes attrs = ctx.getAttributes(ldapUserDN);
			
			logger.debug("fnished searching for the attributes of the user: " + userDN);
			return attrs;
		}catch(NamingException ex){
			logger.error("Exception while querying all attribtues of a user: " + userDN, ex);
		}
		return null;
	}
	
	
	
	// had escaped reserved chars
	/**
	 * return all the attribute of the given ldap group name
	 * @param companyName: is just a simple name (it is not a Distinguished name). The method will build a DN name. Please don't provide a DN name and Please don't escape any character in the name (just leave it as it is). (e.g. correct argument is: "Health, Bot"  not "Health\, Bot").
	 * @return
	 */
	public Attributes getGroupAttributes(String companyName){
		logger.debug("about to search for the attributes of the group: " + companyName);
		
		String baseDN = LdapProperty.getProperty(LdapConstants.BASEGROUP_DN);
		if (baseDN==null)
			baseDN = "OU=Groups,DN=orion,DN=dmz";
			String companyDN = "CN="+Rdn.escapeValue(companyName)+","+baseDN;
			
		try{
			Attributes attrs = ctx.getAttributes(new LdapName(companyDN));
			logger.debug("fnished searching for the attributes of the group: " + companyName);
			return attrs;
		}catch(NamingException ex){
			logger.error(ex.toString());
			ex.printStackTrace();
		}
		return null;
	}
	
	
	// had escaped reserved chars, not yet tested
	/**
	 * get the company name that the userDN is working for (from the LDAP server)
	 * @param userDN represent the user/company. userDN must be a full dn-name and it must have not been escaped the reserved chars.
	 * (e.g of the correct userDN that should give to this method: CN=Lisa, She/pherd,OU=Hospira Pty limited *Project*,OU=Clients,DC=orion,DC=dmz)
	 * @return company simple name in String (not the DN name)
	 */
	public String getUserCompany(String userDN){
		logger.debug("about to search for the company name of user: " + userDN);
		userDN = LdapTool.escapedCharsOnCompleteUserDN(userDN);
		try{
			Attributes attrs = ctx.getAttributes(new LdapName(userDN));
			if(attrs.get("company") == null ){
				String company = LdapTool.getOUvalueFromDNThasHasTwoOU(userDN);
				logger.debug("finished searching for the company name of user: " + userDN);
				
				return company;
			}else{
				logger.debug("finished searching for the company name of user: " + userDN);
				
				return attrs.get("company").get().toString();
			}
		}catch(NamingException ex){
			logger.error("Trying to get attribute " + userDN + "\t", ex);
			ex.printStackTrace();
		}
		return null;
	}
	
	
	// had escaped reserved chars
	/**
	 * check if the given companyName exists in the "Clients" in Ldap server
	 * @param companyName is just a based name, its not a company DN name. companyName must has not be escaped any reserved chars
	 * @return true if the given companyName is in Ldap Server, false otherwise
	 */
	public boolean companyExists(String companyName){
		logger.debug("about to search for the company: " + companyName);
		String baseDN = LdapProperty.getProperty(LdapConstants.GROUP_DN);
		String filter = "("+LdapProperty.getProperty(LdapConstants.GROUP_ATTR)+"="+Rdn.escapeValue(companyName)+")";
		NamingEnumeration<SearchResult> e;
		try {
			e = ctx.search(new LdapName(baseDN), filter, null);
			logger.debug("finished searching for the company: " + companyName);
			if(e.hasMore()){
				return true;
			}
		} catch (NamingException ex) {
		}
		return false;
	}
	
	
	// had escaped reserved chars
	/**
	 * Simple function to check if a given company exists as a group
	 * @param companyName is just a based name, its not a company DN name. companyName must has not be escaped any reserved chars
	 * @return true if more than zero search matches. false otherwise
	 */
	public boolean companyExistsAsGroup(String companyName){
		logger.debug("about to search for the company group: " + companyName);
		
		companyName = Rdn.escapeValue(companyName);
		//Get DN for 'Groups'
		String baseDN = LdapProperty.getProperty(LdapConstants.BASEGROUP_DN);
		//Create search string (CN=<companyName>)
		String filter = "(CN="+companyName+")";
		NamingEnumeration<SearchResult> e;
		try {
			//Run search. If more than zero matches, return true
			e = ctx.search(new LdapName(baseDN), filter, null);
			logger.debug("finished searching for the company group: " + companyName);
			if(e.hasMore()){
				return true;
			}
		} catch (NamingException ex) {
			//If error, log detail and stack trace
			logger.error(ex.getMessage());
			ex.printStackTrace();
		}
		//Otherwise return false
		return false;
	}
	

	// had escaped reserved chars
	/**
	 * Simple function to check if a given username exists
	 * @param username - relevant username (sAMAccountName)  (username must havenot been escaped any reserved chars)
	 * @param company - user's company	(company must havenot been escaped any reserved chars)
	 * @return true if more than zero search matches.
	 */
	public boolean usernameExists(String username, String company){
		logger.debug("about to search for user: " + username + " from company: " + company);
		username = Rdn.escapeValue(username);
		company = Rdn.escapeValue(company);
		
		//Search user's company
		String baseDN = "OU="+company+","+LdapProperty.getProperty(LdapConstants.GROUP_DN);
		//Create search string (sAMAccountName=<username>)
		String filter = "("+LdapProperty.getProperty(LdapConstants.USER_ATTR)+"="+username+")";
		NamingEnumeration<SearchResult> e;
		try {
			//Run search. If more than zero matches, return true
			e = ctx.search(new LdapName(baseDN), filter, null);
			logger.debug("finished searching for user: " + username + " from company: " + company);
			if(e.hasMore()){
				return true;
			}
		} catch (NamingException ex) {
			//If error, log detail and stack trace
			logger.error(ex.getMessage());
			ex.printStackTrace();
		}
		//Otherwise return false
		return false;
	}
	
	
	// had escaped reserved chars
	/**
	 * Simple function to check if a given email exists.
	 * 
	 * @param email - user's email
	 * @param companyName is just a based name, its not a company DN name. companyName must has not be escaped any reserved chars
	 * @return true if more than zero search matches.
	 */
	public boolean emailExists(String email, String company){
		logger.debug("about to search for email: " + email + " from company: " + company);
		
		company = Rdn.escapeValue(company);
		//Search user's company
		String baseDN = "OU="+company+","+LdapProperty.getProperty(LdapConstants.GROUP_DN);
		//Create search string (mail=<email>)
		String filter = "(mail="+email+")";
		NamingEnumeration<SearchResult> e;
		try {
			//Run search. If more than zero matches, return true
			e = ctx.search(new LdapName(baseDN), filter, null);
			
			logger.debug("finished searching for email: " + email + " from company: " + company);
			
			if(e.hasMore()){
				return true;
			}
		} catch (NamingException ex) {
			//If error, log detail and stack trace
			logger.error("Exception while searching for " + baseDN, ex);
		}
		//Otherwise return false
		return false;
	}
	
	
	
	/**
	 * Simple function to check if a given userDN exists.
	 * @param fullname - user's display name
	 * @param company - user's company
	 * @return true if more than zero search matches (means userDN exists).
	 */
	public boolean userDNExists(String fullname, String company){
		logger.debug("about to search for user: " + fullname + " from company: " + company);
		
		//Search user's company
		String baseDN = "OU="+company+","+LdapProperty.getProperty(LdapConstants.GROUP_DN);
		//Create search string (CN=Display Name)
		String filter = "CN="+fullname;
		NamingEnumeration<SearchResult> e;
		try {
			//Run search. If more than zero matches, return true
			e = ctx.search(new LdapName(baseDN), filter, null);
			
			logger.debug("finished searching for user: " + fullname + " from company: " + company);
			
			if(e.hasMore()){
				return true;
			}
		} catch (NamingException ex) {
			//If error, log detail and stack trace
			logger.error("Exception while searching for " + baseDN, ex);
			ex.printStackTrace();
		}
		//Otherwise return false
		return false;
	}
	
	
	// had escaped reserved chars
	/**
	 * Get email address, given the corresponding username. 
	 * 
	 * @param username - the user's sAMAccountName (it must has not been escaped any reserved chars)
	 * @param company - user's company (it must has not been escaped any reserved chars)
	 * @return the first matching email, if there is a match, else returns null
	 */
	public String getEmail(String username, String company){
		logger.debug("about to search for email of user: " + username + " from company: " + company);
		
		company = Rdn.escapeValue(company);
		username = Rdn.escapeValue(username);
		//Search user's company
		String baseDN = "OU="+company+","+LdapProperty.getProperty(LdapConstants.GROUP_DN);
		//Create search string (sAMAccountName=<username>)
		String filter = "("+LdapProperty.getProperty(LdapConstants.USER_ATTR)+"="+username+")";
		NamingEnumeration<SearchResult> e;
		try {
			//Run search. If more than zero matches, return true
			e = ctx.search(new LdapName(baseDN), filter, null);
			if(e.hasMore()){
				SearchResult ne = e.next();
				String mail = (String) ne.getAttributes().get("mail").get();
				logger.debug("finished searching for email of user: " + username + " from company: " + company);
				return mail;
			}
		} catch (NamingException ex) {
			//If error, log detail and stack trace
			logger.error(ex.getMessage());
			ex.printStackTrace();
		}
		//Otherwise return false
		return null;
	}

	
	// had escaped reserved chars
	/**
	 * Get full name, given the corresponding username. 
	 * 
	 * @param username - the user's sAMAccountName
	 * @param company - user's company
	 * @return the first matching name, if there is a match, else returns null.
	 */
	public String getName(String username, String company){
		logger.debug("about to search for fullname of user: " + username + " from : " + company);
		username = Rdn.escapeValue(username);
		company = Rdn.escapeValue(company);
		//Search user's company
		String baseDN = "OU="+company+","+LdapProperty.getProperty(LdapConstants.GROUP_DN);
		//Create search string (sAMAccountName=<username>)
		String filter = "("+LdapProperty.getProperty(LdapConstants.USER_ATTR)+"="+username+")";
		NamingEnumeration<SearchResult> e;
		try {
			//Run search. If more than zero matches, return the CN
			e = ctx.search(new LdapName(baseDN), filter, null);
			if(e.hasMore()){
				SearchResult ne = e.next();
				String cn = (String) ne.getAttributes().get("cn").get();
				logger.debug("about to search for fullname of user: " + username + " from : " + company);
				return cn;
			}
		} catch (NamingException ex) {
			//If error, log detail and stack trace
			logger.error(ex.getMessage());
			ex.printStackTrace();
		}
		//Otherwise return false
		return null;
	}
	//ADDITIONAL FUNCTION
	//Deprecated function
	/*public boolean userDNExists(Map<String, String[]> paramMaps) {
		String fullname = "";
		if(paramMaps.get("displayName")[0] != null){
			fullname = paramMaps.get("displayName")[0];
		}else{
			fullname = paramMaps.get("givenName")[0] + " " + paramMaps.get("sn")[0];
		}
		String groupBaseDN = LdapProperty.getProperty(LdapConstants.GROUP_DN);
		String companyName = paramMaps.get("company")[0];

		String companyDN = LdapProperty.getProperty(LdapConstants.GROUP_ATTR)+"="+companyName+","+groupBaseDN;
		String userDN = "CN="+fullname+","+companyDN;
		if (getUsername(userDN).equals("")) {
			return false;
		}
		return true;
	}*/
	

	// had escaped reserved chars
	/**
	 * Add the given companyName into the Ldap server as a Client (Ldap user), but not into Groups
	 * I mean add compnayName into OU=Clients,DC=orion,DC=dmz
	 * @param companyName that need to be added (must has not been escaped any reserved chars)
	 * @return true if the companyName can be added successfully
	 *         false otherwise
	 * @throws NamingException 
	 */
	public boolean addCompany(String companyName) throws NamingException{
		// e.g. if companyName is "AMICAS, Inc (Now Merge)"
		
		logger.debug("about to add company: " + companyName + "into Clients folder (of Ldap Server)");
		
		// get baseDN for the user (it should be: OU=Clients,DC=orion,DC=dmz)
		String baseDN = LdapProperty.getProperty(LdapConstants.GROUP_DN);
		
		Attributes attributes = new BasicAttributes(true);
		attributes.put("objectClass", "top");
		attributes.put("objectClass", "organizationalUnit");
		// attribute don't need any escape char for the reserver char
		// attribute "ou" => "AMICAS, Inc (Now Merge)"  and  "name" => "AMICAS, Inc (Now Merge)"
		attributes.put("ou", companyName);
		attributes.put("distinguishedName", baseDN);
		attributes.put("name", companyName);
		
		// all DN need escape char for any reserve char
		// companyDN = "ou=AMICAS\, Inc (Now Merge),OU=Clients,DC=orion,DC=dmz"
		String companyDN = LdapProperty.getProperty(LdapConstants.GROUP_ATTR)+"="+Rdn.escapeValue(companyName)+","+baseDN;
		Name ldapCompanyDN = null;
		try {
			ldapCompanyDN = new LdapName(companyDN);
		} catch (InvalidNameException e1) {
			String failureReason = "Can not create LdapName for company: " + companyDN + ". It can be caused by company name contains reserved chars.";
			logger.error(failureReason, e1);
			throw new InvalidNameException(failureReason);
		}
		
		try {
			if (ctx == null)
				logger.debug("ctx uninitialised");
			ctx.createSubcontext(ldapCompanyDN, attributes);
			logger.debug("Company with DN: "+companyDN+" was added successfully");
			return true;
		} catch (NamingException e) {
			logger.error(String.format("Exception while adding %s into Ldap server as a user.", companyName), e);
			if(e instanceof NameAlreadyBoundException){
				throw new NameAlreadyBoundException("This group already exist in Clients");
			}
			
			return false;
		}
	}
	
	
	// had escaped reserved chars
	//ADDITIONAL FUNCTION
	/**
	 * Add Company to 'Groups'. the given companyName must exist in the LDAP server.
	 * Normally, we use this method to add a new created company into some groups.
	 * Which means that (normally) we use addCompany() to create a company into LDAP.
	 * Then we use this method to add that company into some groups.
	 * @param companyName that need to be added as a Ldap Group
	 * @return true if the Organisation was added successfully.
	 * 		   false otherwise.
	 * @throws NamingException 
	 */
	public boolean addCompanyAsGroup(String companyName) throws NamingException{
		// e.g. if companyName is "AMICAS, Inc (Now Merge)"
		
		logger.debug("about to add company: " + companyName + "into Groups folder (of Ldap Server)");
				
		//Get base DN for 'Groups'
		String baseDN = LdapProperty.getProperty(LdapConstants.BASEGROUP_DN);
		if (baseDN==null)
			baseDN = "OU=Groups,DN=orion,DN=dmz";
		
		//Set basic attributes
		Attributes attributes = new BasicAttributes(true);
		attributes.put("objectClass", "top");
		attributes.put("objectClass", "group");
		// attribute don't need any escape char for the reserver char
		// attribute "ou" => "AMICAS, Inc (Now Merge)"  and  "name" => "AMICAS, Inc (Now Merge)"
		attributes.put("cn", companyName);
		attributes.put("distinguishedName", baseDN);
		attributes.put("name", companyName);
		
		// all DN need escape char for any reserve char
		// companyDN = "ou=AMICAS\, Inc (Now Merge),OU=Clients,DC=orion,DC=dmz"
		String companyDN = "CN="+Rdn.escapeValue(companyName)+","+baseDN;
		Name ldapCompanyDN = null;
		try {
			ldapCompanyDN = new LdapName(companyDN);
		} catch (InvalidNameException e1) {
			String failureReason = "Can not create LdapName for company: " + companyDN + ". It can be caused by company name contains reserved chars.";
			logger.error(failureReason, e1);
			throw new InvalidNameException(failureReason);
		}
				
		try {
			if (ctx == null)
				logger.debug("ctx uninitialised");
			//Create company group and log success
			ctx.createSubcontext(ldapCompanyDN, attributes);
			logger.debug("Organisation with DN: "+companyDN+" was added as group successfully");
			return true;
		//If error, log detail and stack trace	
		} catch (NamingException e) {
			logger.error(String.format("Failed to add organisation: %s, as a Ldap Group", companyName), e);
			if(e instanceof NameAlreadyBoundException){
				throw new NameAlreadyBoundException("This group already exist in Clients");
			}
			return false;
		}
	}
	
	
	/**
	 * delete the company from "Groups" folder (defined by given groupDN)
	 * groupDN must has not been escaped value at all. (they will be escaped in this method body)
	 * (e.g. groupDN="CN=company_name,OU=Group, I,OU=Clients,DC=orion,DC=dmz")
	 * @param groupDN: given groupDN that has not been escaped value at all. (they will be escaped in this method body) 
	 * @return true, if the deletion successful, false otherwise.
	 */
	public boolean deleteGroupCompany(String groupDN){
		logger.debug("about to delete group: " + groupDN);
		groupDN = LdapTool.escapedCharsOnCompleteGroupDN(groupDN);
		try{
			ctx.destroySubcontext(new LdapName(groupDN));
			logger.debug("finished deleting groupDN");
			return true;
		}catch(NamingException ex){
			logger.error("Exception while deleting a give userDN: " + groupDN, ex);
			return false;
		}
	}
	
	
	/**
	 * delete the company from "Clients" folder (defined by given groupDN)
	 * groupDN must has not been escaped value at all. (they will be escaped in this method body)
	 * (e.g. groupDN="ou=company_name,OU=Clients,DC=orion,DC=dmz")
	 * @param groupDN: given groupDN that has not been escaped value at all. (they will be escaped in this method body) 
	 * @return true, if the deletion successful, false otherwise.
	 */
	public boolean deleteCompany(String groupDN){
		logger.debug("about to delete Client: " + groupDN);
		groupDN = LdapTool.escapedCharsOnCompleteCompanyDN(groupDN);
		try{
			ctx.destroySubcontext(new LdapName(groupDN));
			logger.debug("finished deleting Client: " + groupDN);
			return true;
		}catch(NamingException ex){
			logger.error("Exception while deleting a give userDN: " + groupDN, ex);
			return false;
		}
	}
	


	/**
	 * Produce groupDN (without escapping reserved chars) from group name
	 * @param groupName - groupName must not have been escaped the reserved chars.
	 * 		groupName is just the name of the group, it is not DN. (i.e. groupname is XX, it is not CN=XX,OU=Groups,DC=orion,DC=dmz)
	 * @return group DN
	 */
	public String getDNFromGroup(String groupName) {

		logger.debug("about to search for the DN of the group: " + groupName);
		// generally, the dn is the combination of CN=groupName and basedDN
		//normally, basedDN is OU=Groups,DC=orion,DC=dmz
		//so, dn should be CN=groupName,OU=Groups,DC=orion,DC=dmz
		// in a special case that groupName is a group from Orion Health folder
		// its basedDN is OU=Orion Health,OU=Clients,DC=orion,DC=dmz

		
		// 1). so, in order to distinguish between a special case and a normal case
		//     we search for this group from the OU=Orion Health,OU=Clients,DC=orion,DC=dmz
		//     if it is found, means it is a special case.
		//     so, return this special case dn
		try{
			// search for groupName from "Orion Health" organisation
			String orionHealthBasedDN = LdapProperty.getProperty("orionhealthOrganisationBasedDN");
			if(orionHealthBasedDN == null){
				orionHealthBasedDN = "OU=Orion Health,OU=Clients,DC=orion,DC=dmz";
			}
			String orionHealthGroupsFilter = LdapProperty.getProperty("orionhealthOrganisationBasedAttribute");
			if(orionHealthGroupsFilter == null){
				orionHealthGroupsFilter = "(CN=" + Rdn.escapeValue(groupName) + ")";
			} else {
				// if it is not null, it should be "orionHealthGroupsFilter = CN"
				// so need to add bracelet
				orionHealthGroupsFilter = "(" + orionHealthGroupsFilter + "=" +groupName+ ")";
			}
			
			NamingEnumeration e = ctx.search(orionHealthBasedDN, orionHealthGroupsFilter, null);
			// if it found, e must have only one index
			while(e.hasMore()){ 
				SearchResult results = (SearchResult)e.next();
				String dn = results.getNameInNamespace();
				return dn; // return the dn we found
			}
		}catch(NamingException ex){
			logger.error(ex.toString());
			ex.printStackTrace();
		}catch(NullPointerException ex){
			logger.error(ex.toString());
			ex.printStackTrace();
		}
		
		// 2). if we can't find groupName in the OU=Orion Health,OU=Clients,DC=orion,DC=dmz
		//     then it means the groupName is a general case (combination between CN=groupName and basedDN)
		String baseDN = LdapProperty.getProperty(LdapConstants.BASEGROUP_DN);
		String attrName = LdapProperty.getProperty(LdapConstants.BASEGROUP_ATTR);
		String dn = attrName+"="+groupName+","+baseDN;
		logger.debug("finished searching for the DN of the group: " + groupName);
		return dn;
	}
	

	
	/**
	 * Produce organisationDN (without escapping reserved charsA) from organisation name
	 * @param orgName - organisation name
	 * @return organisation DN
	 */
	public String getDNFromOrg(String orgName) {
		String baseDN = LdapProperty.getProperty(LdapConstants.GROUP_DN);
		String attrName = LdapProperty.getProperty(LdapConstants.GROUP_ATTR);
		String dn = attrName+"="+orgName+","+baseDN;
		return dn;
	}

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	public boolean isAccountDisabled(String userDN){
		logger.debug("about to search whether this user is disabled or enabled: " + userDN);
		userDN = LdapTool.escapedCharsOnCompleteUserDN(userDN);
		try {
			Attributes attrs = ctx.getAttributes(new LdapName(userDN));
			int userAccountControl = Integer.parseInt(attrs.get("userAccountControl").get().toString());
			logger.debug("finished searching whether this user is disabled or enabled: " + userDN);
			return (userAccountControl & 2) > 0;
		} catch (NamingException e) {
			logger.error(e.toString());
			e.printStackTrace();
		}
		return false;
	}
	
	public boolean disableUser(String userDN){
		logger.debug("about to disable user: " + userDN);
		userDN = LdapTool.escapedCharsOnCompleteUserDN(userDN);
		int UF_PASSWD_NOTREQD = 0x0020;
		int UF_DONT_EXPIRE_PASSWD = 0x10000;
		int UF_NORMAL_ACCOUNT = 0x0200;
		int UF_ACCOUNTDISABLE = 0x2;
		Attributes attributes = new BasicAttributes(true);
		attributes.put("userAccountControl",Integer.toString(UF_NORMAL_ACCOUNT +
				UF_ACCOUNTDISABLE + UF_PASSWD_NOTREQD + UF_DONT_EXPIRE_PASSWD));
		try {
			ctx.modifyAttributes(new LdapName(userDN), DirContext.REPLACE_ATTRIBUTE, attributes);
			logger.debug("Disabled user: "+userDN);
			return true;
		} catch (NamingException e) {
			logger.error(e.toString());
			e.printStackTrace();
		}
		return false;
	}
	public boolean enableUser(String userDN){
		logger.debug("about to enable user: " + userDN);
		userDN = LdapTool.escapedCharsOnCompleteUserDN(userDN);
		int UF_PASSWD_NOTREQD = 0x0020;
		int UF_DONT_EXPIRE_PASSWD = 0x10000;
		int UF_NORMAL_ACCOUNT = 0x0200;
		Attributes attributes = new BasicAttributes(true);
		attributes.put("userAccountControl",Integer.toString(UF_NORMAL_ACCOUNT +
				UF_PASSWD_NOTREQD + UF_DONT_EXPIRE_PASSWD));
		try {
			ctx.modifyAttributes(new LdapName(userDN), DirContext.REPLACE_ATTRIBUTE, attributes);
			logger.debug("Enabled user: "+userDN);
			return true;
		} catch (NamingException e) {
			logger.error(e.toString());
			e.printStackTrace();
		}
		return false;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	/**
	 * return the sAMAccountName from userDN in String
	 * @param userDN - user's LDAP distinguish name
	 * @return the sAMAccountName of userDN in String if there is. Otherwise return an empty String.
	 */
	public String getUsername(String userDN){
		logger.debug("about to search for the username of user: " + userDN);
		userDN = LdapTool.escapedCharsOnCompleteUserDN(userDN);
		try {
			Attributes attrs = ctx.getAttributes(new LdapName(userDN));
			String username = attrs.get("sAMAccountName") == null ? "" : attrs.get("sAMAccountName").get().toString();
			logger.debug("finished searching for the username of user: " + userDN);
			return username;
		} catch (NamingException e) {
			logger.error(e.toString());
			e.printStackTrace();
		}
		return "";
	}
	
	
	/**
	 * close the LDAP Server connection
	 */
	public void close(){
		try{
			ctx.close();
			//MODIFIED: Handle general exception (rather than just naming exception)
		}catch(Exception ex){}
	}



	/**
	 * This method is used to escape any reserved chars that contains in the full complete DN.
	 * e.g. given dn="CN=Mike+Jr,OU=Group, I,OU=Clients,DC=orion,DC=dmz"
	 * @param dn is a full complete dn that has not been escaped any reserved chars. 
	 * @return a final dn that has been escaped all reserved chars (e.g. dn="CN=Mike\+Jr,OU=Group\, I,OU=Clients,DC=orion,DC=dmz")
	 */
	public static String escapedCharsOnCompleteUserDN(String originalDN) {
		// getting cn
		String cn = LdapTool.getCNValueFromDN(originalDN);
		// ecaspe reserved char on CN value
		String escapedCn = Rdn.escapeValue(cn);

		// used to get CN and OU value (preventing CN and OU were written in
		// lowercacse)
		String tempDN = originalDN.toUpperCase();
		// getting OU value
		int dnValueStartIndex = tempDN.indexOf("OU=") + "OU=".length();
		int dnValueEndIndex = tempDN.indexOf(",OU=", dnValueStartIndex);
		String ou = originalDN.substring(dnValueStartIndex, dnValueEndIndex);
		String escapedOu = Rdn.escapeValue(ou);

		String dn = originalDN.replace(cn, escapedCn);
		dn = dn.replace(ou, escapedOu);
		
		return dn;
	}
	
	
	
	/**
	 * This method is used to escape any reserved chars that contains in the full complete Group DN.
	 * e.g. given groupDN="cn=Associated, I,OU=Groups,DC=orion,DC=dmz"
	 * @param dn is a full complete dn that has not been escaped any reserved chars. 
	 * @return a final dn that has been escaped all reserved chars (e.g. groupDN="cn=Associated Regional and University Pathologists\, I,OU=Groups,DC=orion,DC=dmz")
	 */
	public static String escapedCharsOnCompleteGroupDN(String originalDN) {
		// getting cn
		String cn = LdapTool.getCNValueFromDN(originalDN);
		// ecaspe reserved char on CN value
		String escapedCn = Rdn.escapeValue(cn);
		
		String dn = originalDN.replace(cn, escapedCn);

		return dn;
	}
	
	
	/**
	 * This method is used to escape any reserved chars that contains in the full complete company DN (copmany that stored in Clients folder).
	 * e.g. given groupDN="ou=group, name,OU=Clients,DC=orion,DC=dmz"
	 * @param dn is a full complete dn that has not been escaped any reserved chars. 
	 * @return a final dn that has been escaped all reserved chars (e.g. groupDN="ou=group\, name,OU=Clients,DC=orion,DC=dmz")
	 */
	public static String escapedCharsOnCompleteCompanyDN(String originalDN) {
		// getting cn
		String cn = LdapTool.getOUvalueFromDNThasHasTwoOU(originalDN);
		// ecaspe reserved char on CN value
		String escapedCn = Rdn.escapeValue(cn);
		
		String dn = originalDN.replace(cn, escapedCn);

		return dn;
	}
	
	
	
	/**
	 * a helper method to get only cn value from the dn string. 
	 * Note: that if the given dn contains escaped chars the result also contains the escaped chars.
	 * And if the give dn doesn't contains an escaped char the result also doesn't contain an escaped char.
	 * @param dn (e.g. cn=Associated Regional and University Pathologists, I,OU=Groups,DC=orion,DC=dmz)
	 * @return only CN value (e.g. Associated Regional and University Pathologists, I)
	 */
	public static String getCNValueFromDN(String dn) {
		// used to get CN and OU value (preventing CN and OU were written in
		// lowercacse)
		String tempDN = dn.toUpperCase();

		// getting CN value
		int cnValueStartIndex = tempDN.indexOf("CN=") + "CN=".length();
		int cnValueEndIndex = tempDN.indexOf(",OU=", cnValueStartIndex);
		if(cnValueEndIndex < 0){
			cnValueEndIndex = tempDN.indexOf(",CN=", cnValueStartIndex);
		}
		String cn = dn.substring(cnValueStartIndex, cnValueEndIndex);
		return cn;
	}
	
	/**
	 * a helper method to get only ou value from the dn string. 
	 * Note: that if the given dn contains escaped chars the result also contains the escaped chars.
	 * And if the give dn doesn't contains an escaped char the result also doesn't contain an escaped char.
	 * @param dn that has two OU values: e.g. CN=XX,OU=YY,OU=Clients,DC=orion,DC=dmz
	 * @return first ou value (e.g. YY)
	 */
	public static String getOUvalueFromDNThasHasTwoOU(String dn) {
		// used to get CN and OU value (preventing CN and OU were written in
		// lowercacse)
		String tempDN = dn.toUpperCase();

		String ou = null;
		
		try{
			// we believe that dn has two OU values (i.e. CN=XX,OU=YY,OU=Clients,DC=orion,DC=dmz)
			// but, if the case that dn has only one OU value occured (i.e. CN=XX,OU=Clients,DC=orion,DC=dmz)
			// then it will throw StringIndexOutOfBoundsException
			int ouValueStartIndex = tempDN.indexOf("OU=") + "OU=".length();
			int ouValueEndIndex = tempDN.indexOf(",OU=", ouValueStartIndex);
			ou = dn.substring(ouValueStartIndex, ouValueEndIndex);
			
			// So, instead of let the program through exception
			// we try to assume that dn has only one OU value
		} catch (StringIndexOutOfBoundsException e){
			ou = getOUvalueFromDNThatHasOneOU(dn);
		}
		return ou;
	}
	
	
	
	/**
	 * a helper method to get only ou value from the dn string.
	 * Note: that if the given dn contains escaped chars => the result also contains the escaped cahrs.
	 * And if the given dn doesn't contains escaped chars => the result also doesn't contains the escaped cahrs.
	 * @param dn: full DN that has only one OU value: e.g. CN=XX,OU=Clients,DC=orion,DC=dmz
	 * @return the ou value e.g. "Clients"
	 */
	public static String getOUvalueFromDNThatHasOneOU(String dn){
		// used to get OU and DC value (preventing the case that DC and OU were written in lowercacse)
		String tempDN = dn.toUpperCase();
		
		// getting OU value
		int ouValueStartIndex = tempDN.indexOf("OU=") + "OU=".length();
		int ouValueEndIndex = tempDN.indexOf(",DC=", ouValueStartIndex);
		String ou = dn.substring(ouValueStartIndex, ouValueEndIndex);
		return ou;
	}
}
