<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
  
  	<%
  		// synchronizing the account details from Support Tracker DB to Ldap Account
  		// Because we are allowing the user to modify their name (first name, family name)
  		// which result to the posibility of:
  		// a change of the display name
  		// a change of DN (distinguished name) of the account.
  		// So, after the synchronization and if there was a change in display name (or distinguished name)
  		// then the value of "dn" parameter held in the request or request.getParameter("dn") is out of date
  		// and the value userDN after this line 'userDN = SyncAccountDetails.syncAndGetBackNewUserDNForGivenUserDN(userDN);'
  		// is the up to date one.
  		
  		// so, if you need a userDN in this page, you have to use userDN variable, instead of getting
  		// the value from  request.getParameter("dn").
  	
  		String userDN = request.getParameter("dn"); 
  		userDN = SyncAccountDetails.syncAndGetBackNewUserDNForGivenUserDN(userDN);
  	%>
  
    <title>Person: <%=userDN %></title>
    <script type="text/javascript" language="javascript" src="./js/ajaxgen.js"></script>
    <script type="text/javascript" language="javascript" src="./js/jquery.js"></script>
    <link rel="stylesheet" href="//code.jquery.com/ui/1.11.2/themes/smoothness/jquery-ui.css">
  	<script src="//code.jquery.com/ui/1.11.2/jquery-ui.js"></script>
     
    <link rel="stylesheet" href="./css/concerto.css" type="text/css" />
    <link rel="stylesheet" href="./css/general.css" type="text/css" />
    <jsp:useBean id="user" class="beans.LdapUser" scope="page" />
    <jsp:useBean id="countries" class="beans.Countries" scope="session" />
    <jsp:useBean id="groups" class="beans.LdapUserGroups" scope="session" />
    <jsp:useBean id="accounts" class="beans.AccountRequestsBean" scope="session" />
    <%@ page import="java.util.ArrayList" %>
    <%@ page import="java.util.TreeMap" %>
    <%@ page import="java.util.Map" %>
    <%@ page import="java.util.Set" %>
    <%@ page import="java.io.FileNotFoundException" %>
  	<%@ page import="java.net.ConnectException" %>
	<%@ page import="java.util.List" %>
	<%@ page import="javax.naming.directory.Attribute" %>
	<%@ page import="javax.naming.directory.Attributes" %>
	<%@ page import="javax.naming.NamingEnumeration" %>
	<%@ page import="javax.naming.NamingException" %>
	<%@ page import="javax.naming.ldap.Rdn" %>
	
  	<%@ page import="ldap.LdapTool" %>
  	<%@ page import="ldap.ErrorConstants" %>
  	<%@ page import="ldap.LdapProperty" %>
  	<%@ page import="ldap.LdapConstants" %>
  	<%@ page import="servlets.AdminServlet" %>
  	<%@ page import="tools.SyncAccountDetails" %>
  	<%@ page import="tools.SupportTrackerJDBC" %>
  	
  	
    <% 	try{
    		user.processUserDN(userDN);
    		groups.refreshGetUserGroup();
    		
    	} catch (Exception e) {
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
	
	    Attributes attrs = null;
	    Attribute attr = null;
	    Set<String> baseGroups = null;
	
	    if(lt != null){
	    	attrs = lt.getUserAttributes(userDN);
	    	attr = attrs.get("memberOf");
	    	List<String> ohGroupsThisUserCanAccess = (List<String>)session.getAttribute(AdminServlet.OHGROUPS_ALLOWED_ACCESSED);
	    	if(ohGroupsThisUserCanAccess == null){
	    		baseGroups = lt.getBaseGroups();
	    	} else {
				baseGroups = lt.getBaseGroupsWithGivenOHGroupsAllowedToBeAccessed(ohGroupsThisUserCanAccess);
	    	}
	    	lt.close();
	    	
	    	if(attrs==null || baseGroups==null){
	    		session.setAttribute("error", ErrorConstants.UNKNOWN_ERR);
	    	}
	    } else {
	    	session.setAttribute("error", ErrorConstants.UNKNOWN_ERR);
	    }
	%>
	
    <script type="text/javascript" language="javascript">
    
    //used to stored the content of the form before the "Update" button is clicked
    // then this content will be used to replace the content of the form (that user has chagned)
    // when the "Cancel" button is clicked
    var usrDtlsForm = "";
    
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
    	var idsNeededToBeCleanedUp = new Array("validation_msg", "add-removeGroupFailed", "add-removeGroupPassed", "fixAcctFailed", "fixAcctPassed");
    	for(var i=0; i<idsNeededToBeCleanedUp.length; i++){
    		document.getElementById(idsNeededToBeCleanedUp[i]).innerHTML = "";
    	}
    
    }
    function firstCharUp(input) {
        if (input.length > 1) {
            var firstLetter = input.charAt(0).toUpperCase();
            var restOfWord = input.substring(1, input.length);
            return firstLetter + restOfWord;
        } else if (input.length == 1) {
            return input.toUpperCase();
        } else {
            return input;
        }
    }
    
    <%int dsplSizeLimit = accounts.getDisplayNameSizeLimit();%>
	var displayNameSizeLimit = <%=dsplSizeLimit%>;
    
    function validateEntries() {
        var validated = true;
        var theFocus = '';
        
        // check the size of the displayName
        // because SPT DB has a limit size for this column
        // SPT-839
        var displayName = document.getElementById('displayName').value;
        if(displayName.length > displayNameSizeLimit){
      	  alert('The display name: ' + displayName + ' is too long. The allowed size is: ' + displayNameSizeLimit);
      	  document.getElementById('displayName').focus();
      	  return false;
        }
        
        document.getElementById('validation_msg').innerHTML = "";
        if (document.getElementById('sAMAccountName').value == "") {
            document.getElementById('validation_msg').innerHTML = "* Please enter the username<br/>";
            theFocus = 'sAMAccountName';
            validated = false;
        }
        if (document.getElementById('givenName').value == "") {
            document.getElementById('validation_msg').innerHTML = "* Please enter the first name<br/>";
            theFocus = 'givenName';
            validated = false;
        }
        if (document.getElementById('sn').value == "") {
            document.getElementById('validation_msg').innerHTML += "* Please enter the last name<br/>";
            if (theFocus == '')
                theFocus = 'sn';
            validated = false;
        }
        if (document.getElementById('department').value == "") {
            document.getElementById('validation_msg').innerHTML += "* Please enter the department<br/>";
            if (theFocus == '')
                theFocus = 'department';
            validated = false;
        }
        if (document.getElementById('description').value == "") {
            document.getElementById('validation_msg').innerHTML += "* Please enter/select the role of the person<br/>";
            if (theFocus == '')
                theFocus = 'description';
            validated = false;
        }
        if (document.getElementById('company').value == "") {
            document.getElementById('validation_msg').innerHTML += "* Please select from the list the company where this user belongs.<br/>";
            if (theFocus == '')
                theFocus = 'company';
            validated = false;
        }
        if (document.getElementById('c').value == "") {
            document.getElementById('validation_msg').innerHTML += "* Please select a country for the address<br/>";
            if (theFocus == '')
                theFocus = 'c';
            validated = false;
        }
        if (document.getElementById('telephoneNumber').value == "") {
            document.getElementById('validation_msg').innerHTML += "* Please enter a contact phone number<br/>";
            if (theFocus == '')
                theFocus = 'telephoneNumber';
            validated = false;
        }
        if (document.getElementById('mail').value == "") {
            document.getElementById('validation_msg').innerHTML += "* Please enter an email<br/>";
            if (theFocus == '')
                theFocus = 'mail';
            validated = false;
        }
        if (!validated)
            document.getElementById(theFocus).focus();
        return validated;
    }
    function doDisplayName() {
        document.form.displayName.value = document.form.givenName.value + " " + document.form.sn.value;
    }
    
    /* Enable each input of the form to allow user to modify and submit the update
    */
    function UpdateForm() {
    	usrDtlsForm = $('#usrDtlsForm').html();
    	cleanUpThePage();
    	document.getElementById('givenName').disabled = false;
    	document.getElementById('sn').disabled = false;
    	document.getElementById('displayName').disabled = false;
    	document.getElementById('department').disabled = false;
    	document.getElementById('description').disabled = false;
    	document.getElementById('streetAddress').disabled = false;
    	document.getElementById('l').disabled = false;
    	document.getElementById('st').disabled = false;
    	document.getElementById('postalCode').disabled = false;
    	document.getElementById('telephoneNumber').disabled = false;
    	document.getElementById('mail').disabled = false;
    	document.getElementById('facsimileTelephoneNumber').disabled = false;
    	document.getElementById('mobile').disabled = false;
    	if(document.getElementById('company').value == "")
    		document.getElementById('company').disabled = false;
    	document.getElementById('c').disabled = false;
    	document.getElementById('buttonGrp1').style.display = 'none';
        document.getElementById('buttonGrp2').style.display = 'block';
    }
    function CancelForm() {
    	cleanUpThePage();
    	$('#usrDtlsForm').html(usrDtlsForm);
    }
    function BackForm() {
    	history.back();
    }
    function SubmitForm() {
    	cleanUpThePage();
		if (validateEntries()) {
			document.form.submit();
			document.getElementById("submitButton").removeAttribute('onclick');
			document.getElementById("cancelButton").removeAttribute('onclick');
			return true;
		}
    	return false;
    }
    function deleteGroup(encodedGroupDN){
    	cleanUpThePage();
    	
    	var encodedUserDN = document.getElementById("dnInput").value;
    	ajax.open("POST", "RemoveAGroupFromUser", true);
    	ajax.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
        ajax.setRequestHeader("Accept", "text/xml, application/xml, text/plain");
        var params = "userDN=" + encodedUserDN + "&groupDN=" + encodedGroupDN;
        
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
			        			value += "<tr id='" +escape(dnValue)+ "'> <td><a class='Delete' onclick=\"deleteGroup('"+escape(dnValue)+"')\" href='#' title='Delete'></a></td> <td>"+nameValue+"</td> </tr>";
			        		}
		        		} else { // there's no group in the list memberOf
		        			value += "<tr><td> No groups </td></tr>";
		        		}
		        		value += "</table>";
		        		document.getElementById("memberOf").innerHTML = value;	              
		        		
		        		// update the drop down menu which is listing all the groups that this user is not bellonging to
		        		value = "";
		        		var nonMemberOfList = xmlDoc.getElementsByTagName("notMemberOf");
		        		for(var i=0; i<nonMemberOfList.length; i++){
		        			var nameValue = nonMemberOfList[i].firstChild.nodeValue;
		        			value += "<option value='" + escape(nameValue) + "'>" + nameValue + "</option>";
		        		}
		        		document.getElementById("groupselect").innerHTML = value;
		        		
            			var passed = "This user has been deleted from group: " + unescape(encodedGroupDN) + " successfully.";
            			document.getElementById("add-removeGroupPassed").innerHTML = "<font color=\"green\"><b>" + passed +"</b></font>";
		        	} else {
		        		var failedMessage = "<font color=\"red\"><b>Deletion of group '" + unescape(encodedGroupDN) 
														+ "' from user " + unescape(encodedUserDN) + " has failed.</b>"
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
    function SubmitGroupForm() {
    	cleanUpThePage();
    	
    	// send a POST request to AddGroupUser servlet with parameters: dn and gropselect
    	// this is a dn-name of this user
    	var encodedUserDN = document.getElementById("dnInput").value;
    	// this groupSelect is just a simple name (not a dn-name)
    	var encodedGroupSelect = document.getElementById("groupselect").value; 
    	ajax.open("POST", "AddGroupUser", true);
    	ajax.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
        ajax.setRequestHeader("Accept", "text/xml, application/xml, text/plain");
        var params = "dn=" + encodedUserDN + "&groupselect=" + encodedGroupSelect;
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
		        			value += "<tr id='" +escape(dnValue)+ "'> <td><a class='Delete' onclick=\"deleteGroup('"+escape(dnValue)+"')\" href='#' title='Delete'></a></td> <td>"+nameValue+"</td> </tr>";
		        		}
		        		value += "</table>";
		        		document.getElementById("memberOf").innerHTML = value;	              
		        		
		        		// update the drop down menu which is listing all the groups that this user is not bellonging to
		        		value = "";
		        		var nonMemberOfList = xmlDoc.getElementsByTagName("notMemberOf");
		        		for(var i=0; i<nonMemberOfList.length; i++){
		        			var nameValue = nonMemberOfList[i].firstChild.nodeValue;
		        			value += "<option value='" + escape(nameValue) + "'>" + nameValue + "</option>";
		        		}
		        		document.getElementById("groupselect").innerHTML = value;
		        		
		        		var passed = xmlDoc.getElementsByTagName("passed")[0].firstChild.nodeValue;
		        		document.getElementById("add-removeGroupPassed").innerHTML = "<font color=\"green\"><b>" + passed +"</b></font>";
		        	} else {
		        		// xml reponse contains "failed" tage:
		        		// set the add-removeGroupFailed element
		        		var reason = "<font color=\"red\"><b>" + failed.firstChild.nodeValue +"</b></font>";
						document.getElementById("add-removeGroupFailed").innerHTML = reason;
		        	}
        		} else {
            		// if reponse is not 200
            		// set the add-removeGroupFailed element
            		var reason = "<font color=\"red\"><b>Addition of organisation '" + unescape(encodedUserDN) 
    															+ "' to group " + unescape(encodedGroupSelect) + " has failed.</b>"
    												  "<b> Server is not responding to the request. </b>"
    													+"</font>";
    				document.getElementById("add-removeGroupFailed").innerHTML = reason;
        		}
        	}
        }
        
	}
    function ToggleStatus() {
    	cleanUpThePage();
    	
    	var url = "UpdateUserStatus?dn=<%=java.net.URLEncoder.encode(userDN) %>";
        if (document.getElementById('accstatus').innerHTML == "Disabled") {
            url += "&action=enabling";
        }else{
            url += "&action=disabling";
        }
    	ajax.open("POST", url, true);
        ajax.onreadystatechange = handleHttpResponse;
        ajax.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
        ajax.setRequestHeader("Accept", "text/xml, application/xml, text/plain");
        ajax.send('');
    }
    function handleHttpResponse(){
        if(ajax.readyState == 4){
            if(ajax.status == 200){
                results = ajax.responseText.split("|");
                if(results[0] == "true"){
                    document.getElementById('validation_msg').innerHTML =
                        "<font color=\"#00FF00\">* "+results[1]+"</font><br />";
                    if (document.getElementById('accstatus').innerHTML == "Disabled") {
                        document.getElementById('accstatus').innerHTML = "Enabled";
                    }else{
                        document.getElementById('accstatus').innerHTML = "Disabled";
                    }
                }else{
                    document.getElementById('validation_msg').innerHTML =
                        "<font color=\"#FF0000\">* "+results[1]+"</font><br />";
                }
            }else{
    		    document.getElementById('validation_msg').innerHTML =
    		        "<font color=\"#FF0000\">* The system encountered an error while processing, please try again later.</font><br />";
    	    }
        }
    }
    
    
    var fixingSolution = "";
    function getAcctStatusDetails(encodedUserDN){
    	cleanUpThePage();
    	
    	$("#fixAcctPassed").html("Checking account consistency.");
    	
    	var param = "rqst=getAUserStatusDetails&unescapedUserDN=" + encodedUserDN; 
    	
    	var jqxhr = $.post("AccountStatusDetails", param, function(result){
    		cleanUpThePage();
    		
    		try{
				var xmlRslt = $.parseXML(result);
				var result = '<a id="fixActStsLink" style="background-size:20px; padding-left:20px" href="#"' +
							'onclick=\'dialogPop(event,"' + encodedUserDN + '")\''

				var isLimtiedAct = false;
				if($(xmlRslt).find('limited').length > 0){
					fixingSolution = $(xmlRslt).find('limited').find("solution")[0].firstChild.data;
	       			result += ' title="Account is limited. Click for details." class="Add" ></a>';
	       			isLimtiedAct = true;
	       		} else if($(xmlRslt).find('brokenCantBeFixed').length > 0){
	       			fixingSolution = $(xmlRslt).find('brokenCantBeFixed').find("solution")[0].firstChild.data;
	       			result += ' title="Account is broken. Click for details." class="CantBeFixed" ></a>';
	       			
	       		} else if($(xmlRslt).find('brokenInDisabling').length > 0){
	       			fixingSolution = $(xmlRslt).find('brokenInDisabling').find("solution")[0].firstChild.data;
	       			result += ' title="Account is broken. Click for details." class="Fix" ></a>';
	       			
	       		} else if($(xmlRslt).find('broken').length > 0){
	       			fixingSolution = $(xmlRslt).find('broken').find("solution")[0].firstChild.data;
	       			result += ' title="Account is broken. Click for details." class="Fix" ></a>';

	       		// for the case this user is disabled or enabled
	       		} else { //if($(xmlRslt).find('disabled').length > 1 || $(xmlRslt).find('enabled').length > 1)
					result = "";
				}
				
				$('#fixActStatus').html(result);
				
				if(result==""){
					$("#fixAcctPassed").html("Account is consistent.")
				} else {
					if(isLimtiedAct){
						$("#fixAcctPassed").html("Limited account.");
					} else {
						$("#fixAcctPassed").html("Account is not consistent.");
					}
				}
			
								
			} catch (e) {
				cleanUpThePage();
				$("#fixAcctFailed").html("Server failed to process the account status request. Here's the message: " + result);
			}
    	})
    	.fail(function() {
    		cleanUpThePage();
    		$("#fixAcctFailed").html("Could not get a response from server while checking this account status.");
		 });
    	
    }
    
    
    
    // show the dialog that give user 3 choices:
    // 1). fix the broken accoutn
    // 2). link to the issues details page
    // 3). cancel
    function dialogPop(event, encodedUserDN){
    	event.preventDefault();
    	if($('#popedupDialog').length < 1){ // avoid to have multiple dialogs (so, it poped up only, if there's no dialog has been poped up)
    		var text = '<div id="popedupDialog">'+
    		'<div style="color:black; font-weight:normal">' +
    		fixingSolution.replace(new RegExp('\n|\r\n|\r', 'g'), '<br/>') +
    		'</div>' +
    		'<p><br/></p>' +
			'<p><a onclick="openExplanation(event)" href="#">Open issues details page...</a></p>'+
			'<p></p>' +
			'<p><a class="Button" onclick="fixUserAccount(event, \''+encodedUserDN+'\')" href="#">Fix Account</a> <a class="Button" onclick="closeDialog(event)" href="#">Cancel</a>' +
			'</div>';
			$(text).dialog({
				model:true,
				height: 'auto',
				width: 400, 
				resizable: false,
				dialogClass:'noTitleStuff', 
				position:{my:'left top', of:event}
			});
			
    	} else { // if there's a dialog has been poped up => close it
    		closeDialog(event);
    	}
    }
    function closeDialog(event){
    	event.preventDefault();
	    	// close the poped up dialog
	    if($('#popedupDialog').length > 0){
	   		$('#popedupDialog').dialog('close');
	   		$('#popedupDialog').remove();
	   		$('#fixActStsLink').blur();
	    }
    }
	// open the link page (the issues details woki page)
    function openExplanation(event){
    	closeDialog(event);
    	<% String  acctDetailsLink = LdapProperty.getProperty(LdapConstants.ACCT_BROKEN_DETAILS_LINK);%>
    	var win = window.open('<%=acctDetailsLink%>', '_blank');
    	win.focus();
    }
	
	
	
    
    function fixUserAccount(event, encodedUserDN){
    	cleanUpThePage();
    	closeDialog(event);
    	
    	var params = "rqst=fixUser&userDN=" + encodedUserDN;
    	$("#fixAcctPassed").html("Processing request...");
    	var jqxhr = $.post( "AccountStatusDetails", params, function(result) {
    		try{
    			var xmlRslt = $.parseXML(result);
				
				// all broken has been fixed (no any failed to fix in the XML result)
				if($(xmlRslt).find("failedToFix").length < 1){
					$('#fixActStatus').html(""); // remove the broken link
					$("#fixAcctPassed").html("Account has been successfully fixed.");
				
				// some issues have not been fixed
				} else {
					var fixedRslt = '';
					if($(xmlRslt).find("fixed").length > 0){
						// if there are some issues have been fixed
						// show that fixed results
						fixedRslt = $(xmlRslt).find("fixed")[0].firstChild.data;
					}
					
					// show those issues that couldnot be fixed
					var failedToFix = $(xmlRslt).find("failedToFix")[0].firstChild.data;
					$("#fixActStsLink").prop('title', fixedRslt + failedToFix);
					
					$("#fixAcctPassed").html(fixedRslt);
					$("#fixAcctFailed").html(failedToFix);
				}
    		} catch (e) {
    			cleanUpThePage();
				$("#fixAcctFailed").html("Server failed to process fixing account status request. Here's the message: " + e.message);
			}
    	})
    	.fail(function() {
    		cleanUpThePage();
    		$("#fixAcctFailed").html("Could not get a response from server while fixing this account status.");
		 });
    }
    </script>
    <style type="text/css">
      #sAMAccountName             {width: 200px;}
      #givenName                  {width: 200px;}
      #sn                         {width: 200px;}
      #displayName                {width: 200px;}
      #department                 {width: 200px;}
      #description                {width: 200px;}
      #streetAddress              {width: 200px;}
      #l                          {width: 200px;}
      #st                         {width: 200px;}
      #c                          {width: 205px;}
      #postalCode                 {width: 200px;}
      #telephoneNumber            {width: 200px;}
      #mail                       {width: 200px;}
      #facsimileTelephoneNumber   {width: 200px;}
      #mobile                     {width: 200px;}
      #company                    {width: 205px;}
      
      
      /* styles for controlling poped up dialog*/
      div.ui-widget-header, div.ui-state-default, div.ui-button{
            background:white;
            font-family: Arial, Helvetica, sans-serif;
            color: #007186;
            font-size:20px;
            font-weight: bold;
      }
      div.ui-dialog, div.ui-widget, div.ui-widget-content, div.ui-corner-all, div.ui-front, div.ui-resizable, div.ui-dialog-content, div.ui-widget-content {
        	backgroun:white
        	border: 1px solid #b9cd6d;
            font-family: Arial, Helvetica, sans-serif;
            color: #007186;
            font-size:12px;
            font-weight: bold;
      }
      .noTitleStuff .ui-dialog-titlebar {display:none}  /*title bar is not displayed*/
         
    </style>
  </head>
  <body onload='getAcctStatusDetails("<%=java.net.URLEncoder.encode(userDN)%>")'>
    <table align="center" border="0" style="border-color: #ef7224" cellspacing="1">
      <tr>
        <td bgcolor="#ef7224">
          <table bgcolor="#ffffff" width="600px">
            <tr align="center">
              <td align="center">
                <div align="center"><img src="css/images/logos/supporttracker.gif" alt="Support Tracker Logo" /></div>
                
                
                
                <h1><%=user.getDisplayName() %>  <span id="fixActStatus"></span> </h1>

		
				
                <div><a href="ChangePassword?rqstFrom=userDetail">Change Password</a></div>
                
                <div class="row">
                	<span id="fixAcctPassed" style="float: center;" class="passed"></span>
                	<br/>
					<span id="fixAcctFailed" style="float: center;" class="failed"></span>
				</div>
                
                <img src="./css/images/swish.gif" alt="There should be an image here...." />
