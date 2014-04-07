<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Update Person: <%=request.getParameter("dn") %></title>
<jsp:useBean id="user" class="beans.LdapUser" scope="page" />
<jsp:useBean id="groups" class="beans.LdapUserGroups" scope="session" />
<jsp:useBean id="countries" class="beans.Countries" scope="session" />
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.TreeMap" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.net.ConnectException" %>

    <% 	try{
    		user.processUserDN(request.getParameter("dn"));
    		groups.refreshGetUserGroup();
    	} catch (ConnectException e) {
    		session.setAttribute("error", e.getMessage());
    	}
	%>


<script type='text/javascript'>
function validateEntries(){
	if(document.form.sAMAccountName.value == ""){
		alert("Please enter the username");
		document.form.sAMAccountName.focus();
		return false;
	}
	if(document.form.givenName.value == ""){
		alert("Please enter the first name");
		document.form.givenName.focus();
		return false;
	}
	if(document.form.sn.value == ""){
		alert("Please enter the last name");
		document.form.sn.focus();
		return false;
	}
	if(document.form.displayName.value == ""){
		alert("Please enter the display name");
		document.form.displayName.focus();
		return false;
	}
	if(document.form.company.value == ""){
		alert("Please select a company from the list");
		document.form.company.focus();
		return false;
	}
	if(document.form.mail.value == ""){
		alert("Please enter an email");
		document.form.mail.focus();
		return false;
	}
	if(document.form.password01.value != ""){
		if(document.form.password02.value == ""){
			alert("Please repeat your password");
			document.form.password02.focus();
			return false;
		}else if(document.form.password02.value != document.form.password01.value){
			alert("Passwords do not match");
			document.form.password01.value = "";
			document.form.password02.value = "";
			document.form.password01.focus();
			return false;
		}
	}
	return true;
}
function doDisplayName(){
	document.form.displayName.value = document.form.givenName.value+" "+document.form.sn.value; 
}
</script>
</head>
<body>
<font face="arial">
<h3><u>User Details</u></h3>
<form name="form" method="post" action="UpdateUserDetails?dn=<%=request.getParameter("dn") %>" onSubmit="return validateEntries();">
<table>
<tr>
<th align=left>Username</th>
<td><input type="text" name="sAMAccountName" value="<%=user.getUsername()%>"></td>
</tr>
<tr>
<th align=left>First Name</th>
<td><input type="text" name="givenName" onBlur="doDisplayName();" value="<%=user.getFirstName()%>"></td>
</tr>
<tr>
<th align=left>Last Name</th>
<td><input type="text" name="sn" onBlur="doDisplayName();" value="<%=user.getLastName()%>"></td>
</tr>
<tr>
<th align=left>Display Name</th>
<td><input readonly type="text" name="displayName" value="<%=user.getDisplayName()%>"></td>
</tr>
<tr>
<th align=left>Department</th>
<td><input type="text" name="department" value="<%=user.getDepartment()%>"></td>
</tr>
<tr>
<th align=left>Company</th>
<td><select name="company">
<option value="">-- Please select one from the list</option>
<%	String[] userGroups = groups.getUserGroups();
	for(int i = 0; i < userGroups.length; i++){
		String group = userGroups[i];
		if(group.equals(user.getCompany())){	%>
<option value="<%=group %>" selected="selected"><%=group %></option>
<%		}else{	%>
<option value="<%=group %>"><%=group %></option>
<%		}
	}	%>
</select></td>
</tr>
<tr>
<th align=left>Description</th>
<td><input type="text" name="description" value="<%=user.getDescription()%>"></td>
</tr>
<tr>
<th align=left>No./Street</th>
<td><input type="text" name="streetAddress" value="<%=user.getStreet()%>"></td>
</tr>
<tr>
<th align=left>City</th>
<td><input type="text" name="l" value="<%=user.getCity()%>"></td>
</tr>
<tr>
<th align=left>State</th>
<td><input type="text" name="st" value="<%=user.getState()%>"></td>
</tr>
<tr>
<th align=left>Postal Code</th>
<td><input type="text" name="postalCode" value="<%=user.getPostalCode()%>"></td>
</tr>
<tr>
<th align=left>Country</th>
<td><select name="c">
<option value="">Please select one from the list</option>
<%	TreeMap<String,String> countriesMap = countries.getCountries();
	for(Map.Entry<String, String>entry:countriesMap.entrySet()){
		String countryName = entry.getKey();
		String countryCode = entry.getValue();
		if(countryName.equals(user.getCountry())){	%>
<option value="<%=countryCode %>" selected="selected"><%=countryName %></option>
<%		}else{	%>
<option value="<%=countryCode %>"><%=countryName %></option>
<%		}
	}	%>
</select></td>
</tr>
<tr>
<th align=left>Phone Number</th>
<td><input type="text" name="telephoneNumber" value="<%=user.getPhoneNumber()%>"></td>
</tr>
<tr>
<th align=left>Email</th>
<td><input type="text" name="mail" value="<%=user.getEmail()%>"></td>
</tr>
<tr>
<th align=left>New Password:</th>
<td><input type="password" name="password01"></td>
</tr>
<tr>
<th align=left>Repeat Password:</th>
<td><input type="password" name="password02"></td>
</tr>
<tr>
<td><input type="submit" value="Update"></td>
<td><input type="button" value="Cancel"></td>
</tr>
</table>
</form>
</font>
</body>
</html>
