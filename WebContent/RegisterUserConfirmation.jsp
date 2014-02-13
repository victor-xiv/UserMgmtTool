<html>
  <head>
    <title>Register User</title>
    <link rel="stylesheet" href="css/concerto.css" type="text/css" />
    <link rel="stylesheet" href="css/general.css" type="text/css" />
    <jsp:useBean id="countries" class="beans.Countries" scope="session" />
    <jsp:useBean id="user" class="beans.UserDetails" scope="session" />
    <%@ page import="java.util.ArrayList" %>
    <%@ page import="java.util.TreeMap" %>
    <%@ page import="java.util.Map" %>
    <% user.processUsername((String)session.getAttribute("username")); %>
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
        if (document.getElementById('description').value == "") {
            document.getElementById('validation_msg').innerHTML += "* Please enter the role of the person<br/>";
            if (theFocus == '')
                theFocus = 'description';
            validated = false;
        }
        if (document.getElementById('department').value == "") {
            document.getElementById('validation_msg').innerHTML += "* Please enter the department<br/>";
            if (theFocus == '')
                theFocus = 'department';
            validated = false;
        }
        //Company is no longer tested, as Orion Health employees do not have this field
        //if (document.getElementById('company').value == "") {
        //    document.getElementById('validation_msg').innerHTML += "* Please select from the list the company where this user belongs.<br/>";
        //    if (theFocus == '')
        //        theFocus = 'company';
        //    validated = false;
        //}
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
    function validatePwd01() {
        var regex = new RegExp("[A-Za-z0-9]{8,12}");
        if (document.getElementById('password01').value == "") {
            document.getElementById('validation_msg').innerHTML = "<font color=\"#FF0000\">Please enter a valid password.</font>";
            document.getElementById('pwd_msg01').innerHTML = "*";
            return false;
        } else if (!document.getElementById('password01').value.match(regex)) {
            document.getElementById('validation_msg').innerHTML = "<font color=\"#FF0000\">Password needs to have 8-12 characters from letters [A-Za-z0-9]</font>";
            document.getElementById('pwd_msg01').innerHTML = "*";
            return false;
        } else {
        	document.getElementById('validation_msg').innerHTML = "";
            document.getElementById('pwd_msg01').innerHTML = "<img src=\"css/images/check_right.gif\" />";
        }
        if (!document.getElementById('password02').value == "") {
            return validatePwd02();
        } else {
            return true;
        }
    }
    function validatePwd02() {
        if (document.getElementById('password01').value == "") {
            document.getElementById('validation_msg').innerHTML = "<font color=\"#FF0000\">Please fill in the first password.</font>";
            document.getElementById('pwd_msg02').innerHTML = "*";
        } else if (document.getElementById('password02').value != document.getElementById('password01').value) {
            document.getElementById('validation_msg').innerHTML = "<font color=\"#FF0000\">Passwords not matching!</font>";
            document.getElementById('pwd_msg02').innerHTML = "*";
        } else {
        	document.getElementById('validation_msg').innerHTML = "";
            document.getElementById('pwd_msg02').innerHTML = "<img src=\"css/images/check_right.gif\" />";
            return true;
        }
        return false;
    }
    function NextForm() {
        if (validateEntries()) {
            document.getElementById('step1').style.display = 'none';
            document.getElementById('step2').style.display = 'block';
            return true;
        }
        return false;
    }
    function CancelForm() {
        //history.back();
        history.go(-2);
    }
    function BackForm() {
        document.getElementById('step2').style.display = 'none';
        document.getElementById('step1').style.display = 'block';
    }
    function SubmitForm() {
        if (validatePwd01() && validatePwd02()) {
            document.form.submit();
            return true;
        }
        return false;
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
      #password01                 {width: 200px;}
      #password02                 {width: 200px;}
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
                <img src="./css/images/swish.gif" alt="There should be an image here...." />
                <br />
                <span class="error" id="validation_msg" style="float:left; width=100%; text-align: left"></span>
                <div style="width: 500px; padding: 5px; margin: 5px auto ";>
                  <form name="form" method="post" action="RegisterUser">
                    <div id="step1">
                    <div class="row">
                      <span class="label2">Username:</span>
                      <span class="formw">
                        <input readonly="readonly" type="text" id="sAMAccountName" name="sAMAccountName" size="20" maxlength="20" tabindex="1"
                         value="<%=user.getUsername() %>" />
                        <input type="hidden" name="dn" value="<%=request.getParameter("dn") %>" />
                        <input type="hidden" name="info" value="<%=user.getClientId() %>" />
<%	session.setAttribute("dn", request.getParameter("dn")); %>
                      </span>
                      <span class="required">*</span>
                    </div>
                    <div class="row">
                      <span class="label2">First Name:</span>
                      <span class="formw">
                        <input type="text" id="givenName" name="givenName" size="20" maxlength="20" tabindex="2"
                         value="<%=user.getFirstName() %>" onblur="this.value=firstCharUp(this.value); doDisplayName();" />
                      </span>
                      <span class="required">*</span>
                    </div>
                    <div class="row">
                      <span class="label2">Last Name:</span>
                      <span class="formw">
                        <input type="text" id="sn" name="sn" size="20" maxlength="20" tabindex="3"
                         value="<%=user.getLastName() %>" onblur="this.value=firstCharUp(this.value); doDisplayName();" />
                      </span>
                      <span class="required">*</span>
                    </div>
                    <div class="row">
                      <span class="label2">Display Name:</span>
                      <span class="formw">
                        <input readonly="readonly" type="text" id="displayName" name="displayName" size="20" maxlength="20"
                         <%if (user.getDisplayName() != "") {%>
                         	value="<%=user.getDisplayName() %>" />
                         <%}else { %>
                         	value="<%=user.getFirstName() %> <%=user.getLastName() %>" />
                         <%} %>
                      </span>
                    </div>
                    <div class="row">
                      <span class="label2">Position / Role:</span>
                      <span class="formw">
                        <input type="text" id="description" name="description" size="20" maxlength="20" tabindex="4"
                         value="<%=user.getDescription() %>" />
                      </span>
                      <span class="required">*</span>
                    </div>
                    <div class="row">
                      <span class="label2">Department:</span>
                      <span class="formw">
                        <input type="text" id="department" name="department" size="20" maxlength="20" tabindex="5"
                         value="<%=user.getDepartment() %>" />
                        <input type="hidden" id="company" name="company" size="20" maxlength="20" tabindex="5"
                         value="<%=user.getCompany() %>" />
                      </span>
                      <span class="required">*</span>
                    </div>
                    <div class="row">
                      <span class="label2">No. / Street:</span>
                      <span class="formw">
                        <input type="text" id="streetAddress" name="streetAddress" size="50" maxlength="50" tabindex="7"
                         value="<%=user.getStreet() %>" />
                      </span>
                    </div>
                    <div class="row">
                      <span class="label2">City:</span>
                      <span class="formw">
                        <input type="text" id="l" name="l" size="20" maxlength="20" tabindex="8"
                         value="<%=user.getCity() %>" />
                      </span>
                    </div>
                    <div class="row">
                      <span class="label2">State:</span>
                      <span class="formw">
                        <input type="text" id="st" name="st" size="20" maxlength="20" tabindex="9"
                         value="<%=user.getState() %>" />
                      </span>
                    </div>
                    <div class="row">
                      <span class="label2">Postal Code:</span>
                      <span class="formw">
                        <input type="text" id="postalCode" name="postalCode" size="20" maxlength="20" tabindex="10"
                         value="<%=user.getPostalCode() %>" />
                      </span>
                    </div>
                    <div class="row">
                      <span class="label2">Country:</span>
                      <span class="formw">
                        <select id="c" name="c" tabindex="11">
                          <option value="">Please select one from the list</option>
<%	TreeMap<String,String> countriesMap = countries.getCountries();
	for(Map.Entry<String, String>entry:countriesMap.entrySet()){
		String countryName = entry.getKey();
		String countryCode = entry.getValue();
		if(countryCode.equals(user.getCountryCode())){	%>
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
                        <input type="text" id="telephoneNumber" name="telephoneNumber" size="20" maxlength="20" tabindex="12"
                         value="<%=user.getPhoneNumber() %>" />
                      </span>
                      <span class="required">*</span>
                    </div>
                    <div class="row">
                      <span class="label2">Fax:</span>
                      <span class="formw">
                        <input type="text" id="facsimileTelephoneNumber" name="facsimileTelephoneNumber" size="20" maxlength="20" tabindex="13"
                         value="<%=user.getFax() %>" />
                      </span>
                    </div>
                    <div class="row">
                      <span class="label2">Mobile:</span>
                      <span class="formw">
                        <input type="text" id="mobile" name="mobile" size="20" maxlength="20" tabindex="14"
                         value="<%=user.getMobile() %>" />
                      </span>
                    </div>
                    <div class="row">
                      <span class="label2">Email:</span>
                      <span class="formw">
                        <input type="text" id="mail" name="mail" size="50" maxlength="50" tabindex="15"
                         value="<%=user.getEmail() %>" />
                      </span>
                      <span class="required">*</span>
                    </div>
                    <div id="buttonGrp1" class="Buttons" style="text-align: center; clear: none; padding-top: 20px; width: 200px; height: 20px; display: block">
                      <a class="Button" id="nextButton" onclick="javascript: NextForm()" style="display: compact;">Next</a>
                      <a class="Button" id="cancelButton" onclick="javascript: CancelForm()" style="display: compact;">Cancel</a>
                    </div>
                    </div>
                    <div id="step2" style="display: none">
                    <div class="row">
                      <span style="font-family: Arial, Helvetica, sans-serif; height: 30px;">Please enter your current Support Tracker password.</span>
                    </div>
                    <div class="row">
                      <span class="label2">Password:</span>
                      <span class="formw">
                        <input type="password" id="password01" name="password01" size="20" maxlength="12" onblur="javascript: validatePwd01();" />
                      </span>
                      <span id="pwd_msg01" class="required">*</span>
                    </div>
                    <div class="row">
                      <span class="label2">Confirm Password:</span>
                      <span class="formw">
                        <input type="password" id="password02" name="password02" size="20" maxlength="12" onblur="javascript: validatePwd02();" />
                      </span>
                      <span id="pwd_msg02" class="required">*</span>
                    </div>
                    <div id="buttonGrp2" class="Buttons" style="text-align: center; clear: none; padding-top: 20px; width: 200px; height: 20px; display: block">
                      <a class="Button" id="submitButton" onclick="javascript: SubmitForm()" style="display: compact;">Submit</a>
                      <a class="Button" id="backButton" onclick="javascript: BackForm()" style="display: compact;">Back</a>
                    </div>
                    </div>
                  </form>
                </div>
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