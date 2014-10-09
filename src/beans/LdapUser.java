package beans;

import java.io.FileNotFoundException;
import java.net.ConnectException;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import ldap.ErrorConstants;
import ldap.LdapTool;

import org.apache.log4j.Logger;

public class LdapUser {
	private String username, firstName, lastName, displayName, department;
	private String company, description, street, city, state, postalCode;
	private String country, phoneNumber, fax, mobile, email;
	private String[] memberOfGroups;
	private boolean accountDisabled;
	Logger logger = Logger.getRootLogger();
	
	public void processUserDN(String userDN) throws ConnectException{
		logger.debug("Query for the attributes of the user: " + userDN);
		
		
		// because this userDN is passed from browser. so, it has not been escaped the reserved char.
		// so, we need to escape those reserved chars
		//userDN = LdapTool.escapedCharsOnCompleteDN(userDN);
		
		
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
				logger.error(ErrorConstants.FAIL_QUERY_LDAP, ex);
				throw new ConnectException(ErrorConstants.FAIL_QUERY_LDAP);
			}
		}
		
		logger.debug("finished querying the user's attributes");
	}
	
	
	
	/**
	 * setter method
	 * @param accountDisabled
	 */
	public void setAccountDisabled(boolean accountDisabled){
		this.accountDisabled = accountDisabled;
	}
	/**
	 * getter method
	 */
	public boolean getAccountDisabled(){
		return this.accountDisabled;
	}
	
	
	
	/**
	 * setter method
	 * @throws NamingException 
	 */
	public void setMemberOfGroups(Attribute memberOfGroups) throws NamingException {
		if(memberOfGroups == null) {
			this.memberOfGroups = new String[0];
		} else {
			this.memberOfGroups = new String[memberOfGroups.size()];
			try {
				for(int i = 0; i < memberOfGroups.size(); i++){
					this.memberOfGroups[i] = memberOfGroups.get(i).toString();
				}
			} catch (NamingException e) {
				logger.error(ErrorConstants.FAIL_UPDATE_LDAP, e);
				throw new NamingException(ErrorConstants.FAIL_UPDATE_LDAP);
			}
		}
	}
	/**
	 * getter method
	 */
	public String[] getMemberOfGroups(){
		return this.memberOfGroups;
	}
	
	
	
	/**
	 * setter method
	 */
	public void setEmail(String email) {
		this.email = email;
	}
	/**
	 * getter method
	 */
	public String getEmail(){
		return this.email;
	}
	
	
	
	/**
	 * setter method
	 */
	public void setMobile(String mobile) {
		this.mobile = mobile;
	}
	/**
	 * getter method
	 */
	public String getMobile(){
		return this.mobile;
	}
	
	
	
	/**
	 * setter method
	 */
	public void setFax(String fax) {
		this.fax = fax;
	}
	/**
	 * getter method
	 */
	public String getFax(){
		return this.fax;
	}
	
	
	
	/**
	 * setter method
	 */
	public void setphoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}
	/**
	 * getter method
	 */
	public String getPhoneNumber(){
		return this.phoneNumber;
	}
	
	
	
	/**
	 * setter method
	 */
	public void setCountry(String country) {
		this.country = country;
	}
	/**
	 * getter method
	 */
	public String getCountry(){
		return this.country;
	}
	
	
	
	/**
	 * setter method
	 */
	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}
	/**
	 * getter method
	 */
	public String getPostalCode(){
		return this.postalCode;
	}
	
	
	
	/**
	 * setter method
	 */
	public void setState(String state) {
		this.state = state;
	}
	/**
	 * getter method
	 */
	public String getState(){
		return this.state;
	}
	
	
	
	/**
	 * setter method
	 */
	public void setCity(String city) {
		this.city = city;
	}
	/**
	 * getter method
	 */
	public String getCity(){
		return this.city;
	}
		
	
	
	/**
	 * Setter method
	 * @param street
	 */
	public void setStreet(String street) {
		this.street = street;
	}
	/**
	 * getter method
	 */
	public String getStreet(){
		return this.street;
	}
	
	
	
	/**
	 * setter method
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	/**
	 * getter method
	 */
	public String getDescription(){
		return this.description;
	}
	
	
	
	/**
	 * setter method
	 */
	public void setCompany(String company) {
		this.company = company;
	}
	/**
	 * getter method
	 */
	public String getCompany(){
		return this.company;
	}
	/**
	 * setter method
	 */
	public void setDepartment(String department) {
		this.department = department;
	}
	/**
	 * getter method
	 */
	public String getDepartment(){
		return this.department;
	}
	
	
	
	/**
	 * setter method
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	/**
	 * getter method
	 */
	public String getDisplayName(){
		return this.displayName;
	}
	
	
	
	/**
	 * setter method
	 */
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	/**
	 * getter method
	 */
	public String getLastName(){
		return this.lastName;
	}
	
	
	
	/**
	 * setter method
	 */
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	/**
	 * getter method
	 */
	public String getFirstName(){
		return this.firstName;
	}
	
	
	
	/**
	 * setter method
	 */
	public void setUsername(String username){
		this.username = username;
	}
	/**
	 * getter method
	 */
	public String getUsername(){
		return this.username;
	}
	
	
	
	/**
	 * create and return a String contains the HTML to presents user detail
	 */
	public String returnHtml(){
		logger.debug("about to create html string that contains attributes  of user: " + getUsername());
		
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
		
		logger.debug("finished creating html string that contains attributes  of user: " + getUsername());
		
		return output.toString();
	}
}