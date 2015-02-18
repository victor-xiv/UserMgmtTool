<?xml version="1.0" encoding="ISO-8859-1" ?>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
    <meta http-equiv="Cache-Control" content="no-store, no-cache, must-revalidate" />
    <meta http-equiv="Pragma" content="no-cache" /> 
    <meta http-equiv="expires" content="0" />
    <title>Account Requests Management</title>
    
    
    <!-- beans declaration -->
    <jsp:useBean id="accounts" class="beans.AccountRequestsBean" scope="session" />
    
    
    <%@ page import="java.util.HashMap" %>
	<%@ page import="java.util.List" %>
	<%@ page import="java.util.Map" %>
	<%@ page import="java.util.TreeMap" %>

<script type="text/javascript" language="javascript">
/*inform user that we are not supporting IE8 or older*/
if(navigator.appVersion.indexOf("MSIE 8.")!=-1 || navigator.appVersion.indexOf("MSIE 7")!=-1
		|| navigator.appVersion.indexOf("MSIE 6")!=-1 || navigator.appVersion.indexOf("MSIE 5")!=-1
		|| navigator.appVersion.indexOf("MSIE 4")!=-1) {
	alert("Internet Explorer 8 and older are not supported!");
	document.write('');
	window.onload = function(){ document.write('') };
}
</script>



    <link rel="stylesheet" href="./css/concerto.css" type="text/css" />
    <link rel="stylesheet" href="./css/general.css" type="text/css" />
    <link rel="shortcut icon" href="./css/images/oStar.ico" />
    <script src="./js/ajaxgen.js"></script>
    <script src="./js/jquery.js"></script>
    <script src="./js/validator.js"></script>
    <style type="text/css">
.CollapseRegionLink,.CollapseRegionLink:link,.CollapseRegionLink:hover,.CollapseRegionLink:visited
{
    cursor:pointer;
    color: #666666;
    font-family: Arial, Helvetica, sans-serif;
    font-weight:bold;
    font-size: 16px;
    text-decoration:none;
    padding-top: 1px;
    padding-bottom: 0px;
    width: 60%;
    float: left;
}
.CollapsibleSection
{
    border-style: none;
    font-family: Arial, Helvetica, sans-serif;
    font-size: 14px;
    color: #999999;
    text-align: center;
}
.node_o
{
    background:url('./css/images/opened.gif');
    width:16px;height:16px
}
.node_c
{
    background:url('./css/images/closed.gif');
    width:16px;height:16px
}
div.row span.label3{
  float: left;
  position: relative;
  width: 250px;
  text-align: right;
  font-family: Arial, Helvetica, sans-serif;
    top: 0px;
    left: 0px;
}
div.row span.value3{
  float: right;
  position: relative;
  width: 250px;
  text-align: left;
  font-family: Arial, Helvetica, sans-serif;
    top: 0px;
    left: +4px;
}
    </style>
	<script type="text/javascript" language="javascript">

// used to limit the max number of chars for the fullname 
var displayNameSizeLimit = <%=accounts.getDisplayNameSizeLimit()%>;
	
var id = '';

/**
 * clear the text in "validate_msg" element
 */
function cleanupResultMsg(){
	document.getElementById('validation_msg').innerHTML = "";
}


/**
 * toggle on or of the drop down menu of the detail of the account requested that belong to the given idx
 */
function applyClick(idx) {
  cleanupResultMsg();
	
  var item_id = 'request' + idx;
  var image_id = 'image' + idx;
  if (document.getElementById(image_id).className == 'node_c')
    document.getElementById(image_id).className = 'node_o';
  else
    document.getElementById(image_id).className = 'node_c';
  if (document.getElementById(item_id).style.display == "block")
    document.getElementById(item_id).style.display = "none";
  else
    document.getElementById(item_id).style.display = "block";
}

/**
 * check whether the username in the "username" block is valid
 */
