<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"
    import="tools.SupportTrackerJDBC"
    import="java.util.List"
    import="java.util.Arrays"
    import="java.net.ConnectException"
    %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <title>Organisation Management</title>
  <link rel="stylesheet" href="./css/concerto.css" type="text/css" />
  <link rel="stylesheet" href="./css/general.css" type="text/css" />
  <!-- <link rel="stylesheet" href="./css/widget.css" type="text/css" />
  <link rel="stylesheet" href="./css/core.css" type="text/css" />
  <link rel="stylesheet" href="./css/inputtypes.css" type="text/css" />-->
  <jsp:useBean id="groups" class="beans.LdapUserGroups" scope="session" />
  
  <%	try {
	  		groups.refreshGetUserGroup();
  		} catch(ConnectException e){
  			session.setAttribute("error", e.getMessage());
  		}
  %>
  
  <script>
    window.onload = function () {
    	if (document.getElementById("addnew").org.length == 0) {
    		document.getElementById("addnew").removeChild(org);
    		document.getElementById("addnew").removeChild(addlabel);
    		document.getElementById("addnew").removeChild(addbutton);
    	}
    }
    
    function SubmitForm(){
    	document.getElementById("addnew").submit();
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
                <h1>Organisation Management</h1>
                <img src="css/images/swish.gif" alt="#" />
<%  if(session.getAttribute("error") != null){ %>
                <div class="error" style="float: center; width=100%; text-align: center">
                <%=session.getAttribute("error") %>
                </div>
<%  }else if(session.getAttribute("isAdmin") == null){ %>
                <div class="error" style="float: center; width=100%; text-align: center">Only support administrators can access this page.</div>
<%  }else{ %>
                <br />
                <div style="width: 600px; padding: 5px; margin: 5px auto ";>
<%  String[] userGroups = groups.getUserGroups();
  for( int i = 0; i < userGroups.length; i++ ){  %>
                  <div class="row">
                    <a href="OrganisationDetails.jsp?name=<%=userGroups[i] %>"><%=userGroups[i] %></a>
                  </div>
<%  } List<String> orgs = SupportTrackerJDBC.getOrganisations();
  if (orgs.size()>userGroups.length) { %>
                <form id="addnew" method="post" action="AddOrganisation">
                  <br /><span id="addlabel"><b>Add Organisation:</b></span><br />
                  <select name="org" id="org">
<% for (String organisation : orgs) {
	  if (!Arrays.asList(userGroups).contains(organisation)) {
		  %>
		  		    <option value="<%= organisation %>"><%= organisation %></option>
		  <%
	  }
  }
  %>
                  </select>
                  <a class="Button" href="#" id="addbutton" onclick="javascript: SubmitForm()">Add Organisation</a>
                </form>
<% } %>
                </div>
<%  }  %>
                <img src="css/images/swish.gif" alt="#" />
<% if( session.getAttribute("message") != null){ %>
                                <div align="center"><p> <%=session.getAttribute("message")%> </p></div>
                                <div align="center"><img src="./css/images/swish.gif" alt="#" /></div>
<% session.removeAttribute("message"); } %>
                <div align="center" class="disclaimer2">Having problems?<br/>Email <a href="mailto:support@orionhealth.com">Support@Orionhealth.com</a><br /></div>
              </td>
            </tr>
          </table>
        </td>
      </tr>
    </table>
  </body>
</html>
