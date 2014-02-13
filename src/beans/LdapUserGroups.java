package beans;

import ldap.LdapTool;
import java.util.SortedSet;

public class LdapUserGroups {
	private SortedSet<String> userGroups;
	public LdapUserGroups(){
		LdapTool lt = new LdapTool();
		userGroups = lt.getUserGroups();
		lt.close();
	}
	public void setUserGroups(SortedSet<String> userGroups){
		this.userGroups = userGroups;
	}
	
	/*
	 * public SortedSet<String> getUserGroups(){
		return this.userGroups;
	}
	*/
	public String[] getUserGroups(){
		String[] output = new String[userGroups.size()];
		output = (String[])userGroups.toArray(output);
		return output;
	}
}
