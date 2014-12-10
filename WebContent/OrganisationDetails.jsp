<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%@page import="java.net.URLEncoder"%>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <title>Organisation: <%=request.getParameter("name") %></title>
  <script type="text/javascript" language="javascript" src="./js/ajaxgen.js"></script>
  <script src="//ajax.googleapis.com/ajax/libs/jquery/2.1.1/jquery.min.js"></script>
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
  <%@ page import="java.util.ArrayList" %>
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
  
  
<style>

a.showingAcctSt {
	font-family: VArial, Helvetica, sans-serif;
  	width:200px; 
  	display:block;
    border:1px solid #7CBAFF;
    background-color:#7CBAFF;
    text-decoration:none;
    color:#666;
    padding:5px 5px 5px 25px;"
}

a.showingAcctSt:hover{
	border:1px solid #7CBABF;
    background-color:#7CBABF;
    color:#888;
}

a.loadingAcctSt{
    font-family: VArial, Helvetica, sans-serif;
  	width:200px; 
  	display:block;
    border:1px solid #ddd;
    background-color:#ddd;
    text-decoration:none;
    color:#aaa;
    padding:5px 5px 5px 25px;"
}
</style>


<script>

/**
 * idMap, idIndex, putNameAndGetIdIndex(), getIdIndex() are used to store, generate and retrieve
 * a unique id for a given userDN name. this unique id is used for creating an id for an html element
 * for a user account.
 *
 * because userDN cannot be used as an Id of html element, thats why we need this unique id
 */