function validateUsername(idx) {
  cleanupResultMsg();
  
  //check the size of the displayName
  // because SPT DB has a limit size for this column
  // SPT-839
  var displayName = document.getElementById('displayName' + idx).innerHTML;
  if(displayName.length > displayNameSizeLimit){
	  alert('The display name: ' + displayName + ' is too long. The allowed size is: ' + displayNameSizeLimit + ' Please modify it in the request file.');
	  return false;
  }
  
  
  
  var usrname_elmName = 'username' + idx;
  for( var i = 0; i < document./*form.username*/getElementsByName(usrname_elmName).length; i++ ){
    if( document./*form.username*/getElementsByName(usrname_elmName)[i].checked ){
      var username = "";
      
  	  //ADDED BLOCK - Processing for non-standard name
      if (document.getElementsByName(usrname_elmName)[i].value.indexOf("other" + idx)!= -1){
    	  var sname = document.getElementsByName(usrname_elmName)[i].value;
    	  username = document.getElementById('customName'+sname).value;
    	  document.getElementById('sAMAccountName'+idx).value = username;
      } else {
    	  username = document./*form.username*/getElementsByName(usrname_elmName)[i].value;
          document./*form.sAMAccountName*/getElementById('sAMAccountName'+idx).value = username;
      }
  	  
      // check if the username contains any special chars that ldap server doens't allow
  	  var regex = new RegExp('[\\,\\<\\>\\;\\=\\*\\[\\]\\|\\:\\~\\#\\+\\&\\%\\{\\}\\?\\\'\\"]', 'g');
  	  var temp = username.replace(regex, "");
  	  if(temp.length < username.length){
  		  alert('Username contains some forbid speical characters.\n' + 
  				'The special characters allowed to have in username are: ( ) . - _ ` ~ @ $ ^');
  		  return false;
  	  }
  	  if(username.trim().length == 0){
  		  alert('Username cannot be empty');
  		  return false;
  	  }
  	
  	  
      //BLOCK ENDS
      return true;
    }
  }
  
  alert("Please choose a username");
  return false;
}


/**
 * get the password (psw1) and the confirm password (psw2)
 * validate this password
 */
function validatePassword(idx){
	var elmName = 'pswradio' + idx;
	for(var i=0; i<document.getElementsByName(elmName).length; i++){
		if(document.getElementsByName(elmName)[i].checked){
			if(document.getElementsByName(elmName)[i].value === "GenPsw"){
				document.getElementById("password"+idx).value = "GenPsw";
				return true;
			} else if(document.getElementsByName(elmName)[i].value === "CustPsw"){
				var psw1 = document.getElementById("customPswA" + idx);
				var psw2 = document.getElementById("customPswB" + idx);
				
				if (psw1 != null && psw2 != null) {
					var validate = passwordValidator(psw1.value, psw2.value);
					if(validate==true){
						document.getElementById("password" + idx).value = psw1.value;
					}else{
						alert("The password is not incorrect. A valid password must be at least 10 characters with one lowercase alphabet, one uppercase alphabet and one number");
					}
					return validate;
				}
				
			}
		}
	}
	alert("Please choose a password for this user. A valid password must be at least 10 characters with one lowercase alphabet, one uppercase alphabet and one number");
	return false;
}

/**
 * accept the account request, POST the filename to AccepRequest servlet to do the accepting request
 */
function AcceptRequest(idx) {
  cleanupResultMsg();
  
  if(validateUsername(idx) && validatePassword(idx)){
    document.getElementById('accept' + idx).className = 'ButtonDisabled';
    document.getElementById('decline' + idx).className = 'ButtonDisabled';

    var url = "AcceptRequest?filename=" + encodeURIComponent(document.getElementById('filename'+idx).value)
    		 +"&action=accept&username="+ encodeURIComponent(document.getElementById('sAMAccountName'+idx).value)
    		 +"&psw=" + encodeURIComponent(document.getElementById("password" + idx).value);
    var ajax;
	if (window.XMLHttpRequest) {// code for IE7+, Firefox, Chrome, Opera, Safari
		ajax = new XMLHttpRequest();
	} else {// code for IE6, IE5
		ajax = new ActiveXObject("Microsoft.XMLHTTP");
	}
    
    ajax.open("POST", url, true);
    ajax.setRequestHeader("Content-Type","application/x-www-form-urlencoded");
	ajax.setRequestHeader("Accept", "text/xml, application/xml, text/plain");
	ajax.send('');
    ajax.onreadystatechange = function(){
    	if(ajax.readyState == 4){
    	    if(ajax.status == 200){
    	      results = ajax.responseText.split("|");
    	      
    	      if(results[0] == "true"){ // successfully accepting a request
    	      	document.getElementById('validation_msg').innerHTML =
    	          "<font color=\"#00FF00\">* "+results[1]+"</font><br />";
    	          
    	          $('#acctReqRow' + idx).remove(); // remove the request from the page
    	          
    	      }else{ // failed accepting a request
    	    	document.getElementById('validation_msg').innerHTML =
    	          "<font color=\"#FF0000\">* "+results[1]+"</font><br />";
    	    	document.getElementById('accept' + idx).className = 'Button';
		    	document.getElementById('decline' + idx).className = 'Button';
    	      }
    	    }else{
    	      document.getElementById('validation_msg').innerHTML =
    	    	  "<font color=\"#FF0000\">* The system encountered an error while processing, please try again later.</font><br />";
    	      document.getElementById('accept' + idx).className = 'Button';
		      document.getElementById('decline' + idx).className = 'Button';
    	    }
    	}
    }
    
  }
}
 
