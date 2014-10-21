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
      <script type="text/javascript" language="javascript">


/* Convert an input string, 
 and produced an output which its first character is Uppercase */
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


<%int dsplSizeLimit = accounts.getDisplayNameSizeLimit();%>
var displayNameSizeLimit = <%=dsplSizeLimit%>;

/*
 * Validate all the entries of the Account Request form
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


function doDisplayName(){
  document.form.displayName.value = document.form.givenName.value+" "+document.form.sn.value;
}


/* reset the form into default*/
function ResetForm(){
  document.getElementById('validation_msg').innerHTML = "";
  document.form.reset();
  return false;
}


/* submit the account detail */
function SubmitForm(){
  if(validateEntries()){
    document.form.submit();
    return true;
  }
  return false;
}


function keyPressed(event){
	// look for window.event in case event isn't passed in
	if(typeof event == 'undefined' && window.event) event= window.event;
	if(event.keyCode == 13) document.getElementById('submitBtn').click();
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
<%if( session.getAttribute("error") != null ){  %>
                 <span class="error" style="float: center; width=100%; text-align: center">
	<%=session.getAttribute("error") %>
				 </span>
	<%session.removeAttribute("error");  %>  
<%}else{ %>
                 <span class="error" id="validation_msg" style="float:left; width=100%; text-align: left"></span>
                 <div style="width: 500px; padding: 5px; margin: 5px auto ">
                   <form name="form" method="post" action="AccountRequest" onsubmit="return validateEntries();">
                     <div class="row">
                       <span class="label2">First Name:</span>
                       <span class="formw">
                         <input type="text" id="givenName" name="givenName" size="<%=dsplSizeLimit%>" maxlength="<%=dsplSizeLimit%>" tabindex="1" onblur="this.value=firstCharUp(this.value); doDisplayName();" onkeypress="keyPressed(event);"/>
                         <input type="hidden" id="userDN" name="userDN" value="<%=session.getAttribute("userDN") %>"></input>
                         <input type="hidden" id="company" name="company" value="<%=session.getAttribute("company") %>"></input>
                       </span>
                       <span class="required">*</span>
                     </div>
                     <div class="row">
                       <span class="label2">Last Name:</span>
                       <span class="formw">
                         <input type="text" id="sn" name="sn" size="<%=dsplSizeLimit%>" maxlength="<%=dsplSizeLimit%>" tabindex="2" onblur="this.value=firstCharUp(this.value); doDisplayName();" onkeypress="keyPressed(event);"/>
                       </span>
                       <span class="required">*</span>
                     </div>
                     <div class="row">
                       <span class="label2">Display Name:</span>
                       <span class="formw">
                         <input readonly="readonly" type="text" id="displayName" name="displayName" size="<%=dsplSizeLimit%>" maxlength="<%=dsplSizeLimit%>" onkeypress="keyPressed(event);"/>
                       </span>
                     </div>
                     <div class="row">
                       <span class="label2">Position / Role:</span>
                       <span class="formw">
                         <input type="text" id="description" name="description" size="<%=dsplSizeLimit%>" maxlength="<%=dsplSizeLimit%>" tabindex="3" onkeypress="keyPressed(event);"/>
                       </span>
                       <span class="required">*</span>
                     </div>
                     <div class="row">
                       <span class="label2">Department:</span>
                       <span class="formw">
                         <input type="text" id="department" name="department" size="<%=dsplSizeLimit%>" maxlength="<%=dsplSizeLimit%>" tabindex="4" onkeypress="keyPressed(event);"/>
                       </span>
                       <span class="required">*</span>
                     </div>
                     <div class="row">
                       <span class="label2">No. / Street:</span>
                       <span class="formw">
                         <input type="text" id="streetAddress" name="streetAddress" size="50" maxlength="50" tabindex="5" onkeypress="keyPressed(event);"/>
                       </span>
                     </div>
                     <div class="row">
                       <span class="label2">City:</span>
                       <span class="formw">
                         <input type="text" id="l" name="l" size="<%=dsplSizeLimit%>" maxlength="<%=dsplSizeLimit%>" tabindex="6" onkeypress="keyPressed(event);"/>
                       </span>
                     </div>
                     <div class="row">
                       <span class="label2">State:</span>
                       <span class="formw">
                         <input type="text" id="st" name="st" size="<%=dsplSizeLimit%>" maxlength="<%=dsplSizeLimit%>" tabindex="7" onkeypress="keyPressed(event);"/>
                       </span>
                       <span class="required">*</span>
                     </div>
                     <div class="row">
                       <span class="label2">Postal Code:</span>
                       <span class="formw">
                         <input type="text" id="postalCode" name="postalCode" size="<%=dsplSizeLimit%>" maxlength="<%=dsplSizeLimit%>" tabindex="8" onkeypress="keyPressed(event);"/>
                       </span>
                     </div>
                     <div class="row">
                       <span class="label2">Country:</span>
                       <span class="formw">
                         <select id="c" name="c" tabindex="9" onkeypress="keyPressed(event);">
                           <option value="">Please select one from the list</option>
	<%TreeMap<String,String> countriesMap = countries.getCountries();
	for(Map.Entry<String, String>entry:countriesMap.entrySet()){
        String countryName = entry.getKey();
	    String countryCode = entry.getValue();  %>
	                       <option value="
	    <%=countryCode %>">
	    <%=countryName %>
	        				</option>
	<%}%>
                         </select>
                       </span>
                       <span class="required">*</span>
                     </div>
                     <div class="row">
                       <span class="label2">Phone:</span>
                       <span class="formw">
                         <input type="text" id="telephoneNumber" name="telephoneNumber" size="<%=dsplSizeLimit%>" maxlength="<%=dsplSizeLimit%>" tabindex="10" onkeypress="keyPressed(event);"/>
                       </span>
                       <span class="required">*</span>
                     </div>
                     <div class="row">
                       <span class="label2">Fax:</span>
                       <span class="formw">
                         <input type="text" id="facsimileTelephoneNumber" name="facsimileTelephoneNumber" size="<%=dsplSizeLimit%>" maxlength="<%=dsplSizeLimit%>" tabindex="11" onkeypress="keyPressed(event);" />
                       </span>
                     </div>
                     <div class="row">
                       <span class="label2">Mobile:</span>
                       <span class="formw">
                         <input type="text" id="mobile" name="mobile" size="<%=dsplSizeLimit%>" maxlength="<%=dsplSizeLimit%>" tabindex="12" onkeypress="keyPressed(event);"/>
                       </span>
                     </div>
                     <div class="row">
                       <span class="label2">Email:</span>
                       <span class="formw">
                         <input type="text" id="mail" name="mail" size="50" maxlength="50" tabindex="13" onkeypress="keyPressed(event);"/>
                       </span>
                       <span class="required">*</span>
                     </div>
                     <div class="row"></div>
                     <div class="Buttons" style="text-align: center; clear: none; padding-top: 20px; width: 180px; height: 20px;">
                       <a class="Button" href="#" id="submitBtn" onclick="javascript: SubmitForm()">Submit</a>
                       <a class="Button" href="#" onclick="javascript: ResetForm()">Reset</a>
                     </div>
                   </form>
                 </div>
	<%if( session.getAttribute("passed") != null){ %>
	                 <div align="center" class="passed">
	    <%=session.getAttribute("passed")%>
	    			</div>
		<%session.removeAttribute("passed");
	}
	if( session.getAttribute("failed") != null){ %>
	                 <div align="center" class="failed">
        <%=session.getAttribute("failed")%>
        			 </div>
		<%session.removeAttribute("failed");
	}%>
	                 <div align="center"><img src="./css/images/swish.gif" alt="#" /></div>
	                 <div align="center" class="disclaimer2">Having problems?<br />Email <a href="mailto:support@orionhealth.com">support@Orionhealth.com</a><br /><br /></div>
<%}%>
               </td>
             </tr>
           </table>
         </td>
       </tr>
     </table>  
  </body>
</html>