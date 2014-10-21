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

    <link rel="stylesheet" href="./css/concerto.css" type="text/css" />
    <link rel="stylesheet" href="./css/general.css" type="text/css" />
    <link rel="shortcut icon" href="./css/images/oStar.ico" />
    <script src="./js/ajaxgen.js"></script>
    <script src="./js/jquery.js"></script>
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
	
var displayNameSizeLimit = <%=accounts.getDisplayNameSizeLimit()%>;
	
var id = '';

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
    	  document.getElementById('sAMAccountName').value = username;
      } else {
    	  username = document./*form.username*/getElementsByName(usrname_elmName)[i].value;
          document./*form.sAMAccountName*/getElementById('sAMAccountName').value = username;
      }
  	  
      // check if the username contains any special chars that ldap server doens't allow
  	  var regex = new RegExp('[\\,\\<\\>\\;\\=\\*\\[\\]\\|\\:\\~\\#\\+\\&\\%\\{\\}\\?]', 'g');
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
 * accept the account request, POST the filename to AccepRequest servlet to do the accepting request
 */
function AcceptRequest(idx) {
  cleanupResultMsg();
  
  if(validateUsername(idx)){
    document.getElementById('accept' + idx).className = 'ButtonDisabled';
    document.getElementById('decline' + idx).className = 'ButtonDisabled';

    var url = "AcceptRequest?filename=" + encodeURIComponent(document.getElementById('filename'+idx).value)+"&action=accept&username="
    		+encodeURIComponent(document./*form.sAMAccountName*/getElementById('sAMAccountName').value);
    ajax.open("POST", url, true);
    ajax.onreadystatechange = handleHttpResponse;
    ajax.send('');
  }
}
 
/**
 * decline the requeest, POST the filename to AcceptRequestServlet to do the declining request
 */
function DeclineRequest(idx) {
  cleanupResultMsg();
	 
  document.getElementById('accept' + idx).className = 'ButtonDisabled';
  document.getElementById('decline' + idx).className = 'ButtonDisabled';

  var url = "AcceptRequest?filename=" + encodeURIComponent(document.getElementById('filename'+idx).value)+"&action=decline";
  ajax.open("POST", url, true);
  ajax.onreadystatechange = handleHttpResponse;
  ajax.send('');
}
 

 /**
 * Handle the response (result) from the AcceptRequestServlet
 */
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
	                 <div class="row">
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
	                            <input type="hidden" id="sAMAccountName" value="" />
					<%String[] names = accounts.getAvailableNames(singleRequest.get("givenName"), singleRequest.get("sn"));
					for( int j = 0; j < names.length; j++ ){  %>
	                            <input type="radio" name="username<%=id %>" value="<%=names[j] %>" /><%=names[j] %><br />
					<%}%>
								<input type="radio" name="username<%=id %>" value="other<%= id %>" id="customNameotherRadio<%= id %>"/>
								<input type="text" id="customNameother<%= id %>" onblur="document.getElementById('customNameotherRadio<%= id %>').checked = true"></input><br />
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