<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%@page import="java.net.URLEncoder"%>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <title>Organisation: <%=request.getParameter("name") %></title>
  <script type="text/javascript" language="javascript" src="./js/ajaxgen.js"></script>
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
  <%@ page import="java.io.FileNotFoundException" %>
  <%@ page import="javax.naming.NamingException" %>
  <%@ page import="java.net.ConnectException" %>
  <%@ page import="javax.naming.ldap.Rdn" %>
  <%@ page import="java.util.List" %>
  <%@ page import="servlets.AdminServlet" %>
  <%	
  
  	TreeMap<String,String[]> users = null;
  	try{
		org.processOrganisationName(request.getParameter("name"));
		users = org.getUsers();
  	} catch (ConnectException e){
  		session.setAttribute("error", e.getMessage());
  	}
  	
  	LdapTool lt = null;
	try {
		lt = new LdapTool();
	} catch (FileNotFoundException fe){
		session.setAttribute("error", fe.getMessage());					
	} catch (NamingException e) {
		session.setAttribute("error", e.getMessage());
	}
	
  %>
<script>
	Element.prototype.remove = function(){
		this.parentElement.removeChild(this);
	}
	
	NodeList.prototype.remove = HTMLCollection.prototype.remove = function(){
		var len=this.length; 
		for(var i=0; i<len; i++){
			if(this[i] && this[i].parentElement){
				this[i].parentElement.removeChild(this[i]);
			}
		}
	}
	
	function cleanUpThePage(){
    	var idsNeededToBeCleanedUp = new Array("add-removeGroupFailed", "add-removeGroupPassed");
    	for(var i=0; i<idsNeededToBeCleanedUp.length; i++){
    		document.getElementById(idsNeededToBeCleanedUp[i]).innerHTML = "";
    	}
    
    }
	function SubmitForm() {
		cleanUpThePage();
		document.getElementById("create").submit();
		return true;
	}
	function SubmitGroupForm() {
		cleanUpThePage();
		// send a POST request to AddGroupServlet with parameters: name (dn-name of this group) and groupselect (dn-name of selected group)
    	var thisGroupDN = document.getElementById("thisGroupDN").value;
    	var newGroupName = document.getElementById("groupselect").value;
    	ajax.open("POST", "AddGroup", true);
    	ajax.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
        ajax.setRequestHeader("Accept", "text/xml, application/xml, text/plain");
        var params = "thisGroupDN=" + encodeURIComponent(thisGroupDN) + "&newGroupName=" + encodeURIComponent(newGroupName);
        ajax.send(params);
    	
        // handling ajax state
        ajax.onreadystatechange = function(){
        	
        	// handling once request responded
        	if(ajax.readyState == 4){
        		// if reponse is 200
        		if(ajax.status == 200){
		        	var xmlDoc = ajax.responseXML;
		        	
		        	// try to check the reponse that contain a tage "failed" first
		        	// if response contains "failed" tag, mean the adding a group process is failed
		        	var failed = xmlDoc.getElementsByTagName("failed")[0];
	
		        	// if failed tag is not defined or null means the process is successful
		        	if(failed == undefined || failed == null){
		        		
		        		// update the memberOf list
		        		var memberOfList = xmlDoc.getElementsByTagName("memberOf");
		        		var value = "<table>";
		        		for(var i=0; i<memberOfList.length; i++){
		        			var dnValue = memberOfList[i].firstElementChild.firstChild.nodeValue;
		        			var nameValue = memberOfList[i].lastElementChild.firstChild.nodeValue;
		        			value += "<tr id='" +dnValue+ "'> <td><a class='Delete' onclick=\"deleteGroup('"+dnValue+"')\" href='#' title='Delete'></a></td> <td>"+nameValue+"</td> </tr>";
		        		}
		        		value += "</table>";
		        		document.getElementById("memberOf").innerHTML = value;	              
		        		
		        		// update the drop down menu which is listing all the groups that this user is not bellonging to
		        		value = "";
		        		var nonMemberOfList = xmlDoc.getElementsByTagName("notMemberOf");
		        		for(var i=0; i<nonMemberOfList.length; i++){
		        			var nameValue = nonMemberOfList[i].firstChild.nodeValue;
		        			value += "<option value='" + nameValue + "'>" + nameValue + "</option>";
		        		}
		        		document.getElementById("groupselect").innerHTML = value;
		        		
		        		var passed = xmlDoc.getElementsByTagName("passed")[0].firstChild.nodeValue;
		        		document.getElementById("add-removeGroupPassed").innerHTML = "<font color=\"green\"><b>" + passed +"</b></font>";
		        		return true;
		        	} else {
		        		// xml reponse contains "failed" tage:
		        		// set the add-removeGroupFailed element
		        		var reason = "<font color=\"red\"><b>" + failed.firstChild.nodeValue +"</b></font>";
						document.getElementById("add-removeGroupFailed").innerHTML = reason;
						return false;
		        	}
        		} else {
            		// if reponse is not 200
            		// set the add-removeGroupFailed element
            		var reason = "<font color=\"red\"><b>Addition of group '" + thisGroupDN 
    															+ "' to group " + newGroupName + " has failed.</b>"
    												  "<b> Server is not responding to the request. </b>"
    													+"</font>";
    				document.getElementById("add-removeGroupFailed").innerHTML = reason;
    				return false;
        		}
        	}
        }
	}
	
	// remove a group "removedGroupDN" (appeared on the page) from given "fromGroupDN"
	function deleteGroup(fromGroupDN){
		cleanUpThePage();
		
    	var removedGroupDN = document.getElementById("thisGroupDN").value;
    	ajax.open("POST", "RemoveAGroupFromGroup", true);
    	ajax.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
        ajax.setRequestHeader("Accept", "text/xml, application/xml, text/plain");
        var params = "removedGroupDN=" + encodeURIComponent(removedGroupDN) + "&fromGroupDN=" + encodeURIComponent(fromGroupDN);
        
        ajax.send(params);  
        
     	// handling ajax state
        ajax.onreadystatechange = function(){
        	
        	// handling once request responded
        	if(ajax.readyState == 4){
        		// if reponse is 200
        		if(ajax.status == 200){
        			var xmlDoc = ajax.responseXML;
            		
        			// try to check the reponse that contain a tage "failed"
		        	// if response contains "failed" tag, mean the adding a group process is failed
		        	var failed = xmlDoc.getElementsByTagName("failed")[0];
        			
		        	// if failed tag is not defined or null means the process is successful
		        	if(failed == undefined || failed == null){
		        		// update the memberOf list
		        		var memberOfList = xmlDoc.getElementsByTagName("memberOf");
		        		var value = "<table>";
		        		if(memberOfList.length > 0){
			        		for(var i=0; i<memberOfList.length; i++){
			        			var dnValue = memberOfList[i].firstElementChild.firstChild.nodeValue;
			        			var nameValue = memberOfList[i].lastElementChild.firstChild.nodeValue;
			        			value += "<tr id='" +dnValue+ "'> <td><a class='Delete' onclick=\"deleteGroup('"+dnValue+"')\" href='#' title='Delete'></a></td> <td>"+nameValue+"</td> </tr>";
			        		}
		        		} else { // there's no group in the list memberOf
		        			value += "<tr><td> No groups </td></tr>";
		        		}
		        		value += "</table>";
		        		document.getElementById("memberOf").innerHTML = value;	              
		        		
		        		// update the drop down menu which is listing all the groups that this group is not a memberOf
		        		value = "";
		        		var nonMemberOfList = xmlDoc.getElementsByTagName("notMemberOf");
		        		for(var i=0; i<nonMemberOfList.length; i++){
		        			var nameValue = nonMemberOfList[i].firstChild.nodeValue;
		        			value += "<option value='" + nameValue + "'>" + nameValue + "</option>";
		        		}
		        		document.getElementById("groupselect").innerHTML = value;
		        		
		        		var passed = "This group has been deleted from group: " + fromGroupDN + " successfully.";
            			document.getElementById("add-removeGroupPassed").innerHTML = "<font color=\"green\"><b>" + passed +"</b></font>";
		        	} else {
		        		var failedMessage = "<font color=\"red\"><b>Deletion of this group "
														+ "from group " + fromGroupDN + " has failed.</b>"
											+ "<b>"+ failed.firstChild.nodeValue +"</b></font>";
							
						document.getElementById("add-removeGroupFailed").innerHTML = failedMessage;
		        	}
            		
        			return;
        		}
        		
        		// if reponse is not 200 or xml reponse contains "failed" tage:
        		// set the add-removeGroupFailed element
        		var failedMessage = "<font color=\"red\">Server failed to response.</font>";
				document.getElementById("add-removeGroupFailed").innerHTML = failedMessage;
        	}
        	
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
                <div align="center"><img src="http://supporttracker.orionhealth.com/concerto/images/logos/supporttracker.gif" alt="#"/></div>
                <h1><%=request.getParameter("name") %></h1>
				<input id="thisGroupDN" type="hidden" value="<%=lt.getDNFromGroup(request.getParameter("name"))%>"/> <!-- this hidden input used to get complete groupDN for other processes -->
                <img src="css/images/swish.gif" alt="#" />
                
<%if(session.getAttribute("pass") != null){ %>
                	<div class="pass" style="float: center; width=100%; text-align: center">
	<%=session.getAttribute("pass") %>
                	</div>
	<%session.removeAttribute("pass");/*Need to remove pass*/%> 
					<div class="row"><div style="float: right;"><div> <a class="Button" href="OrganisationDetails.jsp?name=<%=request.getParameter("name") %>"> Back </a> </div></div></div>

<%} else if(session.getAttribute("error") != null){ %>
                	<div class="error" style="float: center; width=100%; text-align: center">
	<%=session.getAttribute("error") %>
                	</div>
	<%session.removeAttribute("error");/*Need to remove error*/%> 
					<div class="row"><div style="float: right;"><div> <a class="Button" href="OrganisationDetails.jsp?name=<%=request.getParameter("name") %>"> Back </a> </div></div></div>

<%} else if(session.getAttribute("isAdmin") == null){ %>
                	<div class="error" style="float: center; width=100%; text-align: center">Only support administrators can access this page.</div>
<%} else { %>
				
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
                
	<%
	String[] keySet = org.getUsersKeys();
	for( int i = 0; i < keySet.length; i++ ){
		String userCn = keySet[i];
	    String userDn = users.get(userCn)[0];
	    userDn = java.net.URLEncoder.encode(userDn);
		boolean accountDisabled = users.get(userCn)[1].equals("disabled"); %>
                    <div class="row">
                    	<span style="float: inherit; width: 200px; text-align: center;">
                    
		<%if(accountDisabled){	%>
                    		<a href="t?dn=<%=userDn %>" style="font-style: italic; color: #808080;"><%=userCn %> (disabled)</a>
		<%}else{%>
                      		<a href="UserDetails.jsp?dn=<%=userDn %>"><%=userCn %></a>
		<%}%>
                    	</span>
                  	</div>
	<%} %>		
                </div>
                
                <img src="css/images/swish.gif" alt="#" />
                
                <div class="row">
                   	<span id="add-removeGroupPassed" style="float: center;" class="passed"></span>
					<span id="add-removeGroupFailed" style="float: center;" class="failed"></span>
				</div>
					
                <div style="width: 600px; padding: 5px; margin: 5px auto ";>
                  <h2 style="text-align: left; padding-left: 20px;">Organisation Details</h2>
                  <div class="row" style="font-size:11px; color:#007186">
                    <span style="float: left; text-align: right; width:200px;">Organisation Name:</span>
                    <span style="float: right; text-align: left; width:395px;"><%=org.getName() %></span>
                  </div>
                  <div class="row" style="font-size:11px; color:#007186">
                    <span style="float: left; text-align: right; width:200px;">Group DN:</span>
                    <span style="float: right; text-align: left; width:395px;"><%=Rdn.unescapeValue(org.getDistinguishedName()) %></span>
                  </div>
                  <br/>
<%
	if (lt.companyExistsAsGroup(request.getParameter("name"))) { %>
                  <div class = "row">
                    <span class="label2">Member of:</span>
                    <div id="memberOf" style="float: right; text-align: left; width:395px; font-size:11px; color:#007186">
                    	<table>
		<% Attributes attrs = lt.getGroupAttributes(request.getParameter("name"));
		Attribute attr = attrs.get("memberOf");
		List<String> ohGroupsThisUserCanAccess = (List<String>)session.getAttribute(AdminServlet.OHGROUPS_ALLOWED_ACCESSED);
		Set<String> groups = null;
		if(ohGroupsThisUserCanAccess == null){
			groups = lt.getBaseGroups();
		} else {
			groups = lt.getBaseGroupsWithGivenOHGroupsAllowedToBeAccessed(ohGroupsThisUserCanAccess);
		}
		if (attr != null) {
			NamingEnumeration e = attr.getAll(); 
			while (e.hasMore()) {
				String dn = (String)e.next();
				dn = (String)Rdn.unescapeValue(dn);
				String name = LdapTool.getCNValueFromDN(dn);
				groups.remove(name);%>
					<tr id='<%=dn%>'>
                    	<td><a class="Delete" onclick="deleteGroup('<%=dn%>')" href="#" title="Delete"></a></td>
						<td><%= name %></td>
                    </tr>
		<% 	}
		
		} else { %> <tr><td> No groups </td></tr><% } %>
                    	</table>
                    </div>
                  </div>
    	<% if (groups.size()>0) { %>
                <form id="addto" method="post" action="AddGroup">
                  <!-- <br /><span id="addlabel"><b>Add to Group:</b></span><br /> -->
                  <input type="hidden" id="name" name="name" value="<%= request.getParameter("name") %>" />
                  <select name="groupselect" id="groupselect">
			<% for (String group : groups) {%>
		  		    <option value="<%= group %>"><%= group %></option>
		 	<% } %>
                  </select>
                  <a class="Button" href="#" id="addbutton" onclick="javascript: SubmitGroupForm()">Add Organisation to Group</a>
                </form>
		<% } 
    } else { %>
                  <div class = "row">
                    <form method="post" id="create" action="CreateGroup">
                    <input type="hidden" id="name" name="name" value="<%= request.getParameter("name") %>" />
                    <span style="float: left; text-align: right; width:200px;"></span>
                    <span style="float: right; text-align: left; width:395px;">
                      <a class="Button" href="#" id="addbutton" onclick="javascript: SubmitForm()">Create Organisational Group</a>
                    </span>
                    </form>
                  </div>
	<% } 
	
	lt.close(); 

}%>
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
