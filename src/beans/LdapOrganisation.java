package beans;

import java.util.TreeMap;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import ldap.LdapTool; 

public class LdapOrganisation {
	private TreeMap<String,String[]> users;
	private String name;
	private String distinguishedName;
	
	public void processOrganisationName(String name){
		LdapTool lt = new LdapTool();
		users = lt.getGroupUsers(name);
		Attributes attrs = lt.getOrganisationAttributes(name);
		try{
			setName(attrs.get("name")!=null?attrs.get("name").get().toString():"");
			setDistinguishedName(attrs.get("distinguishedName")!=null?attrs.get("distinguishedName").get().toString():"");
		}catch(NamingException ex){}
		lt.close();
	}

	public void setDistinguishedName(String distinguishedName) {
		this.distinguishedName = distinguishedName;
	}
	public String getDistinguishedName(){
		return this.distinguishedName;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getName(){
		return this.name;
	}
	public void setUsers(TreeMap<String,String[]> users){
		this.users = users;
	}
	public TreeMap<String,String[]> getUsers(){
		return this.users;
	}
	public String[] getUsersKeys(){
		String[] names = new String[users.size()];
		names = users.keySet().toArray(names);
		return names;
	}
}