<%  if(session.getAttribute("error") != null){ %>
                <div class="row">
                  <div class="error" style="float: center; width=100%; text-align: center">
         <%=session.getAttribute("error") %>
         <% session.removeAttribute("error"); %>
                  </div>
                </div>
<%  }else if(session.getAttribute("isAdmin") == null){ %>
                <div class="error" style="float: center; width=100%; text-align: center">Only support administrators can access this page.</div>

<%  }else{ // this open bracket is paired with a close at the end of this file%>
                <br />
                <div style="width: 500px; padding: 5px; margin: 5px auto ";>
                  <span style="float:left; width:auto; text-align: left; font-family: Arial, Helvetica, sans-serif;">Account status: </span>
<%	if(user.getAccountDisabled()){ %>
                  <span id="accstatus" style="float:left; width:auto; text-align: left; padding-left:5px; font-family: Arial, Helvetica, sans-serif;">Disabled</span>
<%	}else{	%>
                  <span id="accstatus" style="float:left; width:auto; text-align: left; padding-left:5px; font-family: Arial, Helvetica, sans-serif;">Enabled</span>
<%	}	%>
                  <a style="float: left; padding: 0px; display: compact; padding-left: 5px; font-family: Arial, Helvetica, sans-serif;"
                   id="toggleStatus" href="#" onclick="javascript: ToggleStatus();">Toggle</a>
                </div>
                <div class="row">
                  <span class="error" id="validation_msg" style="float:left; width=100%; text-align: left"></span>
                </div>
                <div style="width: 500px; padding: 5px; margin: 5px auto ";>
                  <form id="usrDtlsForm" name="form" method="post" action="UpdateUserDetails" onsubmit="return validateEntries();">
                    <div class="row">
                      <span class="label2">Username:</span>
                      <span class="formw">
                        <input disabled="disabled" type="text" id="sAMAccountName" name="sAMAccountName" size="<%=dsplSizeLimit%>" maxlength="<%=dsplSizeLimit%>" tabindex="1"
                         value="<%=user.getUsername() %>" />
                        <input type="hidden" name="dn" value="<%=userDN %>" />
<%	session.setAttribute("dn", userDN); %>
                      </span>
                      <span class="required">*</span>
                    </div>
                    <div class="row">
                      <span class="label2">First Name:</span>
                      <span class="formw">
                        <input disabled="disabled" type="text" id="givenName" name="givenName" size="<%=dsplSizeLimit%>" maxlength="<%=dsplSizeLimit%>" tabindex="2"
                         value="<%=user.getFirstName() %>" onblur="this.value=firstCharUp(this.value); doDisplayName();" />
                      </span>
                      <span class="required">*</span>
                    </div>
                    <div class="row">
                      <span class="label2">Last Name:</span>
                      <span class="formw">
                        <input disabled="disabled" type="text" id="sn" name="sn" size="<%=dsplSizeLimit%>" maxlength="<%=dsplSizeLimit%>" tabindex="3"
                         value="<%=user.getLastName() %>" onblur="this.value=firstCharUp(this.value); doDisplayName();" />
                      </span>
                      <span class="required">*</span>
                    </div>
                    <div class="row">
                      <span class="label2">Display Name:</span>
                      <span class="formw">
                        <input disabled="disabled" readonly="readonly" type="text" id="displayName" name="displayName" size="<%=dsplSizeLimit%>" maxlength="<%=dsplSizeLimit%>"
                         value="<%=user.getDisplayName() %>" />
                      </span>
                    </div>
                    <div class="row">
                      <span class="label2">Position / Role:</span>
                      <span class="formw">
       
       
<!-- if Orion Health staff, then this role will be a drop down menu where the roles list is generated from the LK_positionCode table of ST DB -->               
<% if(user.getCompany().equalsIgnoreCase(LdapTool.ORION_HEALTH_NAME)){ %>
						<select disabled="disabled" id="description" name="description" tabindex="4" style="width:205px">
<% Set<String> allRoles = SupportTrackerJDBC.getAllPositionCodeNames();
		for(String role : allRoles){
			if(role.equalsIgnoreCase(user.getDescription().trim())){
%>
							<option value="<%=user.getDescription().trim() %>" selected="selected"><%=user.getDescription() %></option>

<%			} else {		%>
							<option value="<%=role %>"><%=role %></option>
<%			}
		}
%>
						</select>



<!-- if it is not Orion Health staff, then this role will be an input box -->
<% } else { %>
                        <input disabled="disabled" type="text" id="description" name="description" size="<%=dsplSizeLimit%>" maxlength="<%=dsplSizeLimit%>" tabindex="4"
                         value="<%=user.getDescription() %>" />
<% } %>                     
                         
                      </span>
                      <span class="required">*</span>
                    </div>
                    <div class="row">
                      <span class="label2">Department:</span>
                      <span class="formw">
                        <input disabled="disabled" type="text" id="department" name="department" size="<%=dsplSizeLimit%>" maxlength="<%=dsplSizeLimit%>" tabindex="5"
                         value="<%=user.getDepartment() %>" />
                      </span>
                      <span class="required">*</span>
                    </div>
                    <div class="row">
                      <span class="label2">Company:</span>
                      <span class="formw">
                        <select disabled="disabled" id="company" name="company" tabindex="6">
                          <option value="">Please select one from the list</option>
<%	String[] userGroups = groups.getUserGroups();
	//System.out.println(user.getCompany());
	for(int i = 0; i < userGroups.length; i++){
		String group = userGroups[i];
		if(group.equals(user.getCompany())){	%>
                          <option value="<%=group %>" selected="selected"><%=group %></option>
<%		}else{	%>
                          <option value="<%=group %>"><%=group %></option>
<%		}
	}	%>
                        </select>
                      </span>
                      <span class="required">*</span>
                    </div>
                    <div class="row">
                      <span class="label2">No. / Street:</span>
                      <span class="formw">
                        <input disabled="disabled" type="text" id="streetAddress" name="streetAddress" size="50" maxlength="50" tabindex="7"
                         value="<%=user.getStreet() %>" />
                      </span>
                    </div>
                    <div class="row">
                      <span class="label2">City:</span>
                      <span class="formw">
                        <input disabled="disabled" type="text" id="l" name="l" size="<%=dsplSizeLimit%>" maxlength="<%=dsplSizeLimit%>" tabindex="8"
                         value="<%=user.getCity() %>" />
                      </span>
                    </div>
                    <div class="row">
                      <span class="label2">State:</span>
                      <span class="formw">
                        <input disabled="disabled" type="text" id="st" name="st" size="<%=dsplSizeLimit%>" maxlength="<%=dsplSizeLimit%>" tabindex="9"
                         value="<%=user.getState() %>" />
                      </span>
                    </div>
                    <div class="row">
                      <span class="label2">Postal Code:</span>
                      <span class="formw">
                        <input disabled="disabled" type="text" id="postalCode" name="postalCode" size="<%=dsplSizeLimit%>" maxlength="<%=dsplSizeLimit%>" tabindex="10"
                         value="<%=user.getPostalCode() %>" />
                      </span>
                    </div>
                    <div class="row">
                      <span class="label2">Country:</span>
                      <span class="formw">
                        <select disabled="disabled" id="c" name="c" tabindex="11">
                          <option value="">Please select one from the list</option>
<%	TreeMap<String,String> countriesMap = countries.getCountries();
	for(Map.Entry<String, String>entry:countriesMap.entrySet()){
		String countryName = entry.getKey();
		String countryCode = entry.getValue();
		if(countryCode.equals(user.getCountry())){	%>
                          <option value="<%=countryCode %>" selected="selected"><%=countryName %></option>
<%		}else{	%>
                          <option value="<%=countryCode %>"><%=countryName %></option>
<%		}
	}	%>
                        </select>
                      </span>
                      <span class="required">*</span>
                    </div>
                    <div class="row">
                      <span class="label2">Phone:</span>
                      <span class="formw">
                        <input disabled="disabled" type="text" id="telephoneNumber" name="telephoneNumber" size="<%=dsplSizeLimit%>" maxlength="<%=dsplSizeLimit%>" tabindex="12"
                         value="<%=user.getPhoneNumber() %>" />
                      </span>
                      <span class="required">*</span>
                    </div>
                    <div class="row">
                      <span class="label2">Fax:</span>
                      <span class="formw">
                        <input disabled="disabled" type="text" id="facsimileTelephoneNumber" name="facsimileTelephoneNumber" size="<%=dsplSizeLimit%>" maxlength="<%=dsplSizeLimit%>" tabindex="13"
                         value="<%=user.getFax() %>" />
                      </span>
                    </div>
                    <div class="row">
                      <span class="label2">Mobile:</span>
                      <span class="formw">
                        <input disabled="disabled" type="text" id="mobile" name="mobile" size="<%=dsplSizeLimit%>" maxlength="<%=dsplSizeLimit%>" tabindex="14"
                         value="<%=user.getMobile() %>" />
                      </span>
                    </div>
                    <div class="row">
                      <span class="label2">Email:</span>
                      <span class="formw">
                        <input disabled="disabled" type="text" id="mail" name="mail" size="50" maxlength="50" tabindex="15"
                         value="<%=user.getEmail() %>" />
                      </span>
                      <span class="required">*</span>
                    </div>
	
                    <div id="buttonGrp1" class="Buttons" style="text-align: center; clear: none; padding-top: 20px; width: 200px; height: 20px; display: block">
                      <a class="Button" id="updateButton" onclick="javascript: UpdateForm()" style="display: compact;" href="#">Update</a>
                      <a class="Button" href="OrganisationDetails.jsp?name=<%=java.net.URLEncoder.encode(user.getCompany())%>" style="display: compact;">Back</a>
                    </div>
                    <div id="buttonGrp2" class="Buttons" style="text-align: center; clear: none; padding-top: 20px; width: 200px; height: 20px; display: none">
                      <a class="Button" id="submitButton" onclick="javascript: SubmitForm()" style="display: compact;" href="#">Submit</a>
                      <a class="Button" id="cancelButton" onclick="javascript: CancelForm()" style="display: compact;" href="#">Cancel</a>
                    </div>
                  </form>
                  
                  <div class="row">
                  <%  if(session.getAttribute("passed") != null){ %> 
                    	<span id="add-removeGroupPassed" style="float: center;" class="passed"><%=session.getAttribute("passed") %></span>
                  <%      session.removeAttribute("passed");
                      } else if(session.getAttribute("failed") != null){ %>
                  		<span id="add-removeGroupFailed" style="float: center;" class="failed"><%=session.getAttribute("failed") %></span>
                  <% } %>
                  		
                  		<span id="add-removeGroupPassed" style="float: center;" class="passed"></span>
						<span id="add-removeGroupFailed" style="float: center;" class="failed"></span>
					</div>
					
					
                    <div class="row">
                    <span class="label2">Member of:</span>
                    <div id="memberOf" style="float: right; text-align: left; width:395px; font-size:11px; color:#007186"><table>
<% 
if (attr != null) {
NamingEnumeration e = attr.getAll(); 
	while (e.hasMore()) {
		String dn = (String)e.next();
		dn = (String)Rdn.unescapeValue(dn);
		String name = LdapTool.getCNValueFromDN(dn);
		baseGroups.remove(name);%>
	              <tr id='<%= java.net.URLEncoder.encode(dn) %>'>
	              		<td><a class="Delete" onclick="deleteGroup( '<%= java.net.URLEncoder.encode(dn) %>' )" href="#" title="Delete"></a></td>
	              		<td><%= name %></td>
	              </tr>
	<%} 
} else { %>
				  <tr><td>No groups</td></tr>
<%}%>
                    </table></div>
                  </div>
<%if (baseGroups.size()>0) { %>
                <form id="addto" method="post" action="AddGroupUser">
                  <!-- <br /><span id="addlabel"><b>Add to Group:</b></span><br /> -->
                  <input id="dnInput" type="hidden" name="dn" value="
    <%=java.net.URLEncoder.encode(userDN) %>" />
    <%session.setAttribute("dn", userDN); %>
                  <select name="groupselect" id="groupselect">
    <%for (String group : baseGroups) {%>
		  		    <option value="
		<%= java.net.URLEncoder.encode(group) %>"><%= group %>
		  		    </option>
	<%}%>
                  </select>
                  <a class="Button" href="#" id="addbutton" onclick="javascript: SubmitGroupForm()">Add User to Group</a>
                </form>
<%}%>
                </div>



<%} //this bracket is paired with the open at else after if(isAdmin) %>
				
				
                <div align="center"><img src="./css/images/swish.gif" alt="There should be an image here...." /></div>
                <div align="center" class="disclaimer2">Having problems?<br />Email <a href="mailto:support@orionhealth.com">support@Orionhealth.com</a><br /><br /></div>
              </td>
            </tr>
          </table>
        </td>
      </tr>
    </table>  
  </body>
</html>
