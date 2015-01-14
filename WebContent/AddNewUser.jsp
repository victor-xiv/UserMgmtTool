<?xml version="1.0" encoding="ISO-8859-1" ?>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
    <title>Account Request Form</title>
    <link rel="stylesheet" href="./css/concerto.css" type="text/css" />
      <link rel="stylesheet" href="css/general.css" type="text/css" />
      <link rel="shortcut icon" href="./css/images/oStar.ico" />
      <jsp:useBean id="countries" class="beans.Countries" scope="session" />
      <jsp:useBean id="accounts" class="beans.AccountRequestsBean" scope="session" />
      
      <%@ page import="java.util.TreeMap" %>
      <%@ page import="java.util.Map" %>
      <%@ page import="java.util.Set" %>
      <%@ page import="tools.SupportTrackerJDBC" %>
      <%@ page import="ldap.LdapTool" %>
      <%@ page import="ldap.LdapProperty" %>
      <%@ page import="ldap.LdapConstants" %>
      
      <script src="./js/validator.js"></script>
      <script src="./js/jquery.js"></script>
      <script type="text/javascript" language="javascript">

function firstCharUp(input){
  if(input.length > 1){
    var firstLetter = input.charAt(0).toUpperCase();
    var restOfWord = input.substring(1, input.length);
    return firstLetter + restOfWord;
  }else if(input.length == 1){
    return input.toUpperCase();
  }else{
    return input;
  }
}

//used to limit the max number of chars for the fullname
<%int dsplSizeLimit = accounts.getDisplayNameSizeLimit();%>
var displayNameSizeLimit = <%=dsplSizeLimit%>;


/*
 * Validate all the entries of the new account that is about to be added
 */
function validateEntries(){
  var validated = true;
  var theFocus = '';
  
  //check the size of the displayName
  // because SPT DB has a limit size for this column
  // SPT-839
  var displayName = document.getElementById('displayName').value;
  if(displayName.length > displayNameSizeLimit){
	  alert('The display name: ' + displayName + ' is too long. The allowed size is: ' + displayNameSizeLimit);
	  document.getElementById('displayName').focus();
	  return false;
  }
  
  // check if the username contains any special chars that ldap server doens't allow
  var regex = new RegExp('[\\,\\<\\>\\;\\=\\*\\[\\]\\|\\:\\~\\#\\+\\&\\%\\{\\}\\?\\\'\\"]', 'g');
  var username = document.getElementById('sAMAccountName').value;
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
	  
  
  document.getElementById('validation_msg').innerHTML = "";
  if(document.getElementById('givenName').value == ""){
    document.getElementById('validation_msg').innerHTML = "* Please enter the first name<br/>";
    theFocus = 'givenName';
    validated = false;
  }
  if(document.getElementById('sn').value == ""){
    document.getElementById('validation_msg').innerHTML += "* Please enter the last name<br/>";
    if(theFocus == '')
      theFocus = 'sn';
    validated = false;
  }
  if(document.getElementById('department').value == ""){
    document.getElementById('validation_msg').innerHTML += "* Please enter the department<br/>";
    if(theFocus == '')
      theFocus = 'department';
    validated = false;
  }
  if(document.getElementById('description').value == ""){
    document.getElementById('validation_msg').innerHTML += "* Please enter the role of the person<br/>";
    if(theFocus == '')
      theFocus = 'description';
    validated = false;
  }
  if(document.getElementById('st').value == ""){
    document.getElementById('validation_msg').innerHTML += "* Please enter the state for the address<br/>";
    if(theFocus == '')
      theFocus = 'st';
    validated = false;
  }
  if(document.getElementById('c').value == ""){
    document.getElementById('validation_msg').innerHTML += "* Please select a country for the address<br/>";
    if(theFocus == '')
      theFocus = 'co';
    validated = false;
  }
  if(document.getElementById('telephoneNumber').value == ""){
    document.getElementById('validation_msg').innerHTML += "* Please enter a contact phone number<br/>";
    if(theFocus == '')
      theFocus = 'telephoneNumber';
    validated = false;
  }
  if(document.getElementById('mail').value == ""){
    document.getElementById('validation_msg').innerHTML += "* Please enter an email<br/>";
    if(theFocus == '')
    theFocus = 'mail';
    validated = false;
  }
  if(!validated)
    document.getElementById(theFocus).focus();
  return validated;
}



