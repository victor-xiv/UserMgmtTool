package beans;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import tools.ConcertoJDBC;
import tools.SupportTrackerJDBC;

public class UserDetails {
	private String username, firstName, lastName, displayName, department;
	private String company, description, street, city, state, postalCode;
	private String countryCode, phoneNumber, fax, mobile, email, clientId;
	
	
	/**
	 * process and get the properties of the user (given username) from the Support Tracker DB 
	 * The difference between this bean and the LdapUser bean is that :
	 * * this bean is getting the user's properties from Support Tracker DB.
	 * * LdapUser bean is getting the user's properties from Ldap Server
	 * @param username : is the username used to login to support tracker or ldap server
	 */
	public void processUsername(String username){
		Logger logger = Logger.getRootLogger();
		logger.debug("Querying the details of user " + username + " from Support Tracker DB and Portal DB");
		
		Map<String, String> userDetails = new HashMap<String, String>();
		
		// becuase both Concerto account and Support Tracker account are storing the same information, but some times both information have different values
		// and we give priority to Support Tracker information.
		// so, if there are different values, we will use the values stored in Support Tracker
		// thats why we pull out the information from Concerto and put into userDetails
		// then pull out information from Support Tracker and put into userDetails, if there are the same information, 
		// the values from Support Tracker will replace the ones from Concerto
		
		try {
			userDetails.putAll(ConcertoJDBC.getUserDetails(username));
		} catch (SQLException e) {
			// dt need to do anything
		}
		
		try {
			userDetails.putAll(SupportTrackerJDBC.getUserDetails(username, userDetails.get("info")));
		} catch (SQLException e) {
			// dt need to do anything
		}
		
		setUsername(username);

		setDisplayName(userDetails.get("displayName")!=null?userDetails.get("displayName"):"");
		setDepartment(userDetails.get("department")!=null?userDetails.get("department"):"");
		setCompany(userDetails.get("company")!=null?userDetails.get("company"):"");
		setDescription(userDetails.get("description")!=null?userDetails.get("description"):"");
		setStreet(userDetails.get("streetAddress")!=null?userDetails.get("streetAddress"):"");
		setCountryCode(userDetails.get("c")!=null?userDetails.get("c"):"");
		setphoneNumber(userDetails.get("telephoneNumber")!=null?userDetails.get("telephoneNumber"):"");
		setFax(userDetails.get("facsimileTelephoneNumber")!=null?userDetails.get("facsimileTelephoneNumber"):"");
		setMobile(userDetails.get("mobile")!=null?userDetails.get("mobile"):"");
		setEmail(userDetails.get("mail")!=null?userDetails.get("mail"):"");
		setClientId(userDetails.get("info")!=null?userDetails.get("info"):"");
		
		setFirstName(userDetails.get("givenName")!=null?userDetails.get("givenName"):"");
		setLastName(userDetails.get("sn")!=null?userDetails.get("sn"):"");
		setCity(userDetails.get("l")!=null?userDetails.get("l"):"");
		setState(userDetails.get("st")!=null?userDetails.get("st"):"");
		setPostalCode(userDetails.get("postalCode")!=null?userDetails.get("postalCode"):"");
		
		logger.debug("Finished querying the details of user " + username + " from Support Tracker DB and Portal DB");
	}
	public void setClientId(String clientId){
		this.clientId = clientId;
	}
	public String getClientId(){
		return this.clientId;
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
	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}
	public String getCountryCode(){
		return this.countryCode;
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
}