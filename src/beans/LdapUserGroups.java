package beans;

import ldap.LdapTool;

import java.io.FileNotFoundException;
import java.util.SortedSet;

import javax.naming.NamingException;

public class LdapUserGroups {
	private SortedSet<String> userGroups;
	public LdapUserGroups(){
		
		
		LdapTool lt = null;
		try {
			lt = new LdapTool();
		} catch (FileNotFoundException fe){
			// TODO Auto-generated catch block
			fe.printStackTrace();					
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// TODO
		if( lt == null){
			
		}
		
		
		
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