//generate displayname (fullname) = givenName + firstname
function doDisplayName(){
  document.form.displayName.value = document.form.givenName.value+" "+document.form.sn.value;
}


/* reset the form into default*/
function ResetForm(){
  document.getElementById('validation_msg').innerHTML = "";
  document.getElementById("passed").innerHTML="";
  document.getElementById("failed").innerHTML="";
  document.form.reset();
  return false;
}



/**
 * submit the form to AddUserServlet 
 */
function SubmitForm(){
  if(validateEntries()){
	  var psw = validatePassword();
	  if(psw==="false"){
		  return false;
	  } else {
		  // start processing. produce a message telling user that the request is being processed
		  document.getElementById('failed').innerHTML = "";
		  document.getElementById('passed').innerHTML = "Processing request...";
		  var url = prepareUrlParams();
		  url += "&password01=" + psw; 
		  
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
		    		// remove "Processing request..." message from the page and update the result message
		    		document.getElementById('passed').innerHTML = "";
		    	    if(ajax.status == 200){
		    	      results = ajax.responseText.split("|");
		    	      if(results[0] == "true"){
		    	      	 document.getElementById('passed').innerHTML = "<font color=\"#00FF00\">* "+results[1]+"</font><br />";
		    	      }else{
			    	     document.getElementById('failed').innerHTML = "<font color=\"red\">* "+results[1]+"</font><br />";		    	    	
		    	      }
		    	    }else{
		    	    	document.getElementById('failed').innerHTML = "<font color=\"red\">* "+results[1]+"</font><br />";
		    	    }
		    	}
		    }
	  }
  }
  return false;
}


/**
 * get the mobile number that is being typed by the user, and validate
 */
function validateMobile(){
	//document.getElementById("GenPswChk").checked=false;
	var mobile = document.getElementById("mobile").value;
	if(mobile != undefined && mobile != null){
		
		if(mobileValidator(mobile)){
			if(document.getElementById("GenPswChk").disabled){
				document.getElementById("GenPswChkDv").innerHTML = '<input tabindex="14" id="GenPswChk" type="checkbox" name="pswradio" value="GenPsw" onclick="disOrEnablePswBoxes()"><span>Random password and sms</span></input>';
			}
		} else {
			document.getElementById("GenPswChkDv").innerHTML = '<input tabindex="14" id="GenPswChk" type="checkbox" name="pswradio" value="GenPsw" disabled onclick="disOrEnablePswBoxes()"  ></input><span style="color:rgb(150,150,150)">Random password and sms</span>';
			enablePswBoxes();
		}
	}
//	alert('done');
	
}


/*
 * get password (psw1) and confirm password (psw2) and valdiate both of them
 */
function validatePassword(){
	var psw1, psw2;
	if(document.getElementById("GenPswChk").checked){
		return "GenPsw";
		
	} else {
		psw1 = document.getElementById("psw01").value;
		psw2 = document.getElementById("psw02").value;
		var validated = passwordValidator(psw1, psw2);
		if(validated){
			return psw1;
		} else {
			alert("The password is not incorrect. A valid password must be at least 10 characters with one lowercase alphabet, one uppercase alphabet and one number");
			return "false";
		}
	}
}

/*
 * this method will be called after click even applied on checkbox
 * if checked for "randomly generated password" =>  disable the manual password
 */
function disOrEnablePswBoxes(){ 
	if(document.getElementById("GenPswChk").checked == true){
		disbalePswBoxes();
	} else {
		enablePswBoxes();
	}
}

 /*
 * enable manual password boxes (this method called when the user untick the Random Generate Password option)
 */
