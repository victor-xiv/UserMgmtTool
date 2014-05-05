package ldap;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.naming.CommunicationException;
import javax.naming.Context;
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

import org.apache.log4j.Logger;

import tools.LoggerTool;

public class LdapTool {
	private DirContext ctx;
	private Hashtable<String, String> env;
	private Properties props = LdapProperty.getConfiguration();
	
	Logger logger = LoggerTool.setupDefaultRootLogger(); //initialize with default root logger
	
	
	/**
	 * LdapTool represent the connection with LDAP server.
	 * The LdapTool object is successfully constructed if and only if it successfully connects to LDAP server.
	 * Otherwise, it will throw either FileNotFoundException or NamingException.
	 * 
	 * @throws FileNotFoundException when ldap.properties configuration file is nout found
	 * @throws NamingException when connection with LDAP server failed
	 */
	public LdapTool() throws FileNotFoundException, NamingException{
		
		// if LDAP config file is not found
		// the props will contain an "error" key
		if(props.getProperty("error") != null){
			throw new FileNotFoundException("LDAP " + ErrorConstants.CONFIG_FILE_NOTFOUND);
			// don't need to log, because it has been logged in LdapProperty.getConfiguration()
		}
		
		// setup environment map to connect to ldap server
		env = new Hashtable<String, String>();
		env.put(Context.INITIAL_CONTEXT_FACTORY, props.getProperty(LdapConstants.LDAP_CLASS));
		env.put(Context.PROVIDER_URL, props.getProperty(LdapConstants.LDAP_URL));
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		env.put(Context.SECURITY_PRINCIPAL, props.getProperty(LdapConstants.ADMIN_DN));
		env.put(Context.SECURITY_CREDENTIALS, props.getProperty(LdapConstants.ADMIN_PWD));
		
		// if the config file declared to connect ldap server through ssl
		boolean sslEnabled = props.getProperty(LdapConstants.SSL_ENABLED).equals("true");
		if( sslEnabled ){
			env.put(Context.SECURITY_PROTOCOL, "ssl" ); // SSL
			System.setProperty("javax.net.debug", "ssl");
			System.setProperty("javax.net.ssl.trustStore", props.getProperty(LdapConstants.SSL_CERT_LOC));
			System.setProperty( "javax.net.ssl.trustStorePassword", props.getProperty(LdapConstants.SSL_CERT_PWD));
		}
		
		try{
			ctx = new InitialDirContext(env);
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
	

	
	/**
	 * update the Ldap's user with the given attributes (paramMaps). The userDN is the index 0 element of the value that mapped with key "dn"
	 * @param paramMaps: given attributes that need to be updated or deleted.
	 * @return: {"false","Failed to update details for user: "+userDN} if it failed to update.
	 *          {"true", userDN} or {"true", newUserDN} otherwise.
	 */
	public String[] updateUser(Map<String,String[]> paramMaps){
		String userDN = paramMaps.get("dn")[0];
		Attributes attrs = getUserAttributes(userDN);
		
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
			String groupBaseDN = props.getProperty(LdapConstants.GROUP_DN);
			newUserDN = "CN="+fullname+",OU="+replaceMap.get("company")[0]+","+groupBaseDN;
		}

		// This object is used to modify the user object in Ldap Server
		ModificationItem[] mods = new ModificationItem[replaceMap.size()+removeList.size()];

		// mods has length 0, means nothing to update
		if( mods.length == 0 ){
			return new String[]{"true",userDN};
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
				ctx.rename(userDN, newUserDN);
				ctx.modifyAttributes(newUserDN, mods);
			}else{
				ctx.modifyAttributes(userDN, mods);
			}
			logger.info("Updated details for user: "+userDN);
		} catch (NamingException e) {
			logger.error("Exception while updating the Ldap user's attribute", e);
			return new String[]{"false","Failed to update details for user: "+userDN + " into Ldap Server."};
		}
		
		// updating password => this.changePassword() will handle this
		if(!password.equals("")){
			if(!changePassword(userDN, password)){
				logger.info("Failed to update password for user: "+userDN);
				return new String[]{"false","Failed to update password for user: "+userDN};
			}
		}
		if(!newUserDN.equals(""))
			return new String[]{"true",newUserDN};
		else
			return new String[]{"true",userDN};
	}
	
	
	
	
	