/**
 * decline the requeest, POST the filename to AcceptRequestServlet to do the declining request
 */
function DeclineRequest(idx) {
  cleanupResultMsg();
	 
  document.getElementById('accept' + idx).className = 'ButtonDisabled';
  document.getElementById('decline' + idx).className = 'ButtonDisabled';

  var url = "AcceptRequest?filename=" + encodeURIComponent(document.getElementById('filename'+idx).value)
		  +"&action=decline";
  
  var ajax;
  if (window.XMLHttpRequest) {// code for IE7+, Firefox, Chrome, Opera, Safari
	ajax = new XMLHttpRequest();
  } else {// code for IE6, IE5
	ajax = new ActiveXObject("Microsoft.XMLHTTP");
  }
  ajax.open("POST", url, true);
  ajax.setRequestHeader("Content-Type","application/x-www-form-urlencoded");
  ajax.setRequestHeader("Accept", "text/xml, application/xml, text/plain");
  ajax.send('');
  
  ajax.onreadystatechange = function(){
	  if(ajax.readyState == 4){
		    if(ajax.status == 200){
		      results = ajax.responseText.split("|");
		      
		      if(results[0] == "true"){ // successfully process the declining
		      	document.getElementById('validation_msg').innerHTML =
		          "<font color=\"#00FF00\">* "+results[1]+"</font><br />";
		          
		      	$('#acctReqRow' + idx).remove(); // remove the request from the page
		      	
		      	
		      }else{ // unsuccessfully process the declining
		    	document.getElementById('validation_msg').innerHTML =
		          "<font color=\"#FF0000\">* "+results[1]+"</font><br />";
		    	document.getElementById('accept' + idx).className = 'Button';
		    	document.getElementById('decline' + idx).className = 'Button';
		      }
		    }else{
		      document.getElementById('validation_msg').innerHTML =
		    	  "<font color=\"#FF0000\">* The system encountered an error while processing, please try again later.</font><br />";
		      document.getElementById('accept' + idx).className = 'Button';
		      document.getElementById('decline' + idx).className = 'Button';
		    }
	  }
  }
}
 

 /**
 * Handle the response (result) from the AcceptRequestServlet
 */
/* 
  
no longer needed
  
 
 function handleHttpResponse(){
  cleanupResultMsg();
  
  if(ajax.readyState == 4){
    if(ajax.status == 200){
      results = ajax.responseText.split("|");
      if(results[0] == "true"){
      	document.getElementById('validation_msg').innerHTML =
          "<font color=\"#00FF00\">* "+results[1]+"</font><br />";
      }else{
    	document.getElementById('validation_msg').innerHTML =
          "<font color=\"#FF0000\">* "+results[1]+"</font><br />";
      }
    }else{
      document.getElementById('validation_msg').innerHTML =
    	  "<font color=\"#FF0000\">* The system encountered an error while processing, please try again later.</font><br />";
    }
  }
}	*/
 
/**
 * If admin user choose to generate a random password. then disable the custom (manual) password boxes 
 */
 function GenPswTick(idx){
	 document.getElementById("customPswA" + idx).setAttribute("disabled", true);
	 document.getElementById("customPswB" + idx).setAttribute("disabled", true);
 }
 
/**
 * If admin user choose to type in the password manually then remove the disable attribute from the password boxes
 */
 function CustPswTick(idx){
	 document.getElementById("customPswA" + idx).removeAttribute("disabled");
	 document.getElementById("customPswB" + idx).removeAttribute("disabled");
	 
 }
	
    </script>
  </head>
  <body>

<%accounts.refresh();%>

    <table align="center" border="0" style="border-color: #ef7224" cellspacing="1">
      <tr>
        <td bgcolor="#ef7224">
          <table bgcolor="#ffffff" width="700px">
            <tr align="center">
              <td align="center">
                <div align="center"><img src="css/images/logos/supporttracker.gif" alt="Support Tracker Logo" /></div>
                <h1>Account Requests Management</h1>
                <img src="./css/images/swish.gif" alt="#" />
                <br />