var idMap = {};
var idIndex = -1;
var xmlRslt = {};
function putNameAndGetIdIndex(encodedUserDN){
	idIndex = idIndex + 1;
	var thisIndex = 'acctStatus'+idIndex;
	idMap[encodedUserDN] = thisIndex;
	return thisIndex;
}
function getIdIndex(encodedUserDN){
	return idMap[encodedUserDN];
}

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
	
	// make the elemnts of "add-removeFailed" and "add-removePassed" to an empty html 
	function cleanUpThePage(){
    	var idsNeededToBeCleanedUp = new Array("add-removeFailed", "add-removePassed");
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
		        		document.getElementById("add-removePassed").innerHTML = passed;
		        		return true;
		        	} else {
		        		// xml reponse contains "failed" tage:
		        		// set the add-removeFailed element
		        		var reason =  failed.firstChild.nodeValue ;
						document.getElementById("add-removeFailed").innerHTML = reason;
						return false;
		        	}
        		} else {
            		// if reponse is not 200
            		// set the add-removeFailed element
            		var reason = "Addition of group '" + thisGroupDN + "' to group " + newGroupName +
            					" has failed. Because Server is not responding to the request.";
    				document.getElementById("add-removeFailed").innerHTML = reason;
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
            			document.getElementById("add-removePassed").innerHTML = passed;
		        	} else {
		        		var failedMessage = "Deletion of this group "
														+ "from group " + fromGroupDN + " has failed."
											+ failed.firstChild.nodeValue;
							
						document.getElementById("add-removeFailed").innerHTML = failedMessage;
		        	}
            		
        			return;
        		}
        		
        		// if reponse is not 200 or xml reponse contains "failed" tage:
        		// set the add-removeFailed element
        		var failedMessage = "Server failed to response.";
				document.getElementById("add-removeFailed").innerHTML = failedMessage;
        	}
        	
        }
    }
	
	// get all accounts status from the server and stored it into global variable "xmlRslt"
	// then "xmlRslt" will be used to update the accounts statuses when user click on the "Show account status" button
	// we do like this, because it is to slow to wait fro server to response with the account status
	// so, we let the user work with the accounts first, then provide them the ability to update later when the server provided the response
	function getAllAccountsForThisOrganisation(orgSimpleName){
		cleanUpThePage();
		
		var param = "rqst=getAllUsersOfOrganisation&orgSimpleName=" + orgSimpleName;
		
		var jqxhr = $.post("OrganisationDetails", param, function(result){
			try{
				xmlRslt = $.parseXML(result);
				$("#acctStsLink").html('<a class="showingAcctSt" href="#" id="accountStatus" onclick="javascript: updateAccountStatus()">Show Accounts Status</a>');
				cleanUpThePage();
				
			} catch (e) {
				$("#add-removeFailed").html("Server failed to process the request. Here's the message: " + result);
			}
		})
			 .fail(function() {
				 alert( "Could not get a response from server." );
			 }) 
	}
	
	// updating the account status that the server provided as response 
	function updateAccountStatus(){
		if(xmlRslt){

			// add all the disabled accounts into the "#Disabledusers" element
			var disabledUsersText = "";
			$(xmlRslt).find("disabled").each(
				function(intIndex){
					var userDN = $(this).find("dn")[0].firstChild.data;
					var displayName = $(this).find("name")[0].firstChild.data;
					var thisIdIndex = putNameAndGetIdIndex(encodeURI(userDN));
					disabledUsersText += '<div id="'+thisIdIndex+'" class="row"><span style="float: inherit; width: 200px; text-align: center;">'+
											'<a  id="'+thisIdIndex+'Title'+'"  href="UserDetails.jsp?dn=' +encodeURI(userDN)+ '" style="font-style: italic; color: #808080;" >'+
											displayName + ' (disabled)</a></span></div>';
				}
			);
			$('#DisabledUsers').html(disabledUsersText);
			
			// add all the enabled accounts into the "#EnbaledUsers" element
			var enabledUsersText = "";
			$(xmlRslt).find("enabled").each(
				function(intIndex){
					var userDN = $(this).find("dn")[0].firstChild.data;
					var displayName = $(this).find("name")[0].firstChild.data;
					var thisIdIndex = putNameAndGetIdIndex(encodeURI(userDN));
					enabledUsersText += '<div id="'+thisIdIndex+'" class="row"><span style="float: inherit; width: 200px; text-align: center;">'+
											'<a href="UserDetails.jsp?dn=' +encodeURI(userDN)+ '">' +displayName+ '</a></span></div>';
				}
			);
			$('#EnbaledUsers').html(enabledUsersText);
			
			// add limited users into "#Limitedusers" element
			// and turn on the "#LimitedUsersCrossBar"
			var limitedUsersText = "";            
			var areThereAnyLimitedUsers = false;
			$(xmlRslt).find("limited").each(
				function(intIndex){
					
					areThereAnyLimitedUsers = true;
					var userDN = $(this).find("dn")[0].firstChild.data;
					var displayName = $(this).find("name")[0].firstChild.data;
					var solution = $(this).find("solution")[0].firstChild.data;
					var thisIdIndex = putNameAndGetIdIndex(encodeURI(userDN));
					limitedUsersText += '<tr id="'+thisIdIndex+'">';
					limitedUsersText += '<td><a  id="'+thisIdIndex+'Title'+'"  class="Add" onclick="fixUserAccount(\'' +encodeURI(userDN)+ '\')" href="#" title="' +solution+ '"/></td>' +
                						'<td><a href="UserDetails.jsp?dn=' +encodeURI(userDN)+ '">' +displayName+ '</a></td>'+
                						'</tr>';
				}
			);
			if(areThereAnyLimitedUsers){
				$('#LimitedUsersCrossBar').css("visibility", "visible");
				$('#LimitedUsers').html(limitedUsersText);
			}
			

			// add broken accounts (brokenCantBeFixed, brokenInDisabling and broken) to "#BrokenUsers" element
			var brokenUsersText = "";
			var areThereAnyBrokenUsers = false;
			$(xmlRslt).find("brokenCantBeFixed").each(
					function(intIndex){
						areThereAnyBrokenUsers = true;
						var userDN = $(this).find("dn")[0].firstChild.data;
						var displayName = $(this).find("name")[0].firstChild.data;
						var solution = $(this).find("solution")[0].firstChild.data;
						var thisIdIndex = putNameAndGetIdIndex(encodeURI(userDN));
						brokenUsersText += '<tr id="'+thisIdIndex+'">';
						brokenUsersText += '<td><a id="'+thisIdIndex+'Title'+'" class="CantBeFixed" onclick="fixUserAccount(\'' +encodeURI(userDN)+ '\')" href="#" title="' +solution+ '"/></td>' +
											'<td><a style="color:black" href="UserDetails.jsp?dn=' +encodeURI(userDN)+ '">' +displayName+ '</a></td>'+
		                					'</tr>';  
					}	
			);
			$(xmlRslt).find("brokenInDisabling").each(
					function(intIndex){
						areThereAnyBrokenUsers = true;
						var userDN = $(this).find("dn")[0].firstChild.data;
						var displayName = $(this).find("name")[0].firstChild.data;
						var solution = $(this).find("solution")[0].firstChild.data;
						var thisIdIndex = putNameAndGetIdIndex(encodeURI(userDN));
						brokenUsersText += '<tr id="'+thisIdIndex+'">';
						brokenUsersText += '<td><a id="'+thisIdIndex+'Title'+'" class="Fix" onclick="fixUserAccount(\'' +encodeURI(userDN)+ '\')" href="#" title="' +solution+ '"/></td>' +
											'<td><a style="font-style: italic; color: #808080;"  href="UserDetails.jsp?dn=' +encodeURI(userDN)+ '">' +displayName+ ' (disabled)</a></td>'+
		                					'</tr>';  
					}
			);
			$(xmlRslt).find("broken").each(
				function(intIndex){
					areThereAnyBrokenUsers = true;
					var userDN = $(this).find("dn")[0].firstChild.data;
					var displayName = $(this).find("name")[0].firstChild.data;
					var solution = $(this).find("solution")[0].firstChild.data;
					var thisIdIndex = putNameAndGetIdIndex(encodeURI(userDN));
					brokenUsersText += '<tr id="'+thisIdIndex+'">';
					brokenUsersText += '<td><a id="'+thisIdIndex+'Title'+'" class="Fix" onclick="fixUserAccount(\'' +encodeURI(userDN)+ '\')" href="#" title="' +solution+ '"/></td>' +
										'<td><a href="UserDetails.jsp?dn=' +encodeURI(userDN)+ '">' +displayName+ '</a></td>'+
	                					'</tr>';  
				}	
			);
			if(areThereAnyBrokenUsers){
				$('#BrokenUsersCrossBar').css("visibility", "visible");
				$('#BrokenUsers').html(brokenUsersText);
			}
			
			$("#acctStsLink").html('');
		}
	}
	
	
	//fix the a user account
	function fixUserAccount(encodedUserDN){
		//alert(encodedUserDN);
		cleanUpThePage();
		$("#add-removePassed").html("Loading...");
		
		var params = "rqst=fixUser&userDN=" + encodedUserDN;
		
		var jqxhr = $.post( "OrganisationDetails", params, function(result) {
			
			try{
				var xmlRslt = $.parseXML(result);
				
				var userDN = $(xmlRslt).find("dn")[0].firstChild.data;
				var displayName = $(xmlRslt).find("name")[0].firstChild.data;
				var thisIdIndex = getIdIndex(encodeURI(userDN));
				
				// all broken has been fixed (no any failed to fix in the XML result)
				if($(xmlRslt).find("failedToFix").length < 1){
					
					// try to figure out which account that has just been fixed
					// if this is a disabled account that has just been fixed
					// then isItDisabledAcct should become true
					var isItDisabledAcct = ($("#" + thisIdIndex).text().indexOf("disabled") > -1);
					
					// if it has been fixed, so remove the link from "broken accounts" section
					$("#" + thisIdIndex).remove();
					
					// if a disabled broken account has been fixed.
					if(isItDisabledAcct){
						var disabledUser = '<div id="'+thisIdIndex+'" class="row"><span style="float: inherit; width: 200px; text-align: center;">'+
											'<a  id="'+thisIdIndex+'Title'+'"  href="UserDetails.jsp?dn=' +encodeURI(userDN)+ '" style="font-style: italic; color: #808080;" '+
											'title="">'+ displayName + ' (disabled)</a></span></div>';
						$('#DisabledUsers').append(disabledUser);
					
					
					// other type of broken account has been fixed 
					} else {
						var fixedUser = '<div id="'+thisIdIndex+'" class="row"><span style="float: inherit; width: 200px; text-align: center;">'+
											'<a href="UserDetails.jsp?dn=' +encodeURI(userDN)+ '">' +displayName+ '</a></span></div>';
						$('#EnbaledUsers').append(fixedUser);
					}
					
					$("#add-removePassed").html("Account has been successfully fixed.");
					
				// some issues have not been fixed
				} else {
					var fixedRslt = '';
					if($(xmlRslt).find("fixed").length > 0){
						fixedRslt = $(xmlRslt).find("fixed")[0].firstChild.data;
					}
					var failedToFix = $(xmlRslt).find("failedToFix")[0].firstChild.data;
					$("#" + thisIdIndex + "Title").prop('title', fixedRslt + failedToFix);
					
					$("#add-removePassed").html(fixedRslt);
					$("#add-removeFailed").html(failedToFix);
				}
				
			} catch (e) {
				$("#add-removeFailed").html("Server failed to process the request. Here's the message: " + result);
			}
		
		})
			.fail(function() {
				alert( "Could not get a response from server." );
			})
			.always(function() {
			
			});
	}
	

	