	public boolean addUser(Map<String,String[]> paramMaps){
		try{
			// http://forums.sun.com/thread.jspa?threadID=582103
			// http://msdn.microsoft.com/en-us/library/ms675090(VS.85).aspx
			String fullname = "";
			if(paramMaps.get("displayName")[0] != null){
				fullname = paramMaps.get("displayName")[0];
			}else{
				fullname = paramMaps.get("givenName")[0] + " " + paramMaps.get("sn")[0];
			}
			String groupBaseDN = props.getProperty(LdapConstants.GROUP_DN);
			String companyName = paramMaps.get("company")[0];
			if(!companyExists(companyName)){
				if(!addCompany(companyName)){
					return false;
				}
			}
			String companyDN = props.getProperty(LdapConstants.GROUP_ATTR)+"="+companyName+","+groupBaseDN;
			String userDN = "CN="+fullname+","+companyDN;
			Attributes attributes = new BasicAttributes(true);
			//attributes.put("distinguishedName", userDN);
			attributes.put("objectClass", "top");
			attributes.put("objectClass", "person");
			attributes.put("objectClass", "organizationalPerson");
			attributes.put("objectClass", "user");
			attributes.put("sAMAccountName", paramMaps.get("sAMAccountName")[0]);
			attributes.put("cn", fullname);
			attributes.put("givenName", paramMaps.get("givenName")[0]);
			attributes.put("sn", paramMaps.get("sn")[0]);
			attributes.put("displayName", fullname);
			attributes.put("userPrincipalName",paramMaps.get("sAMAccountName")[0]+"@"+
							props.getProperty(LdapConstants.LDAP_DOMAIN));
			attributes.put("description", paramMaps.get("description")[0]);
			attributes.put("department", paramMaps.get("department")[0]);
			attributes.put("company", paramMaps.get("company")[0]);
			if(paramMaps.get("info") != null && !paramMaps.get("info")[0].equals(""))
				attributes.put("info", paramMaps.get("info")[0]);
			if(paramMaps.get("streetAddress") != null && !paramMaps.get("streetAddress")[0].equals(""))
				attributes.put("streetAddress", paramMaps.get("streetAddress")[0]);
			if(paramMaps.get("l") != null && !paramMaps.get("l")[0].equals(""))
				attributes.put("l", paramMaps.get("l")[0]);
			if(paramMaps.get("st") != null && !paramMaps.get("st")[0].equals(""))
				attributes.put("st", paramMaps.get("st")[0]);
			if(paramMaps.get("postalCode") != null && !paramMaps.get("postalCode")[0].equals(""))
				attributes.put("postalCode", paramMaps.get("postalCode")[0]);
			attributes.put("c", paramMaps.get("c")[0]);
			attributes.put("telephoneNumber", paramMaps.get("telephoneNumber")[0]);
			if(paramMaps.get("facsimileTelephoneNumber") != null && !paramMaps.get("facsimileTelephoneNumber")[0].equals(""))
				attributes.put("facsimileTelephoneNumber", paramMaps.get("facsimileTelephoneNumber")[0]);
			if(paramMaps.get("mobile") != null && !paramMaps.get("mobile")[0].equals(""))
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
			
			logger.info("About to create User: " + userDN);
			ctx.createSubcontext(userDN, attributes);
			//ADDITIONAL CODE
			//Get 'Groups' base DN
			String baseDN = props.getProperty(LdapConstants.BASEGROUP_DN);
			if (baseDN==null)
				baseDN = "OU=Groups,DN=orion,DN=dmz";
			//Set DN for company organisational group
			String orgGroupDN = "CN="+companyName+","+baseDN;
			//Add user to company organistion group
			addUserToGroup(userDN, orgGroupDN);
			//ADDITIONAL CODE ENDS
			logger.info("Successfully created User: " + userDN);
			String password = paramMaps.get("password01")[0];
			if(!changePassword(userDN, password)){
				deleteUser(userDN);
				return false;
			}

			String defaultGroupID = LdapConstants.GROUP_DEFAULT;
			for(Enumeration<?> e = props.propertyNames(); e.hasMoreElements(); ){
				String key = (String)e.nextElement();
				if (key.length() >= defaultGroupID.length() &&
						key.substring(0, defaultGroupID.length()).equals(defaultGroupID)){
					if(!addUserToGroup(userDN, props.getProperty(key))){
						deleteUser(userDN);
						return false;
					}
				}
			}
			
			if(paramMaps.get("isLdapClient") != null){
				String groupDN = props.getProperty(LdapConstants.GROUP_LDAP_CLIENT);
				if(!addUserToGroup(userDN, groupDN)){
					deleteUser(userDN);
					return false;
				}
			}
		
			return true;
		}catch(NamingException ex){
			System.err.println(ex.toString());
			//ADDED LINE: print stack trace, not just error string
			ex.printStackTrace();
			return false;
		}
	}
	
	
	
