<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <title>Organisation: <%=request.getParameter("name") %></title>
  <link rel="stylesheet" href="./css/concerto.css" type="text/css" />
  <link rel="stylesheet" href="./css/general.css" type="text/css" />
  <jsp:useBean id="org" class="beans.LdapOrganisation" scope="page" />
  <%@ page import="java.util.Enumeration" %>
  <%@ page import="java.util.TreeMap" %>
  <%@ page import="java.util.Set" %>
  <%@ page import="ldap.LdapTool" %>
  <%@ page import="javax.naming.directory.Attribute" %>
  <%@ page import="javax.naming.directory.Attributes" %>
  <%@ page import="javax.naming.NamingEnumeration" %>
  <%	org.processOrganisationName(request.getParameter("name"));
  		TreeMap<String,String[]> users = org.getUsers();	%>
  <script>
  function SubmitForm(){
  	document.getElementById("create").submit();
	    return true;
  	}
  function SubmitGroupForm(){
	  	document.getElementById("addto").submit();
		    return true;
	  	}
  </script>
</head>
  <body>
    <table align="center" border="0" style="border-color: #ef7224" cellspacing="1">
      <tr>
        <td bgcolor="#ef7224">
          <table bgcolor="#ffffff">
            <tr align="center">
              <td align="center">
                <div align="center"><img src="http://supporttracker.orionhealth.com/concerto/images/logos/supporttracker.gif" alt="#"/></div>
                <h1><%=request.getParameter("name") %></h1>
                <img src="css/images/swish.gif" alt="#" />
<%	if(session.getAttribute("error") != null){ %>
                <div class="error" style="float: center; width=100%; text-align: center"><%=session.getAttribute("error") %></div>
<%	/*Need to remove error*/session.removeAttribute("error"); }else if(session.getAttribute("isAdmin") == null){ %>
                <div class="error" style="float: center; width=100%; text-align: center">Only support administrators can access this page.</div>
<%	}else{ %>
                <br />
                <div style="width: 600px; padding: 5px; margin: 5px auto ";>
                
                <div class="row">
                  <div style="float: left">
                    <h2 style="text-align: left; padding-left: 20px;">Users</h2>
                  </div>
                  <div style="float: right">
                    <a class="Button" href="AddNewUser.jsp?company=<%= request.getParameter("name") %>">Add New</a>
                  </div>
                </div>
                
<%  String[] keySet = org.getUsersKeys();
    for( int i = 0; i < keySet.length; i++ ){
        String userCn = keySet[i];
        String userDn = users.get(userCn)[0];
		boolean accountDisabled = users.get(userCn)[1].equals("disabled");%>
                  <div class="row">
                    <span style="float: inherit; width: 200px; text-align: center;">
<%	if(accountDisabled){	%>
                      <a href="UserDetails.jsp?dn=<%=userDn %>" style="font-style: italic; color: #808080;"><%=userCn %> (disabled)</a>
<%	}else{	%>
                      <a href="UserDetails.jsp?dn=<%=userDn %>"><%=userCn %></a>
<%	}	%>
                    </span>
                  </div>
<%	} %>		
                </div>
                <img src="css/images/swish.gif" alt="#" />
                <div style="width: 600px; padding: 5px; margin: 5px auto ";>
                  <h2 style="text-align: left; padding-left: 20px;">Organisation Details</h2>
                  <div class="row">
                    <span style="float: left; text-align: right; width:200px;">Organisation Name:</span>
                    <span style="float: right; text-align: left; width:395px;"><%=org.getName() %></span>
                  </div>
                  <div class="row">
                    <span style="float: left; text-align: right; width:200px;">Group DN:</span>
                    <span style="float: right; text-align: left; width:395px;"><%=org.getDistinguishedName() %></span>
                  </div>
<% LdapTool lt = new LdapTool(); if (lt.companyExistsAsGroup(request.getParameter("name"))) { %>
                  <div class = "row">
                    <span style="float: left; text-align: right; width:200px;">Member of:</span>
                    <div style="float: right; text-align: left; width:395px;"><ul>
<% Attributes attrs = lt.getGroupAttributes(request.getParameter("name"));
Attribute attr = attrs.get("memberOf");
Set<String> groups = lt.getBaseGroups();
if (attr != null) {
	NamingEnumeration e = attr.getAll(); 
	while (e.hasMore()) {
		String dn = (String)e.next();
		String name = dn.split(",")[0].split("=")[1];
		groups.remove(name);%>
                    <li title='<%= dn %>'><%= name %></li>
	<% } } else { %><li>No groups</li><% } %>
                    </ul></div>
                  </div>
    <% if (groups.size()>0) { %>
                <form id="addto" method="post" action="AddGroup">
                  <!-- <br /><span id="addlabel"><b>Add to Group:</b></span><br /> -->
                  <input type="hidden" id="name" name="name" value="<%= request.getParameter("name") %>" />
                  <select name="groupselect" id="groupselect">
<% for (String group : groups) {
		  %>
		  		    <option value="<%= group %>"><%= group %></option>
		  <% } %>
                  </select>
                  <a class="Button" href="#" id="addbutton" onclick="javascript: SubmitGroupForm()">Add Organisation to Group</a>
                </form>
<% } } else { %>
                  <div class = "row">
                    <form method="post" id="create" action="CreateGroup">
                    <input type="hidden" id="name" name="name" value="<%= request.getParameter("name") %>" />
                    <span style="float: left; text-align: right; width:200px;"></span>
                    <span style="float: right; text-align: left; width:395px;">
                      <a class="Button" href="#" id="addbutton" onclick="javascript: SubmitForm()">Create Organisational Group</a>
                    </span>
                    </form>
                  </div>
<% } lt.close(); }%>
                </div>
                <img src="css/images/swish.gif" alt="#" />
                <div align="center" class="disclaimer2">Having problems?<br/>Email <a href="mailto:support@orionhealth.com">Support@Orionhealth.com</a><br /></div>
              </td>
            </tr>
          </table>
        </td>
      </tr>
    </table>
  </body>
</html>
