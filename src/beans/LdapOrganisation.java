package beans;

import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.TreeMap;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import ldap.LdapTool;

import org.apache.log4j.Logger;

public class LdapOrganisation{
	private TreeMap<String,String[]> users;
	private String name;
	private String distinguishedName;
	
	
	/**
	 * process the map that storing the properties of the Group Users from Ldap. 
	 * The key of the map is the display name of that group (it is not Distinguished name).
	 * The value is an array, the first element (at index 0) is the distinguished name of this group and second element (at index 1) is the word "enabled"
	 * @param name: is just a simple name (it is not a Distinguished name). The method will build a DN name. Please don't provide a DN name and Please don't escape any character in the name (just leave it as it is). (e.g. correct argument is: "Health, Bot"  not "Health\, Bot"
	 */
	public void processOrganisationName(String name) throws ConnectException{
		Logger logger = Logger.getRootLogger();
		
		logger.debug("about to query for the attribute of the organisation: " + name);
		
		LdapTool lt = null;
		try {
			lt = new LdapTool();
		} catch (FileNotFoundException fe){
			throw new ConnectException(fe.getMessage());	
			// we are not logging this error here, because it has been logged in LdapTool()
		} catch (NamingException e) {
			throw new ConnectException(e.getMessage());
			// we are not logging this error here, because it has been logged in LdapTool()
		}
		
		if( lt != null){
			users = lt.getClientUsers(name);
			Attributes attrs = lt.getOrganisationAttributes(name);
			try{
				setName(attrs.get("name")!=null ? attrs.get("name").get().toString():"");
				setDistinguishedName(attrs.get("distinguishedName")!=null?attrs.get("distinguishedName").get().toString():"");
			}catch(NamingException ex){
				lt.close();
				throw new ConnectException(ex.getMessage());
				// we are not logging this error here, because it has been logged in LdapTool()
			}
			
			lt.close();
		}
		
		logger.debug("Finished querying for the organisation's attributes.");
	}

	/**
	 * set the distinguish name of this organisation
	 * @param distinguishedName
	 */
	public void setDistinguishedName(String distinguishedName) {
		this.distinguishedName = distinguishedName;
	}
	
	/**
	 * get the distinguish name of this organisation
	 * @return
	 */
	public String getDistinguishedName(){
		return this.distinguishedName;
	}
	
	/**
	 * set the simple name of this organisation (not the distinguish name)
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * get the simple name of this organisation (not the distinguish name)
	 * @return
	 */
	public String getName(){
		return this.name;
	}
	/**
	 * set the whole properties of this organisation
	 * @param users
	 */
	public void setUsers(TreeMap<String,String[]> users){
		this.users = users;
	}
	/**
	 * get the whole properties of this organisation
	 * @return
	 */
	public TreeMap<String,String[]> getUsers(){
		return this.users;
	}
	/**
	 * get the array of the keys of the properties of this organisation
	 * @return
	 */
	public String[] getUsersKeys(){
		String[] names = new String[users.size()];
		names = users.keySet().toArray(names);
		return names;
	}
	

}