<%if(session.getAttribute("error") != null){ %>
                <span class="error" style="float: center; width=100%; text-align: center">
	<%=session.getAttribute("error") %>
	<%session.removeAttribute("error");%>
                </span>
<%}else if(session.getAttribute("isAdmin") == null){ %>
                <span class="error" style="float: center; width=100%; text-align: center">Only support administrators can access this page.</span>

<%}else{ %>
                <span id="validation_msg" style="float:left; width: 500px; text-align: left"></span>
                
                
                
                
                
                
                
                
                
                
                
                <div style="width: 600px; padding: 5px; margin: 5px auto ";>

	<%TreeMap<String, List<Map<String, String>>> reqListMap = accounts.getRequests();%>
		<%if( reqListMap.size() == 0 ){%>
                  <span style="float: center; text-align: center; font-size: 16px">There are no pending account requests.</span>
		<%}else{
			int i = -1; // used to defined which request is clicked (on html page)
			
			for( Map.Entry<String, List<Map<String, String>>> rqls : reqListMap.entrySet() ){
				
				String responsibleStaff = rqls.getKey();
				%>
				
				 
				 <!-- print a list of request. this is the header of each group -->
				 <div class="row"> <h3> Responsible staff: <%=responsibleStaff%> </h3> </div>
				 
					
					
				<%
				// start listing each request as a drop down row
				
				List<Map<String, String>> rqstList = rqls.getValue();
				
				for(Map<String, String> singleRequest : rqstList){
					i++; // increament the id of the request ID (used to defined request that is clicked - on html page)
				
				//HashMap<String, String> singleRequest = reqList.get(i);
					String id = (i < 10 ? "0"+i : ""+i);
					String nodeID = "node"+id;
					String imageID = "image"+id;
					String filenameID = "filename"+id;
					String acceptID = "accept"+id;
					String declineID = "decline"+id;
					String requestID = "request"+id;	%>
	                 <div class="row" id="acctReqRow<%=id%>">
	                    <div id="<%=nodeID %>" class="CollapseRegionLink" style="text-align: left;" onclick="applyClick('<%=id %>'); ">
	                      
	                      <table style="width:480px">
	                      	<tr>
	                      		<td style="width:8em">
	                      			<input type="hidden" id="<%=filenameID %>" value="<%=singleRequest.get("filename") %>" />
	                      			<img id="<%=imageID %>" class="node_c" src="./css/images/clear.gif" style="border-width:0px;vertical-align:middle;" alt="#" />&nbsp;
	                      
	                      			<font color="#404040"> <%=singleRequest.get("createdDate") %> </font>
	                      		</td>
	                      		<td> 
	                      			<%= singleRequest.get("displayName") %> 
	                      			(<%=singleRequest.get("company") %>)
	                      		</td>
	                      	</tr>
	                      </table>
	                    </div>
	                    <div class="Buttons" style="float:right; text-align: center; clear: none;">
	                      <a class="Button" id="<%=acceptID %>" href="#" onclick="javascript: AcceptRequest('<%=id %>');">Accept</a>
	                      <a class="Button" id="<%=declineID %>" href="#" onclick="javascript: DeclineRequest('<%=id %>');">Decline</a>
	                    </div>
	                    
	                    
	                    <div id="<%=requestID %>" class="CollapsibleSection" style="display:none;">
	                    
	                      <div class="row">
	                        <span class="label3">Username:</span>
	                        <span class="value3">
	                          <form name="form" id="form">
	                          	<input type="radio" name="username<%=id %>" value="other<%= id %>" id="customNameotherRadio<%= id %>"/>
								<input type="text" id="customNameother<%= id %>" onkeyup="document.getElementById('customNameotherRadio<%= id %>').checked = true"></input><br />
								
	                            <input type="hidden" id="sAMAccountName<%=id%>" value="" />
					<%String[] names = accounts.getAvailableNames(singleRequest.get("givenName"), singleRequest.get("sn"));
					for( int j = 0; j < names.length; j++ ){  %>
	                            <input type="radio" name="username<%=id %>" value="<%=names[j] %>" /><%=names[j] %><br />
					<%}%>
	                          </form>
	                        </span>
	                      </div>
	                      
	                      <div class="row">
	                      	<span class="label3">User's Password</span>
	                      	<span class="value3">
	                      		<form name="form2" id="form2">
	                      			<input type="hidden" id="password<%=id%>" value="" />
						<% String mobile = singleRequest.get("mobile");
	                       if(accounts.isThisMobileNumberValid(mobile)){ %>
	                      			<input type="radio" name="pswradio<%=id%>" value="GenPsw" onclick="GenPswTick('<%=id%>')">Random password and sms</input><br />
	                      <%}%>
	                      
	                      			<input type="radio" name="pswradio<%=id%>" value="CustPsw" onclick="CustPswTick('<%=id%>')" id="pswB<%=id%>">Assign a password</input>
	                      			
	                      			<table>
	                      			<tr>
	                      				<td width="15em"></td>
	                      				<td>Password:</td>
	                      				<td><input type="password" id="customPswA<%=id%>" onkeyup="document.getElementById('pswB<%=id%>').checked=true"></input><td/>
	                      			</tr>
	                      			<tr>
	                      				<td width="15em"></td>
	                      				<td>Confirm:</td>
	                      				<td><input type="password" id="customPswB<%=id%>" onkeyup="document.getElementById('pswB<%=id%>').checked=true"></input> </td>
	                      			</tr>
	                      			</table>
	                      		</form>
	                      	</span>
	                      </div>
	                      
	                      
	                      <div class="row">
	                        <span class="label3">First Name:</span>
	                        <span class="value3"><%=singleRequest.get("givenName") %></span>
	                      </div>
	                      
	                      <div class="row">
	                        <span class="label3">Lastname:</span>
	                        <span class="value3"><%=singleRequest.get("sn") %></span>
	                      </div>
	                      
	                      <div class="row">
	                        <span class="label3">Display Name:</span>
	                        <span class="value3" id="displayName<%=id%>"><%=singleRequest.get("displayName") %></span>
	                      </div>
	                      
	                      <div class="row">
	                        <span class="label3">Phone:</span>
	                        <span class="value3"><%=singleRequest.get("telephoneNumber") %></span>
	                      </div>
	                      
	                      <div class="row">
	                        <span class="label3">Fax:</span>
	                        <span class="value3"><%=singleRequest.get("facsimileTelephoneNumber") %></span>
	                      </div>
	                      
	                      <div class="row">
	                        <span class="label3">Mobile:</span>
	                        <span class="value3"><%=singleRequest.get("mobile") %></span>
	                      </div>
	                      
	                      <div class="row">
	                        <span class="label3">Email:</span>
	                        <span class="value3"><%=singleRequest.get("mail") %></span>
	                      </div>
	                      
	                      <div class="row">
	                        <span class="label3">Position / Role:</span>
	                        <span class="value3"><%=singleRequest.get("description") %></span>
	                      </div>
	                      
	                      <div class="row">
	                        <span class="label3">Department:</span>
	                        <span class="value3"><%=singleRequest.get("department") %></span>
	                      </div>
	                      
	                      <div class="row">
	                        <span class="label3">Company:</span>
	                        <span class="value3"><%=singleRequest.get("company") %></span>
	                      </div>
	                      
	                      <div class="row">
	                        <span class="label3">No. / Street:</span>
	                        <span class="value3"><%=singleRequest.get("streetAddress") %></span>
	                      </div>
	                      
	                      <div class="row">
	                        <span class="label3">City:</span>
	                        <span class="value3"><%=singleRequest.get("l") %></span>
	                      </div>
	                      
	                      <div class="row">
	                        <span class="label3">State:</span>
	                        <span class="value3"><%=singleRequest.get("st") %></span>
	                      </div>
	                      
	                      <div class="row">
	                        <span class="label3">Postal Code:</span>
	                        <span class="value3"><%=singleRequest.get("postalCode") %></span>
	                      </div>
	                      
	                      <div class="row">
	                        <span class="label3">Country:</span>
	                        <span class="value3"><%=singleRequest.get("co") %></span>
	                      </div>
	                      
	                    </div>
	                  </div>
<%				}
			}	
		}	%>
                </div>
                
                
                
                
                
                
                
                
                
                
                
                <div align="center"><img src="./css/images/swish.gif" alt="#" /></div>
                <div align="center" class="disclaimer2">Having problems?<br />Email <a href="mailto:support@orionhealth.com">support@Orionhealth.com</a><br /><br /></div>
<%	} %>
              </td>
            </tr>
          </table>
        </td>
      </tr>
    </table>  
  </body>
</html>