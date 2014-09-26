<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"
    import="java.util.Properties" 
    import="ldap.LdapProperty"
    import="ldap.UserMgmtConstants" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<% 
String conserver = LdapProperty.getProperty(UserMgmtConstants.CONCERTO_URL);//"supporttracker.orionhealth.com"; %>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Enter Username</title>
    <link type="text/css" rel="stylesheet" href="<%= conserver %>/common/css/core.css">
    <link rel="stylesheet" type="text/css" href="<%= conserver %>/common/css/widget.css">
	<link rel="stylesheet" type="text/css" href="<%= conserver %>/css/inputtypes.css">
	<link rel="stylesheet" type="text/css" href="<%= conserver %>/css/concerto.css">
	<script src="<%= conserver %>/javascript/Concerto.js" type="text/javascript"></script>
	<script src="<%= conserver %>/javascript/Dialog.js" type="text/javascript"></script>
</head>
<body>
	<h1>Request new password</h1>
	<form action="CheckType" method="post">
		<table>
			<tr>
				<td>Username:</td>
				<td><input type="text" name="user" id="user" /></td>
			</tr>
			<tr>
				<td>Organisation: </td>
				<td><input type="text" name="org" id="org" /></td>
			</tr>
			<tr>
				<td></td>
				<td><button type="submit">Submit</button></td>
			</tr>
		</table>
	</form>
<% if (session.getAttribute("message") != null) { %>
	<br /><%= session.getAttribute("message") %>
<% session.removeAttribute("message"); } %>
</body>
</html>