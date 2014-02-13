<?xml version="1.0" encoding="ISO-8859-1" ?>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
<title>Test</title>
<jsp:useBean id="accounts" class="beans.AccountRequestsBean" scope="session" />
</head>
<body>
	This is the test page.
  <div align="center"><br> 
<%  String[] names = accounts.getAvailableNames("Simon", "Brown");
    for( int i = 0; i < names.length; i++ ){  %>
<input type="radio" name="username" value="<%=names[i] %>"><%=names[i] %><br>
<%  }  %>
  </div>
</body>
</html>