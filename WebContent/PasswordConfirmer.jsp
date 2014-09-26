<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    import="com.concerto.sdk.security.ValidatedRequest"
    import="com.concerto.sdk.security.InvalidRequestException"
    pageEncoding="ISO-8859-1"
    import="java.util.Properties" 
    import="ldap.LdapProperty"
    import="ldap.EmailConstants" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<% 
String conserver = LdapProperty.getProperty(EmailConstants.MAIL_HOST);//"supporttracker.orionhealth.com"; %>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Enter New Password</title>
    <link type="text/css" rel="stylesheet" href="<%= conserver %>/common/css/core.css">
    <link rel="stylesheet" type="text/css" href="<%= conserver %>/common/css/widget.css">
	<link rel="stylesheet" type="text/css" href="<%= conserver %>/css/inputtypes.css">
	<link rel="stylesheet" type="text/css" href="<%= conserver %>/css/concerto.css">
	<script src="<%= conserver %>/javascript/Concerto.js" type="text/javascript"></script>
	<script src="<%= conserver %>/javascript/Dialog.js" type="text/javascript"></script>
<script language="javascript">
	function vp(form) {
		if (form.password.value == form.confirm.value)
			return true;
		alert("Cannot submit: Passwords do not match ");
		return false;
	}
</script>
</head>
<body>
<% ValidatedRequest req;
String errtype = "";
//Ensure that key is the same as in CheckTypeServlet
String key = "8290A7F6A2DD5A79C32CC0EF2F9D9280";
//Decrypt message
try {
	req = new ValidatedRequest(request, key);
	//Check that variable 'user' has been set
	if (req.getParameter("user")!=null) { %>
	<form action="ForgotPassword" method="post" id="formData" onsubmit="return vp(this);">
		<table>
			<tr>
				<td>Password: </td>
				<td><input type="password" id="password" name="password" /></td>
			</tr>
			<tr>
				<td>Confirm Password: </td>
				<td><input type="password" id="confirm" name="confirm" /></td>
			</tr>
			<tr>
				<td><input type="hidden" id="user" name="user" value="<%= req.getParameter("user") %>" /></td>
				<td><button type="submit" >Request Password</button></td>
			</tr>
		</table>
	</form>
	<% } else 
	{ errtype = "Invalid Request! To receive a new password, please follow the link from the email."; } 
 } catch (InvalidRequestException e) {
		//If request is invalid (e.g. made from web browser not Concerto)
		//error and quit
		errtype = "Invalid Request! To receive a new password, please follow the link from the email.<br />";
		errtype += "If you followed this link from the email, your password change request may have expired.";
		e.printStackTrace();
} %>
<%= errtype %>

</body>
</html>