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
/*inform user that we are not supporting IE8 or older*/
if(  (navigator.appVersion.indexOf("MSIE 8.")!=-1 || navigator.appVersion.indexOf("MSIE 7")!=-1
		|| navigator.appVersion.indexOf("MSIE 6")!=-1 || navigator.appVersion.indexOf("MSIE 5")!=-1
		|| navigator.appVersion.indexOf("MSIE 4")!=-1)
		
		// check if it is not enterprise mode
		// (if spellcheck feature availbale means that it is enterprise mode of IE11. we are supporting IE11 enterprise mode)
		&&  !('spellcheck' in document.createElement('textarea')) ) {
	
	window.onload = function(){ document.write('Internet Explorer 8 is not supported. Please use another browser.') };
} else {
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
                <div align="center"><img src="css/images/logos/supporttracker.gif" alt="Support Tracker Logo"/></div>
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
<!-- Show a list of the groups that are stored in Ldap's Clients folder. each group is displayed with a link to OrganisationDetails.jsp -->
<%  String[] userGroups = groups.getUserGroups();
	List<String> userGroupsList = Arrays.asList(userGroups);
  for( int i = 0; i < userGroups.length; i++ ){  %>
                  <div class="row">
                    <a href="OrganisationDetails.jsp?name=<%=java.net.URLEncoder.encode(userGroups[i]) %>"><%=userGroups[i] %></a>
                  </div>
<%  } 
  List<String> orgs = SupportTrackerJDBC.getOrganisations();
  if (orgs.size()>userGroups.length) { %>
                <form id="addnew" method="post" action="AddOrganisation">
                  <br /><span id="addlabel"><b>Add Organisation:</b></span><br />
                  <select name="org" id="org">

<!-- make a drop down list of groups that are stored in SupportTrakcerJDBC, but not stored in Ldap's Clients folder -->
<% for (String organisation : orgs) {
	  if (!userGroupsList.contains(organisation.trim())) {
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