</script>



</head>
  <body onload='getAllAccountsForThisOrganisation("<%=request.getParameter("name")%>")'>
  
  

  
  
  
  	
    <table align="center" border="0" style="border-color: #ef7224" cellspacing="1">
      <tr>
        <td bgcolor="#ef7224">
          <table bgcolor="#ffffff">
            <tr align="center">
              <td align="center">
                <div align="center"><img src="css/images/logos/supporttracker.gif" alt="Support Tracker Logo"/></div>
                <h1><%=request.getParameter("name") %></h1>
				<input id="thisGroupDN" type="hidden" value="<%=lt.getDNFromGroup(request.getParameter("name"))%>"/> <!-- this hidden input used to get complete groupDN for other processes -->
				
				


				<!-- used to update the process status, and process result. e.g. "Loading..." or "Successfully added a grou" or "Failed to update account status" -->
				<div class="row">
                   	<span id="add-removePassed" style="float:center; color:green; font-weight:bold;" class="passed"></span>
				</div>
				<div class="row">
					<span id="add-removeFailed" style="float:center; color:red; font-weight:bold;" class="failed"></span>
				<div>
				
				
				
				
                <img src="css/images/swish.gif" alt="#" />
                
<%if(session.getAttribute("pass") != null){ %>
                	<div class="pass" style="float: center; width=100%; text-align: center">
	<%=session.getAttribute("pass") %>
                	</div>
	<%session.removeAttribute("pass");/*Need to remove pass*/%> 
					<div class="row"><div style="float: right;"><div> <a class="Button" href="OrganisationDetails.jsp?name=<%=java.net.URLEncoder.encode(request.getParameter("name")) %>"> Back </a> </div></div></div>

<%} else if(session.getAttribute("error") != null){ %>
                	<div class="error" style="float: center; width=100%; text-align: center">
	<%=session.getAttribute("error") %>
                	</div>
	<%session.removeAttribute("error");/*Need to remove error*/%> 
					<div class="row"><div style="float: right;"><div> <a class="Button" href="OrganisationDetails.jsp?name=<%=java.net.URLEncoder.encode(request.getParameter("name")) %>"> Back </a> </div></div></div>

<%} else if(session.getAttribute("isAdmin") == null){ %>
                	<div class="error" style="float: center; width=100%; text-align: center">Only support administrators can access this page.</div>
<%} else { %>
				
				
				
			
				<!-- showing the button of "Lodaing Account Status..." and "Show Account Status" -->
				<br/>
				<div class="row"> <div id="acctStsLink"  >
						<a class="loadingAcctSt" id="accountStatus" >Loading Account Status...</a>
  				</div></div>
				
				<br />
				
				
				
				
				<!--  "Add New" user button -->
                
                <div style="width: 600px; padding: 5px; margin: 5px auto ";>
                
                <div class="row">
                  <div style="float: left">
                    <h2 style="text-align: left; padding-left: 20px;">Support Tracker Users</h2>
                  </div>
                  <div style="float: right">
                    <a class="Button" href="AddNewUser.jsp?company=<%= java.net.URLEncoder.encode(request.getParameter("name")) %>">Add New</a>
                  </div>
                </div>
                
                
                
           <!-- showing account name (temporary, until account status has been loaded, and user commanded to update the view) -->     
                <div id="EnbaledUsers">
	<%
		String[] keySet = org.getUsersKeys();
		ArrayList<String> disabledUserCNs = new ArrayList<String>();
		for( int i = 0; i < keySet.length; i++ ){
			String userCn = keySet[i];
			String userDn = users.get(userCn)[0];
			userDn = java.net.URLEncoder.encode(userDn);
			boolean accountDisabled = users.get(userCn)[1].equals("disabled"); 
			if(accountDisabled){
				disabledUserCNs.add(userCn);
			} else { %>
					<div class="row">
						<span style="float: inherit; width: 200px; text-align: center;">
							<a href="UserDetails.jsp?dn=<%=userDn %>"><%=userCn %></a>
						</span>
					</div>
				
	<%		}
		}%>
                </div>
                
                <div id="DisabledUsers">
	<%	for(int i=0; i<disabledUserCNs.size(); i++){ 
			String userCn = disabledUserCNs.get(i);
			String userDn = users.get(userCn)[0];
	%>
      				<div class="row">
							<span style="float: inherit; width: 200px; text-align: center;">
								<a href="UserDetails.jsp?dn=<%=userDn %>"><%=userCn %></a>
							</span>
						</div>
	<%	} %>
      
      
              	  	</div>
                </div>
                
                
                
                <!-- limited users start here   -->
                <div  class="row"  id="LimitedUsersCrossBar" style="visibility:hidden">
	                <div class="row">
	                <img src="css/images/swish.gif" alt="#" /></div>
	                <div class="row">
	                  <div style="float: left">
	                    <h2 style="text-align: left; padding-left: 20px;">Limited Users</h2>
	                  </div>
	                </div>
	            </div>
                
           
                <div class="row">
	                <table id="LimitedUsers">               
	                </table>
                </div>
                <div class="row"> <br/> </div>
                
                
                
                
                <!-- broken account users start here    -->
                <div class="row" id="BrokenUsersCrossBar" style="visibility:hidden" >
	                <div class="row"><img src="css/images/swish.gif" alt="#" /></div>
	                <div class="row">
	                  <div style="float: left">
	                    <h2 style="text-align: left; padding-left: 20px;">Broken accounts</h2>
	                  </div>
	                </div>
                </div>
           
                <div class="row">
	                <table id="BrokenUsers">                 		
	                </table>
                </div>
                <div class="row"> <br/> </div>
                
                
                
                
                
                
                <div class="row"><img src="css/images/swish.gif" alt="#" /></div>
				
					
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