	/**
	 * Update LDAP server: add a user (who represented by userDN) to a group (represented by groupDN)
	 * @param userDN representing user who needed to be assigned to the group
	 * @param groupDN representing group that needed to be assigned to the user
	 * @return true if the Adding process completed successfully
	 * @throws NamingException if an exception thrown during the process
	 */
	public boolean addUserToGroup(String userDN, String groupDN) throws NamingException{
		try	{
			ModificationItem member[] = new ModificationItem[1];
			member[0]= new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute("member", userDN)); 
			ctx.modifyAttributes(groupDN,member);
			logger.info("Added user "+userDN+" to group: " + groupDN);
			return true;
		}
		catch (NamingException e) {
			 logger.error("Problem adding user: "+userDN+" to group: " + groupDN, e);
			 throw new NamingException("Adding user to a group: " + ErrorConstants.FAIL_UPDATE_LDAP);
		}
	}
	
	
	
	/**
	 * remove the given userDN from given groupDN  
	 * @param userDN - ldap's user DN (it's not just a name, it is a DN of that user)
	 * @param groupDN - ldap's group DN (it's not just a name, it is a DN of that group)
	 * @return true if the removing successfully
	 * @throws NamingException if it cannot remove this userDN from the groupDN
	 */
	public boolean removeUserFromAGroup(String userDN, String groupDN) throws NamingException{
		try	{
			// create a remove attribute (groupDN is an attribute of userDN)
			ModificationItem member[] = new ModificationItem[1];
			member[0]= new ModificationItem(DirContext.REMOVE_ATTRIBUTE, new BasicAttribute("member", userDN)); 
			// apply the remove attribute
			ctx.modifyAttributes(groupDN,member);
			logger.info("Added user "+userDN+" to group: " + groupDN);
			return true;
		}
		catch (NamingException e) {
			 logger.error("Problem in removing user: "+userDN+" from group: " + groupDN, e);
			 throw new NamingException("Removing user from a group: " + ErrorConstants.FAIL_UPDATE_LDAP);
		}
	}
	
	
	/**
	 * Change the password of give userDN with the given password
	 * @param userDN: Ldap user whose password need to be changed
	 * @param password: new password
	 * @return true if the modification is successful. false otherwise.
	 */
	public boolean changePassword(String userDN, String password){
		try{
			String quotedPwd = "\""+password+"\"";
			byte encodedPwd[] = quotedPwd.getBytes( "UTF-16LE" );
			ModificationItem[] mods = new ModificationItem[2];
			mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("pwdLastSet", "-1"));
			mods[1] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("unicodePwd", encodedPwd));
			ctx.modifyAttributes(userDN, mods);
			logger.info("Updated password for user: "+userDN);
			return true;
		}catch(NamingException ex){
			logger.error(String.format("Exception while modifying user's password, (userDN, psw) = (%s, %s)", userDN, password), ex);
		}catch(UnsupportedEncodingException ex){
			logger.error("Exception while encoding the given password: " + userDN,ex);
		}
		return false;
	}
	
	
	/**
	 * delete the user (defined by given userDN)
	 * @param userDN: given userDN that need to be deleted
	 * @return true, if the deletion successful, false otherwise.
	 */
	public boolean deleteUser(String userDN){
		try{
			ctx.destroySubcontext(userDN);
			return true;
		}catch(NamingException ex){
			logger.error("Exception while deleting a give userDN: " + userDN, ex);
			return false;
		}
	}
	
	
	
	
	
	public SortedSet<String> getUserGroups(){
		String baseDN = props.getProperty(LdapConstants.GROUP_DN);
		String groupAttr = props.getProperty(LdapConstants.GROUP_ATTR);
		String filter = "("+groupAttr+"=*)";
		SortedSet<String> output = new TreeSet<String>();
		try{
			NamingEnumeration<SearchResult> e = ctx.search(baseDN, filter, null);
			while(e.hasMore()){
				SearchResult results = (SearchResult)e.next();
				Attributes attributes = results.getAttributes();
				output.add((String)attributes.get(groupAttr).get());
			}
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
	//Get a list of all Groups (not organizations) in the AD
	public SortedSet<String> getBaseGroups(){
		String baseDN = props.getProperty(LdapConstants.BASEGROUP_DN);
		if (baseDN == null)
			baseDN = "OU=Groups,DC=orion,DC=dmz";
		String groupAttr = props.getProperty(LdapConstants.BASEGROUP_ATTR);
		if (groupAttr == null)
			groupAttr = "cn";
		String filter = "("+groupAttr+"=*)";
		SortedSet<String> output = new TreeSet<String>();
		try{
			NamingEnumeration<SearchResult> e = ctx.search(baseDN, filter, null);
			while(e.hasMore()){
				SearchResult results = (SearchResult)e.next();
				Attributes attributes = results.getAttributes();
				output.add((String)attributes.get(groupAttr).get());
			}
		}catch(NamingException ex){
			logger.error(ex.toString());
			ex.printStackTrace();
		}catch(NullPointerException ex){
			logger.error(ex.toString());
			ex.printStackTrace();
		}
		return output; 
	}
	
	
	
	
	public TreeMap<String,String[]> getGroupUsers(String name){
		TreeMap<String,String[]> users = new TreeMap<String,String[]>();
		String baseDN = "OU="+name+","+props.getProperty(LdapConstants.GROUP_DN);
		String userAttr = props.getProperty(LdapConstants.USER_ATTR);
		String filter = "("+userAttr+"=*)";
		try{
			NamingEnumeration<SearchResult> e = ctx.search(baseDN, filter, null);
			while(e.hasMore()){
				try{
					SearchResult results = (SearchResult)e.next();
					Attributes attributes = results.getAttributes();
					String cn = attributes.get("cn").get().toString(); // displayName
					String dn = attributes.get("distinguishedName").get().toString();
					int userAccountControl = Integer.parseInt(attributes.get("userAccountControl").get().toString());
					String disabled = "enabled";
					if( (userAccountControl & 2) > 0 ){
						disabled = "disabled";
					}
					users.put(cn, new String[]{dn, "enabled"});
				} catch (NullPointerException ne){
					logger.error("Exception while querying all users from " + baseDN, ne);
				}
			}
		}catch(NamingException ex){
			logger.error("Exception while querying for: " + baseDN + " from Ldap server.", ex);
		}
		return users;
	}
	
	
	
	
	
	public Attributes getOrganisationAttributes(String name){
		try{
			String baseDN = "OU="+name+","+props.getProperty(LdapConstants.GROUP_DN);
			Attributes attrs = ctx.getAttributes(baseDN);
			return attrs;
		}catch(NamingException ex){
			logger.error(ex.toString());
			ex.printStackTrace();
		}
		return null;
	}
	
	
	/**
	 * get attributes of a Ldap user who has userDN
	 * @param userDN: of the Ldap user
	 * @return an Attributes object that stores all the current attributes belong to this userDN.
	 *         null otherwise.
	 */
	public Attributes getUserAttributes(String userDN){
		try{
			Attributes attrs = ctx.getAttributes(userDN);
			return attrs;
		}catch(NamingException ex){
			logger.error("Exception while querying all attribtues of a user: " + userDN, ex);
		}
		return null;
	}
	
	public Attributes getGroupAttributes(String companyName){
		String baseDN = props.getProperty(LdapConstants.BASEGROUP_DN);
		if (baseDN==null)
			baseDN = "OU=Groups,DN=orion,DN=dmz";
		String companyDN = "CN="+companyName+","+baseDN;
		try{
			Attributes attrs = ctx.getAttributes(companyDN);
			return attrs;
		}catch(NamingException ex){
			logger.error(ex.toString());
			ex.printStackTrace();
		}
		return null;
	}
	
	
	
	/**
	 * get the company name that the userDN is working for (from the LDAP server)
	 * @param userDN represent the user/company
	 * @return company name in String
	 */
	public String getUserCompany(String userDN){
		try{
			Attributes attrs = ctx.getAttributes(userDN);
			if(attrs.get("company") == null ){
				String company = userDN;
				int index = company.indexOf(",");
				// if there's "," => get the substring after ","
				if(index != -1){
					company = company.substring(index+1);
					index = company.indexOf(",");
					// if there's "," => get the substring after ","
					if(index != -1) company = company.substring(0, index);
				}
				index = company.indexOf("=");
				// if there's "=" => get the substring after "="
				if(index != -1) company = company.substring(index+1);
				return company;
			}else{
				return attrs.get("company").get().toString();
			}
		}catch(NamingException ex){
			logger.error("Trying to get attribute " + userDN + "\t", ex);
			ex.printStackTrace();
		}
		return null;
	}
	
	
	
	/**
	 * check if the given companyName exists in the Ldap server
	 * @param companyName
	 * @return true if the given companyName is in Ldap Server, false otherwise
	 */
	public boolean companyExists(String companyName){
		String baseDN = props.getProperty(LdapConstants.GROUP_DN);
		String filter = "("+props.getProperty(LdapConstants.GROUP_ATTR)+"="+companyName+")";
		NamingEnumeration<SearchResult> e;
		try {
			e = ctx.search(baseDN, filter, null);
			if(e.hasMore()){
				return true;
			}
		} catch (NamingException ex) {
		}
		return false;
	}
	
	
	/**
	 * Simple function to check if a given company exists as a group
	 * @param companyName - the company under consideration
	 * @return true if more than zero search matches. false otherwise
	 */
	public boolean companyExistsAsGroup(String companyName){
		//Get DN for 'Groups'
		String baseDN = props.getProperty(LdapConstants.BASEGROUP_DN);
		//Create search string (CN=<companyName>)
		String filter = "(CN="+companyName+")";
		NamingEnumeration<SearchResult> e;
		try {
			//Run search. If more than zero matches, return true
			e = ctx.search(baseDN, filter, null);
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
	

	/**
	 * Simple function to check if a given username exists
	 * @param username - relevant username (sAMAccountName)
	 * @param company - user's company
	 * @return true if more than zero search matches.
	 */
	public boolean usernameExists(String username, String company){
		//Search user's company
		String baseDN = "OU="+company+","+props.getProperty(LdapConstants.GROUP_DN);
		//Create search string (sAMAccountName=<username>)
		String filter = "("+props.getProperty(LdapConstants.USER_ATTR)+"="+username+")";
		NamingEnumeration<SearchResult> e;
		try {
			//Run search. If more than zero matches, return true
			e = ctx.search(baseDN, filter, null);
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
	
	/**
	 * Simple function to check if a given email exists.
	 * 
	 * @param email - user's email
	 * @param company - user's company
	 * @return true if more than zero search matches.
	 */
	public boolean emailExists(String email, String company){
		//Search user's company
		String baseDN = "OU="+company+","+props.getProperty(LdapConstants.GROUP_DN);
		//Create search string (mail=<email>)
		String filter = "(mail="+email+")";
		NamingEnumeration<SearchResult> e;
		try {
			//Run search. If more than zero matches, return true
			e = ctx.search(baseDN, filter, null);
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
		//Search user's company
		String baseDN = "OU="+company+","+props.getProperty(LdapConstants.GROUP_DN);
		//Create search string (CN=Display Name)
		String filter = "CN="+fullname;
		NamingEnumeration<SearchResult> e;
		try {
			//Run search. If more than zero matches, return true
			e = ctx.search(baseDN, filter, null);
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
	
	
	/**
	 * Get email address, given the corresponding username. 
	 * 
	 * @param username - the user's sAMAccountName
	 * @param company - user's company
	 * @return the first matching email, if there is a match, else returns null
	 */
	public String getEmail(String username, String company){
		//Search user's company
		String baseDN = "OU="+company+","+props.getProperty(LdapConstants.GROUP_DN);
		//Create search string (sAMAccountName=<username>)
		String filter = "("+props.getProperty(LdapConstants.USER_ATTR)+"="+username+")";
		NamingEnumeration<SearchResult> e;
		try {
			//Run search. If more than zero matches, return true
			e = ctx.search(baseDN, filter, null);
			if(e.hasMore()){
				SearchResult ne = e.next();
				String mail = (String) ne.getAttributes().get("mail").get();
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
	
	/**
	 * Get full name, given the corresponding username. 
	 * 
	 * @param username - the user's sAMAccountName
	 * @param company - user's company
	 * @return the first matching name, if there is a match, else returns null.
	 */
	public String getName(String username, String company){
		//Search user's company
		String baseDN = "OU="+company+","+props.getProperty(LdapConstants.GROUP_DN);
		//Create search string (sAMAccountName=<username>)
		String filter = "("+props.getProperty(LdapConstants.USER_ATTR)+"="+username+")";
		NamingEnumeration<SearchResult> e;
		try {
			//Run search. If more than zero matches, return true
			e = ctx.search(baseDN, filter, null);
			if(e.hasMore()){
				SearchResult ne = e.next();
				String mail = (String) ne.getAttributes().get("cn").get();
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
	//ADDITIONAL FUNCTION
	//Deprecated function
	/*public boolean userDNExists(Map<String, String[]> paramMaps) {
		String fullname = "";
		if(paramMaps.get("displayName")[0] != null){
			fullname = paramMaps.get("displayName")[0];
		}else{
			fullname = paramMaps.get("givenName")[0] + " " + paramMaps.get("sn")[0];
		}
		String groupBaseDN = props.getProperty(LdapConstants.GROUP_DN);
		String companyName = paramMaps.get("company")[0];

		String companyDN = props.getProperty(LdapConstants.GROUP_ATTR)+"="+companyName+","+groupBaseDN;
		String userDN = "CN="+fullname+","+companyDN;
		if (getUsername(userDN).equals("")) {
			return false;
		}
		return true;
	}*/
	
	/**
	 * Add the given companyName into the Ldap server as a Client (Ldap user)
	 * @param companyName that need to be added
	 * @return true if the companyName can be added successfully
	 *         false otherwise
	 */
	public boolean addCompany(String companyName){
		// get base dn for the user (it should be: OU=Clients,DC=orion,DC=dmz)
		String baseDN = props.getProperty(LdapConstants.GROUP_DN);
		String companyDN = props.getProperty(LdapConstants.GROUP_ATTR)+"="+companyName+","+baseDN;
		Attributes attributes = new BasicAttributes(true);
		attributes.put("objectClass", "top");
		attributes.put("objectClass", "organizationalUnit");
		attributes.put("ou", companyName);
		attributes.put("distinguishedName", baseDN);
		attributes.put("name", companyName);
		try {
			if (ctx == null)
				logger.info("ctx uninitialised");
			ctx.createSubcontext(companyDN, attributes);
			logger.info("Company with DN: "+companyDN+" was added successfully");
			return true;
		} catch (NamingException e) {
			logger.error(String.format("Exception while adding %s into Ldap server as a user.", companyName), e);
			return false;
		}
	}
	
	//ADDITIONAL FUNCTION
	/**
	 * Add Company to 'Groups'
	 * @param companyName that need to be added as a Ldap Group
	 * @return true if the Organisation was added successfully.
	 * 		   false otherwise.
	 */
	public boolean addCompanyAsGroup(String companyName){
		//Get base DN for 'Groups'
		String baseDN = props.getProperty(LdapConstants.BASEGROUP_DN);
		if (baseDN==null)
			baseDN = "OU=Groups,DN=orion,DN=dmz";
		//Get company DN as group
		String companyDN = "CN="+companyName+","+baseDN;
		//Set basic attributes
		Attributes attributes = new BasicAttributes(true);
		attributes.put("objectClass", "top");
		attributes.put("objectClass", "group");
		attributes.put("cn", companyName);
		attributes.put("distinguishedName", baseDN);
		attributes.put("name", companyName);
		try {
			if (ctx == null)
				logger.info("ctx uninitialised");
			//Create company group and log success
			ctx.createSubcontext(companyDN, attributes);
			logger.info("Organisation with DN: "+companyDN+" was added as group successfully");
			return true;
		//If error, log detail and stack trace
		} catch (NamingException e) {
			logger.error(String.format("Failed to add organisation: %s, as a Ldap Group", companyName), e);
			return false;
		}
	}
	

	/**
	 * Produce groupDN from group name
	 * @param groupName - group name
	 * @return group DN
	 */
	public String getDNFromGroup(String groupName) {
		String baseDN = props.getProperty(LdapConstants.BASEGROUP_DN);
		String attrName = props.getProperty(LdapConstants.BASEGROUP_ATTR);
		String dn = attrName+"="+groupName+","+baseDN;
		return dn;
	}
	

	/**
	 * Produce organisation DN from organisation name
	 * @param orgName - organisation name
	 * @return organisation DN
	 */
	public String getDNFromOrg(String orgName) {
		String baseDN = props.getProperty(LdapConstants.GROUP_DN);
		String attrName = props.getProperty(LdapConstants.GROUP_ATTR);
		String dn = attrName+"="+orgName+","+baseDN;
		return dn;
	}

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	public boolean isAccountDisabled(String userDN){
		try {
			Attributes attrs = ctx.getAttributes(userDN);
			int userAccountControl = Integer.parseInt(attrs.get("userAccountControl").get().toString());
			return (userAccountControl & 2) > 0;
		} catch (NamingException e) {
			logger.error(e.toString());
			e.printStackTrace();
		}
		return false;
	}
	
	public boolean disableUser(String userDN){
		int UF_PASSWD_NOTREQD = 0x0020;
		int UF_DONT_EXPIRE_PASSWD = 0x10000;
		int UF_NORMAL_ACCOUNT = 0x0200;
		int UF_ACCOUNTDISABLE = 0x2;
		Attributes attributes = new BasicAttributes(true);
		attributes.put("userAccountControl",Integer.toString(UF_NORMAL_ACCOUNT +
				UF_ACCOUNTDISABLE + UF_PASSWD_NOTREQD + UF_DONT_EXPIRE_PASSWD));
		try {
			ctx.modifyAttributes(userDN, DirContext.REPLACE_ATTRIBUTE, attributes);
			logger.info("Disabled user: "+userDN);
			return true;
		} catch (NamingException e) {
			logger.error(e.toString());
			e.printStackTrace();
		}
		return false;
	}
	public boolean enableUser(String userDN){
		int UF_PASSWD_NOTREQD = 0x0020;
		int UF_DONT_EXPIRE_PASSWD = 0x10000;
		int UF_NORMAL_ACCOUNT = 0x0200;
		Attributes attributes = new BasicAttributes(true);
		attributes.put("userAccountControl",Integer.toString(UF_NORMAL_ACCOUNT +
				UF_PASSWD_NOTREQD + UF_DONT_EXPIRE_PASSWD));
		try {
			ctx.modifyAttributes(userDN, DirContext.REPLACE_ATTRIBUTE, attributes);
			logger.info("Enabled user: "+userDN);
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
		try {
			Attributes attrs = ctx.getAttributes(userDN);
			String username = attrs.get("sAMAccountName") == null ? "" : attrs.get("sAMAccountName").get().toString();
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
}
