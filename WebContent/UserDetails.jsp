<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <title>Person: <%=request.getParameter("dn") %></title>
    <script type="text/javascript" language="javascript" src="./js/ajaxgen.js"></script>
    <link rel="stylesheet" href="./css/concerto.css" type="text/css" />
    <link rel="stylesheet" href="./css/general.css" type="text/css" />
    <jsp:useBean id="user" class="beans.LdapUser" scope="page" />
    <jsp:useBean id="countries" class="beans.Countries" scope="session" />
    <jsp:useBean id="groups" class="beans.LdapUserGroups" scope="session" />
    <%@ page import="java.util.ArrayList" %>
    <%@ page import="java.util.TreeMap" %>
    <%@ page import="java.util.Map" %>
    <%@ page import="java.util.Set" %>
    <%@ page import="ldap.LdapTool" %>
	<%@ page import="javax.naming.directory.Attribute" %>
	<%@ page import="javax.naming.directory.Attributes" %>
	<%@ page import="javax.naming.NamingEnumeration" %>
	<%@ page import="java.io.FileNotFoundException" %>
  	<%@ page import="javax.naming.NamingException" %>
  	
  	
    <% user.processUserDN(request.getParameter("dn")); %>
    <script type="text/javascript" language="javascript">
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
    function validateEntries() {
        var validated = true;
        var theFocus = '';
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
            document.getElementById('validation_msg').innerHTML += "* Please enter the role of the person<br/>";
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
    function UpdateForm() {
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
    	location.reload(true);
    }
    function BackForm() {
    	history.back();
    }
    function SubmitForm() {
        if (validateEntries()) {
            document.form.submit();
            return true;
        }
        return false;
    }
    function SubmitGroupForm() {
	  	document.getElementById("addto").submit();
		    return true;
	}
    function ToggleStatus() {
    	var url = "UpdateUserStatus?dn="+encodeURIComponent('<%=request.getParameter("dn") %>');
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
                    document.getElementById('validation_msg').innerHTML +=
                        "<font color=\"#00FF00\">* "+results[1]+"</font><br />";
                    if (document.getElementById('accstatus').innerHTML == "Disabled") {
                        document.getElementById('accstatus').innerHTML = "Enabled";
                    }else{
                        document.getElementById('accstatus').innerHTML = "Disabled";
                    }
                }else{
                    document.getElementById('validation_msg').innerHTML +=
                        "<font color=\"#FF0000\">* "+results[1]+"</font><br />";
                }
            }else{
    		    document.getElementById('validation_msg').innerHTML +=
    		        "<font color=\"#FF0000\">* The system encountered an error while processing, please try again later.</font><br />";
    	    }
        }
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
    </style>
  </head>
  <body>
    <table align="center" border="0" style="border-color: #ef7224" cellspacing="1">
      <tr>
        <td bgcolor="#ef7224">
          <table bgcolor="#ffffff" width="600px">
            <tr align="center">
              <td align="center">
                <div align="center"><img src="http://supporttracker.orionhealth.com/concerto/images/logos/supporttracker.gif" alt="#" /></div>
                <h1><%=user.getDisplayName() %></h1>
                <span><a href="ChangePassword">Change Password</a></span>
                <img src="./css/images/swish.gif" alt="There should be an image here...." />
