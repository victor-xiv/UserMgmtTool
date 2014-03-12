package beans;

import java.sql.SQLException;
import java.util.Map;

import tools.ConcertoJDBC;
import tools.SupportTrackerJDBC;

public class UserDetails {
	private String username, firstName, lastName, displayName, department;
	private String company, description, street, city, state, postalCode;
	private String countryCode, phoneNumber, fax, mobile, email, clientId;
	
	public void processUsername(String username){
		Map<String, String> userDetails = null;
		try {
			userDetails = SupportTrackerJDBC.getUserDetails(username);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(userDetails == null){
			// TODO
		}
		
		
		userDetails.putAll(ConcertoJDBC.getUserDetails(username));
		setUsername(username);
		setFirstName(userDetails.get("givenName")!=null?userDetails.get("givenName"):"");
		setLastName(userDetails.get("sn")!=null?userDetails.get("sn"):"");
		setDisplayName(userDetails.get("displayName")!=null?userDetails.get("displayName"):"");
		setDepartment(userDetails.get("department")!=null?userDetails.get("department"):"");
		setCompany(userDetails.get("company")!=null?userDetails.get("company"):"");
		setDescription(userDetails.get("description")!=null?userDetails.get("description"):"");
		setStreet(userDetails.get("streetAddress")!=null?userDetails.get("streetAddress"):"");
		setCity(userDetails.get("l")!=null?userDetails.get("l"):"");
		setState(userDetails.get("st")!=null?userDetails.get("st"):"");
		setPostalCode(userDetails.get("postalCode")!=null?userDetails.get("postalCode"):"");
		setCountryCode(userDetails.get("c")!=null?userDetails.get("c"):"");
		setphoneNumber(userDetails.get("telephoneNumber")!=null?userDetails.get("telephoneNumber"):"");
		setFax(userDetails.get("facsimileTelephoneNumber")!=null?userDetails.get("facsimileTelephoneNumber"):"");
		setMobile(userDetails.get("mobile")!=null?userDetails.get("mobile"):"");
		setEmail(userDetails.get("mail")!=null?userDetails.get("mail"):"");
		setClientId(userDetails.get("info")!=null?userDetails.get("info"):"");
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