function enablePswBoxes(){
	var psw01Lbl =document.getElementById("psw01Lbl");
	psw01Lbl.innerHTML = "Password:";
	psw01Lbl.setAttribute("style", "color:rgb(0,0,0)");
	var psw02Lbl =document.getElementById("psw02Lbl");
	psw02Lbl.innerHTML = "Confirm:";
	psw02Lbl.setAttribute("style", "color:rgb(0,0,0)");
	document.getElementById("psw01").removeAttribute("disabled");
	document.getElementById("psw02").removeAttribute("disabled");
	document.getElementById("psw01Rq").style.visibility = "visible";
	document.getElementById("psw02Rq").style.visibility = "visible";
}

 
/*
 * disable manual password boxes (this method called when the user tick the Random Generate Password option)
 */
function disbalePswBoxes(){
	var psw01Lbl =document.getElementById("psw01Lbl");
	psw01Lbl.innerHTML = "Password:";
	psw01Lbl.setAttribute("style", "color:rgb(150,150,150)");
	var psw02Lbl =document.getElementById("psw02Lbl");
	psw02Lbl.innerHTML = "Confirm:";
	psw02Lbl.setAttribute("style", "color:rgb(150,150,150)");
	document.getElementById("psw01").setAttribute("disabled", true);
	document.getElementById("psw02").setAttribute("disabled", true);
	document.getElementById("psw01Rq").style.visibility = "hidden";
	document.getElementById("psw02Rq").style.visibility = "hidden";
}


/**
 * a helper method that takes all the values from the input boxes
 * and prepare them as the url to POST to server
 */
