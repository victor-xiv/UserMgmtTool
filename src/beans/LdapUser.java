package beans;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.apache.log4j.Logger;

import ldap.LdapTool;

public class LdapUser {
	private String username, firstName, lastName, displayName, department;
	private String company, description, street, city, state, postalCode;
	private String country, phoneNumber, fax, mobile, email;
	private String[] memberOfGroups;
	private boolean accountDisabled;
	Logger logger = Logger.getRootLogger();
	
	public void processUserDN(String userDN){
		LdapTool lt = new LdapTool();
		Attributes attrs = lt.getUserAttributes(userDN);
		lt.close();
		try{
			setUsername(attrs.get("sAMAccountName")!=null?attrs.get("sAMAccountName").get().toString():"");
			setFirstName(attrs.get("givenName")!=null?attrs.get("givenName").get().toString():"");
			setLastName(attrs.get("sn")!=null?attrs.get("sn").get().toString():"");
			setDisplayName(attrs.get("displayName")!=null?attrs.get("displayName").get().toString():"");
			setDepartment(attrs.get("department")!=null?attrs.get("department").get().toString():"");
			setCompany(attrs.get("company")!=null?attrs.get("company").get().toString():"");
			setDescription(attrs.get("description")!=null?attrs.get("description").get().toString():"");
			setStreet(attrs.get("streetAddress")!=null?attrs.get("streetAddress").get().toString():"");
			setCity(attrs.get("l")!=null?attrs.get("l").get().toString():"");
			setState(attrs.get("st")!=null?attrs.get("st").get().toString():"");
			setPostalCode(attrs.get("postalCode")!=null?attrs.get("postalCode").get().toString():"");
			setCountry(attrs.get("c")!=null?attrs.get("c").get().toString():"");
			setphoneNumber(attrs.get("telephoneNumber")!=null?attrs.get("telephoneNumber").get().toString():"");
			setFax(attrs.get("facsimileTelephoneNumber")!=null?attrs.get("facsimileTelephoneNumber").get().toString():"");
			setMobile(attrs.get("mobile")!=null?attrs.get("mobile").get().toString():"");
			setEmail(attrs.get("mail")!=null?attrs.get("mail").get().toString():"");
			setMemberOfGroups(attrs.get("memberOf")!=null?attrs.get("memberOf"):null);
			int userAccountControl = Integer.parseInt(attrs.get("userAccountControl").get().toString());
			accountDisabled = (userAccountControl & 2) > 0 ;
		}catch(NamingException ex){
			logger.error(ex.toString());
			ex.printStackTrace();
		}
	}
	public void setAccountDisabled(boolean accountDisabled){
		this.accountDisabled = accountDisabled;
	}
	public boolean getAccountDisabled(){
		return this.accountDisabled;
	}
	public void setMemberOfGroups(Attribute memberOfGroups) {
		if(memberOfGroups == null)
			this.memberOfGroups = new String[0];
		else
			this.memberOfGroups = new String[memberOfGroups.size()];
		try {
			for(int i = 0; i < memberOfGroups.size(); i++){
				this.memberOfGroups[i] = memberOfGroups.get(i).toString();
			}
		} catch (NamingException e) {
			logger.error(e.toString());
			e.printStackTrace();
		}
	}
	public String[] getMemberOfGroups(){
		return this.memberOfGroups;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getEmail(){
		return this.email;
	}
	public void setMobile(String mobile) {
		this.mobile = mobile;
	}
	public String getMobile(){
		return this.mobile;
	}
	public void setFax(String fax) {
		this.fax = fax;
	}
	public String getFax(){
		return this.fax;
	}
	public void setphoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}
	public String getPhoneNumber(){
		return this.phoneNumber;
	}
	public void setCountry(String country) {
		this.country = country;
	}
	public String getCountry(){
		return this.country;
	}
	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}
	public String getPostalCode(){
		return this.postalCode;
	}
	public void setState(String state) {
		this.state = state;
	}
	public String getState(){
		return this.state;
	}
	public void setCity(String city) {
		this.city = city;
	}
	public String getCity(){
		return this.city;
	}
	public void setStreet(String street) {
		this.street = street;
	}
	public String getStreet(){
		return this.street;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getDescription(){
		return this.description;
	}
	public void setCompany(String company) {
		this.company = company;
	}
	public String getCompany(){
		return this.company;
	}
	public void setDepartment(String department) {
		this.department = department;
	}
	public String getDepartment(){
		return this.department;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	public String getDisplayName(){
		return this.displayName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	public String getLastName(){
		return this.lastName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	public String getFirstName(){
		return this.firstName;
	}
	public void setUsername(String username){
		this.username = username;
	}
	public String getUsername(){
		return this.username;
	}
	public String returnHtml(){
		StringBuffer output = new StringBuffer();
		output.append("<tr><th align=\"left\">Username</th><td>"+getUsername()+"</td></tr>\n");
		output.append("<tr><th align=\"left\">First Name</th><td>"+getFirstName()+"</td></tr>\n");
		output.append("<tr><th align=\"left\">Last Name</th><td>"+getLastName()+"</td></tr>\n");
		output.append("<tr><th align=\"left\">Display Name</th><td>"+getDisplayName()+"</td></tr>\n");
		output.append("<tr><th align=\"left\">Department</th><td>"+getDepartment()+"</td></tr>\n");
		output.append("<tr><th align=\"left\">Company</th><td>"+getCompany()+"</td></tr>\n");
		output.append("<tr><th align=\"left\">Description</th><td>"+getDescription()+"</td></tr>\n");
		output.append("<tr><th align=\"left\">Street</th><td>"+getStreet()+"</td></tr>\n");
		output.append("<tr><th align=\"left\">City</th><td>"+getCity()+"</td></tr>\n");
		output.append("<tr><th align=\"left\">State</th><td>"+getState()+"</td></tr>\n");
		output.append("<tr><th align=\"left\">Postal Code</th><td>"+getPostalCode()+"</td></tr>\n");
		output.append("<tr><th align=\"left\">Country</th><td>"+getCountry()+"</td></tr>\n");
		output.append("<tr><th align=\"left\">Phone Number</th><td>"+getPhoneNumber()+"</td></tr>\n");
		output.append("<tr><th align=\"left\">Email</th><td>"+getEmail()+"</td></tr>\n");
		for(int i = 0; i < memberOfGroups.length; i++ ){
			output.append("<tr><th align=\"left\">Member of Group</th><td>"+memberOfGroups[i]+"</td></tr>\n");
		}
		return output.toString();
	}
}