<%  if(session.getAttribute("error") != null){ %>
                <div class="row">
                  <div class="error" style="float: center; width=100%; text-align: center"><%=session.getAttribute("error") %></div>
                </div>
<%  }else if(session.getAttribute("isAdmin") == null){ %>
                <div class="error" style="float: center; width=100%; text-align: center">Only support administrators can access this page.</div>
<%  }else{ %>
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
                  <form name="form" method="post" action="UpdateUserDetails" onsubmit="return validateEntries();">
                    <div class="row">
                      <span class="label2">Username:</span>
                      <span class="formw">
                        <input disabled="disabled" type="text" id="sAMAccountName" name="sAMAccountName" size="20" maxlength="20" tabindex="1"
                         value="<%=user.getUsername() %>" />
                        <input type="hidden" name="dn" value="<%=request.getParameter("dn") %>" />
<%	session.setAttribute("dn", request.getParameter("dn")); %>
                      </span>
                      <span class="required">*</span>
                    </div>
                    <div class="row">
                      <span class="label2">First Name:</span>
                      <span class="formw">
                        <input disabled="disabled" type="text" id="givenName" name="givenName" size="20" maxlength="20" tabindex="2"
                         value="<%=user.getFirstName() %>" onblur="this.value=firstCharUp(this.value); doDisplayName();" />
                      </span>
                      <span class="required">*</span>
                    </div>
                    <div class="row">
                      <span class="label2">Last Name:</span>
                      <span class="formw">
                        <input disabled="disabled" type="text" id="sn" name="sn" size="20" maxlength="20" tabindex="3"
                         value="<%=user.getLastName() %>" onblur="this.value=firstCharUp(this.value); doDisplayName();" />
                      </span>
                      <span class="required">*</span>
                    </div>
                    <div class="row">
                      <span class="label2">Display Name:</span>
                      <span class="formw">
                        <input disabled="disabled" readonly="readonly" type="text" id="displayName" name="displayName" size="20" maxlength="20"
                         value="<%=user.getDisplayName() %>" />
                      </span>
                    </div>
                    <div class="row">
                      <span class="label2">Position / Role:</span>
                      <span class="formw">
                        <input disabled="disabled" type="text" id="description" name="description" size="20" maxlength="20" tabindex="4"
                         value="<%=user.getDescription() %>" />
                      </span>
                      <span class="required">*</span>
                    </div>
                    <div class="row">
                      <span class="label2">Department:</span>
                      <span class="formw">
                        <input disabled="disabled" type="text" id="department" name="department" size="20" maxlength="20" tabindex="5"
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
	System.out.println(user.getCompany());
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
                        <input disabled="disabled" type="text" id="l" name="l" size="20" maxlength="20" tabindex="8"
                         value="<%=user.getCity() %>" />
                      </span>
                    </div>
                    <div class="row">
                      <span class="label2">State:</span>
                      <span class="formw">
                        <input disabled="disabled" type="text" id="st" name="st" size="20" maxlength="20" tabindex="9"
                         value="<%=user.getState() %>" />
                      </span>
                    </div>
                    <div class="row">
                      <span class="label2">Postal Code:</span>
                      <span class="formw">
                        <input disabled="disabled" type="text" id="postalCode" name="postalCode" size="20" maxlength="20" tabindex="10"
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
                        <input disabled="disabled" type="text" id="telephoneNumber" name="telephoneNumber" size="20" maxlength="20" tabindex="12"
                         value="<%=user.getPhoneNumber() %>" />
                      </span>
                      <span class="required">*</span>
                    </div>
                    <div class="row">
                      <span class="label2">Fax:</span>
                      <span class="formw">
                        <input disabled="disabled" type="text" id="facsimileTelephoneNumber" name="facsimileTelephoneNumber" size="20" maxlength="20" tabindex="13"
                         value="<%=user.getFax() %>" />
                      </span>
                    </div>
                    <div class="row">
                      <span class="label2">Mobile:</span>
                      <span class="formw">
                        <input disabled="disabled" type="text" id="mobile" name="mobile" size="20" maxlength="20" tabindex="14"
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
<%	if( session.getAttribute("passed") != null){ %>
                    <div class="row">
                      <span style="float: center;" class="passed"><%=session.getAttribute("passed")%></span>
                    </div>
<%		session.removeAttribute("passed");
	}
	if( session.getAttribute("failed") != null){ %>
                    <div class="row">
                      <span style="float: center;" class="failed"><%=session.getAttribute("failed")%></span>
                    </div>
<%		session.removeAttribute("failed");
	}	%>
                    <div id="buttonGrp1" class="Buttons" style="text-align: center; clear: none; padding-top: 20px; width: 200px; height: 20px; display: block">
                      <a class="Button" id="updateButton" onclick="javascript: UpdateForm()" style="display: compact;">Update</a>
                      <a class="Button" id="backButton" onclick="javascript: BackForm()" style="display: compact;">Back</a>
                    </div>
                    <div id="buttonGrp2" class="Buttons" style="text-align: center; clear: none; padding-top: 20px; width: 200px; height: 20px; display: none">
                      <a class="Button" id="submitButton" onclick="javascript: SubmitForm()" style="display: compact;">Submit</a>
                      <a class="Button" id="cancelButton" onclick="javascript: CancelForm()" style="display: compact;">Cancel</a>
                    </div>
                  </form>
                    <div class="row">
                    <span class="label2">Member of:</span>
                    <div style="float: right; text-align: left; width:395px;"><ul>
<% 

LdapTool lt = null;
try {
	lt = new LdapTool();
} catch (FileNotFoundException fe){
	// TODO Auto-generated catch block
	fe.printStackTrace();					
} catch (NamingException e) {
	// TODO Auto-generated catch block
	e.printStackTrace();
}

if(lt == null){
	//TODO
}


Attributes attrs = lt.getUserAttributes(request.getParameter("dn"));
Attribute attr = attrs.get("memberOf");
Set<String> baseGroups = lt.getBaseGroups();
if (attr != null) {
	NamingEnumeration e = attr.getAll(); 
	while (e.hasMore()) {
		String dn = (String)e.next();
		String name = dn.split(",")[0].split("=")[1];
		baseGroups.remove(name);%>
                    <li title='<%= dn %>'><%= name %></li>
	<% } } else { %><li>No groups</li><% } %>
                    </ul></div>
                  </div>
    <% if (baseGroups.size()>0) { %>
                <form id="addto" method="post" action="AddGroupUser">
                  <!-- <br /><span id="addlabel"><b>Add to Group:</b></span><br /> -->
                  <input type="hidden" name="dn" value="<%=request.getParameter("dn") %>" />
                  <%	session.setAttribute("dn", request.getParameter("dn")); %>
                  <select name="groupselect" id="groupselect">
<% for (String group : baseGroups) {
		  %>
		  		    <option value="<%= group %>"><%= group %></option>
		  <% } %>
                  </select>
                  <a class="Button" href="#" id="addbutton" onclick="javascript: SubmitGroupForm()">Add User to Group</a>
                </form>
<% } lt.close(); %>
                </div>
<%	}	%>
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