function prepareUrlParams(){
	var params = ["sAMAccountName","givenName","sn","displayName","description","department",
	              "streetAddress", "l", "st", "c", "postalCode", "mail", "telephoneNumber", "facsimileTelephoneNumber",
	              "mobile", "userDN", "company"];
	var url = "AddUser?a=a";
	for(var i=0; i < params.length; i++){
		url += "&" + params[i] + "=" + encodeURIComponent(document.getElementById(params[i]).value);
	}
	return url;
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
      #psw01                     {width: 200px;}
      #psw02                     {width: 200px;}
    </style>
  </head>
  <body>
    <table align="center" border="0" style="border-color: #ef7224" cellspacing="1">
       <tr>
         <td bgcolor="#ef7224">
           <table bgcolor="#ffffff" width="600px">
             <tr align="center">
               <td align="center">
                 <div align="center"><img src="css/images/logos/supporttracker.gif" alt="Support Tracker Logo" /></div>
                 <h1>Account Request Form</h1>
                 <img src="./css/images/swish.gif" alt="#" />
                 <br />
<%	if( session.getAttribute("message") != null ){  %>
                 <span class="error" style="float: center; width=100%; text-align: center">
        <%=session.getAttribute("message") %>
        		 </span>
		<%  session.removeAttribute("message");  %>  
<%	}else{ %>
                 <span class="error" id="validation_msg" style="float:left; width=100%; text-align: left"></span>
                 <div style="width: 500px; padding: 5px; margin: 5px auto ";>
                   <form name="form" method="post" action="AddUser" onsubmit="return validateEntries();">
                     <div class="row">
                       <span class="label2">Username:</span>
                       <span class="formw">
                         <input type="text" id="sAMAccountName" name="sAMAccountName" size="<%=dsplSizeLimit%>" maxlength="<%=dsplSizeLimit%>" tabindex="1" onblur="this.value=firstCharUp(this.value); doDisplayName();"/>
                       </span>
                       <span class="required">*</span>
                     </div>
                     <div class="row">
                       <span class="label2">First Name:</span>
                       <span class="formw">
                         <input type="text" id="givenName" name="givenName" size="<%=dsplSizeLimit%>" maxlength="<%=dsplSizeLimit%>" tabindex="1" onblur="this.value=firstCharUp(this.value); doDisplayName();"/>
                         <input type="hidden" id="userDN" name="userDN" value="<%=session.getAttribute("userDN") %>"></input>
                         <input type="hidden" id="company" name="company" value="<%=request.getParameter("company") %>"></input>
                       </span>
                       <span class="required">*</span>
                     </div>
                     <div class="row">
                       <span class="label2">Last Name:</span>
                       <span class="formw">
                         <input type="text" id="sn" name="sn" size="<%=dsplSizeLimit%>" maxlength="<%=dsplSizeLimit%>" tabindex="2" onblur="this.value=firstCharUp(this.value); doDisplayName();"/>
                       </span>
                       <span class="required">*</span>
                     </div>
                     <div class="row">
                       <span class="label2">Display Name:</span>
                       <span class="formw">
                         <input readonly="readonly" type="text" id="displayName" name="displayName" size="<%=dsplSizeLimit%>" maxlength="<%=dsplSizeLimit%>"/>
                       </span>
                     </div>
                     <div class="row">
                       <span class="label2">Position / Role:</span>
                       <span class="formw">
                       
                       


<!-- if Orion Health staff, then this role will be a drop down menu where the roles list is generated from the LK_positionCode table of ST DB -->               
<% String company = request.getParameter("company");
   String defaultOrionStaffPosition = LdapProperty.getProperty(LdapConstants.DEFAULT_ORION_STAFF_POSITION);
   defaultOrionStaffPosition = defaultOrionStaffPosition==null ? "Orion Health Staff" : defaultOrionStaffPosition.trim();
   
   if(company.equalsIgnoreCase(LdapTool.ORION_HEALTH_NAME)){ %>
						<select id="description" name="description" tabindex="3" style="width:205px">
<% Set<String> allRoles = SupportTrackerJDBC.getAllPositionCodeNames();
		for(String role : allRoles){
			if(role.equalsIgnoreCase(defaultOrionStaffPosition)){
%>
							<option value="<%=defaultOrionStaffPosition %>" selected="selected"><%=defaultOrionStaffPosition %></option>

<%			} else {		%>
							<option value="<%=role %>"><%=role %></option>
<%			}
		}
%>
						</select>



<!-- if it is not Orion Health staff, then this role will be an input box -->
<% } else { %>
                        <input type="text" id="description" name="description" size="<%=dsplSizeLimit%>" maxlength="<%=dsplSizeLimit%>" tabindex="3"/>
<% } %>    


                       
                       
                       
                       
                       
                       </span>
                       <span class="required">*</span>
                     </div>
                     <div class="row">
                       <span class="label2">Department:</span>
                       <span class="formw">
                         <input type="text" id="department" name="department" size="<%=dsplSizeLimit%>" maxlength="<%=dsplSizeLimit%>" tabindex="4"/>
                       </span>
                       <span class="required">*</span>
                     </div>
                     <div class="row">
                       <span class="label2">No. / Street:</span>
                       <span class="formw">
                         <input type="text" id="streetAddress" name="streetAddress" size="50" maxlength="50" tabindex="5"/>
                       </span>
                     </div>
                     <div class="row">
                       <span class="label2">City:</span>
                       <span class="formw">
                         <input type="text" id="l" name="l" size="<%=dsplSizeLimit%>" maxlength="<%=dsplSizeLimit%>" tabindex="6"/>
                       </span>
                     </div>
                     <div class="row">
                       <span class="label2">State:</span>
                       <span class="formw">
                         <input type="text" id="st" name="st" size="<%=dsplSizeLimit%>" maxlength="<%=dsplSizeLimit%>" tabindex="7"/>
                       </span>
                       <span class="required">*</span>
                     </div>
                     <div class="row">
                       <span class="label2">Postal Code:</span>
                       <span class="formw">
                         <input type="text" id="postalCode" name="postalCode" size="<%=dsplSizeLimit%>" maxlength="<%=dsplSizeLimit%>" tabindex="8"/>
                       </span>
                     </div>
                     <div class="row">
                       <span class="label2">Country:</span>
                       <span class="formw">
                         <select id="c" name="c" tabindex="9">
                           <option value="">Please select one from the list</option>
<%  TreeMap<String,String> countriesMap = countries.getCountries();
    for(Map.Entry<String, String>entry:countriesMap.entrySet()){
        String countryName = entry.getKey();
        String countryCode = entry.getValue();  %>
                           <option value="<%=countryCode %>"><%=countryName %></option>
<%  }  %>
                         </select>
                       </span>
                       <span class="required">*</span>
                     </div>
                     <div class="row">
                       <span class="label2">Email:</span>
                       <span class="formw">
                         <input type="text" id="mail" name="mail" size="50" maxlength="50" tabindex="10"/>
                       </span>
                       <span class="required">*</span>
                     </div>
                     <div class="row">
                       <span class="label2">Phone:</span>
                       <span class="formw">
                         <input type="text" id="telephoneNumber" name="telephoneNumber" size="<%=dsplSizeLimit%>" maxlength="<%=dsplSizeLimit%>" tabindex="11" placeholder="+64-21-3627893 ext 32"/>
                       </span>
                       <span class="required">*</span>
                     </div>
                     <div class="row">
                       <span class="label2">Fax:</span>
                       <span class="formw">
                         <input type="text" id="facsimileTelephoneNumber" name="facsimileTelephoneNumber" size="<%=dsplSizeLimit%>" maxlength="<%=dsplSizeLimit%>" tabindex="12"/>
                       </span>
                     </div>
                     <div class="row">
                       <span class="label2">Mobile:</span>
                       <span class="formw">
                         <input type="text" id="mobile" name="mobile" size="<%=dsplSizeLimit%>" maxlength="<%=dsplSizeLimit%>" 
                         tabindex="13" onkeyup="validateMobile()" placeholder="+64-21-3627893"/>
                       </span>
                     </div>
                     
                     
                     
                     <br/><br/>
                     
                     <div class="row" id="GenPswChkDv">
                     	<input tabindex="14" id="GenPswChk" type="checkbox" name="pswradio" value="GenPsw" disabled onclick="disOrEnablePswBoxes()"  ></input>
                     	<span style="color:rgb(150,150,150)">Random password and sms</span>
                     </div>
                     
                     <br/>
                     
                     <div class="row">
                       <span class="label2" id="psw01Lbl">Password:</span>
                       <span class="formw">
                       		<input type="password" id="psw01" size="<%=dsplSizeLimit%>"
                      		 tabindex="15" onkeypress="document.getElementById('GenPswChk').checked=false"/></span>
                       <span class="required" id="psw01Rq">*</span>
                     </div>
                     
                     <div class="row">
                       <span class="label2" id="psw02Lbl">Confirm:</span>
                       <span class="formw">
                       		<input type="password" id="psw02" size="<%=dsplSizeLimit%>"
                      		 tabindex="16" onkeypress='document.getElementById("GenPswChk").checked=false'/></span>
                       <span class="required" id="psw02Rq">*</span>
                     </div>
                     
                     
                     
                     <div class="row"></div>
                     <div class="Buttons" style="text-align: center; clear: none; padding-top: 20px; width: 180px; height: 20px;">
                       <a class="Button" tabindex="17" href="#" onclick="javascript: SubmitForm()">Submit</a>
                       <a class="Button" tabindex="18" href="#" onclick="javascript: ResetForm()">Reset</a>
                     </div>
                   </form>
                 </div>
                 
                 
                 
<% if (request.getParameter("company")!=null){ %>   
                <div class='row'>
					<a class="Button" href="OrganisationDetails.jsp?name=<%=java.net.URLEncoder.encode(request.getParameter("company"))%>">Back to <b><%=request.getParameter("company")%></b></a>
					<br/><br/>
				</div>
<% } %>
				
                 
                 
                 <div align="center" id="passed" class="passed">
<%	if( session.getAttribute("passed") != null){ %>
                 <%=session.getAttribute("passed")%>
<%		session.removeAttribute("passed");
	} %>
				</div>
	 			
	 			<div align="center" id="failed" class="failed">
<%  if( session.getAttribute("failed") != null){ %>
                <%=session.getAttribute("failed")%>
<%		session.removeAttribute("failed");
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