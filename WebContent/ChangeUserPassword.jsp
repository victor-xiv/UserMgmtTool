<html>
	<head>
		<title>Password Management Console</title>
		<link rel="stylesheet" href="css/concerto.css" type="text/css" />
		<link rel="stylesheet" href="css/general.css" type="text/css" />
		<jsp:useBean id="user" class="beans.LdapUser" scope="page" />
		
<%
try{
	String userDN = (String)session.getAttribute("userDN"); 
	user.processUserDN(userDN);	
} catch (Exception e) {
	session.setAttribute("error", e.getMessage());
}		
%>
		<script src="./js/validator.js"></script>
		
		<script type="text/javascript" language="javascript">

/**
 * Validate user's input password.
 * Pasword must be more than 8 and less than 12 characters in length
 * Password must contatins A-Z, a-z, 0-9
 */
function validatePwd01(){
    var regex = new RegExp("[A-Za-z0-9]{8,12}");
    if(document.getElementById('password01').value == "" ){
        document.getElementById('pwd_msg01').innerHTML = "<font color=\"#FF0000\">Please enter a valid password.</font>";
        return false;
    }else if( !document.getElementById('password01').value.match(regex)){
        document.getElementById('pwd_msg01').innerHTML = "<font color=\"#FF0000\">Password needs to have 8-12 characters from letters [A-Za-z0-9]</font>";
        return false;
    }else{
        document.getElementById('pwd_msg01').innerHTML = "<img src=\"css/images/check_right.gif\" />";
    }
	if(!document.getElementById('password02').value == ""){
		return validatePwd02();
	}else{
		return true;
	}
}


/**
 * Validate the confirmation password.
 * If this confirmation password matches the first one => true
 * else false
 */
function validatePwd02(){
    if(document.getElementById('password01').value == ""){
        document.getElementById('pwd_msg02').innerHTML = "<font color=\"#FF0000\">Please fill in the first password.</font>";
	}else if(document.getElementById('password02').value != document.getElementById('password01').value){
		document.getElementById('pwd_msg02').innerHTML = "<font color=\"#FF0000\">Passwords not matching!</font>";
	}else{
	    document.getElementById('pwd_msg02').innerHTML = "<img src=\"css/images/check_right.gif\" />";
	    return true;
	}
	return false;
}
 
/**
 * reset password forms
 */
function ResetForm(){
    document.getElementById('pwd_msg01').innerHTML == "";
    document.getElementById('pwd_msg02').innerHTML == "";
    document.form.reset(); 
    return false;
}

/**
 * submit the manullay typed in passwords to ChangePasswordServlet
 */
function SubmitForm(){
	
	if(!validatePwd01()){
    	return false;
    }
	
	var psw1 = document.getElementById('password01').value;
	var psw2 = document.getElementById('password02').value;
	if(!passwordValidator(psw1, psw2)){
		return false;
	}
	
	
	// if both passwords (typed in by user) are the same and valdiated => process further
	var ajax3;
	if (window.XMLHttpRequest) {// code for IE7+, Firefox, Chrome, Opera, Safari
		ajax3 = new XMLHttpRequest();
	} else {// code for IE6, IE5
		ajax3 = new ActiveXObject("Microsoft.XMLHTTP");
	}
	

	ajax3.open("POST", "ChangePassword", true);
	ajax3.setRequestHeader("Content-Type","application/x-www-form-urlencoded");
	ajax3.setRequestHeader("Accept", "text/xml, application/xml, text/plain");
	var params = "rqst=" + "ChangePassword" + "&NewPsw=" + document.getElementById('password01').value;
	ajax3.send(params);

	// handling ajax state
	ajax3.onreadystatechange = function() {
		// handling once request responded
		if (ajax3.readyState == 4) {
			// if response "OK"
			if (ajax3.status == 200) {
				var rsp = ajax3.responseText;
				rsp = rsp.split("|");
				if(rsp[0] === "failed"){
					document.getElementById("failed").innerHTML = rsp[1];
				} else {
					document.getElementById("passed").innerHTML = ajax3.responseText;
				}
			} else {
				document.getElementById("failed").innerHTML = "Server failed to response. Response code is: "
						+ ajax3.status;
			}
		}
	}
}


/**
 * call to ChangePasswordServlet to Generate a new random password and SMS to the user
 * with request parameter: rqst=GeneratePassword
 */
function generateRandomPassword(){
	var ajax1;
	if (window.XMLHttpRequest) {// code for IE7+, Firefox, Chrome, Opera, Safari
		ajax1 = new XMLHttpRequest();
	} else {// code for IE6, IE5
		ajax1 = new ActiveXObject("Microsoft.XMLHTTP");
	}
	

	ajax1.open("POST", "ChangePassword", true);
	ajax1.setRequestHeader("Content-Type","application/x-www-form-urlencoded");
	ajax1.setRequestHeader("Accept", "text/xml, application/xml, text/plain");
	var params = "rqst=" + "GeneratePassword";
	ajax1.send(params);

	// handling ajax state
	ajax1.onreadystatechange = function() {
		// handling once request responded
		if (ajax1.readyState == 4) {
			// if response "OK"
			if (ajax1.status == 200) {
				var rsp = ajax1.responseText;
				rsp = rsp.split("|");
				if(rsp[0] === "failed"){
					document.getElementById("failed").innerHTML = rsp[1];
				} else {
					document.getElementById("passed").innerHTML = ajax1.responseText;
				}
			} else {
				document.getElementById("failed").innerHTML = "Server failed to response. Response code is: "
						+ ajax1.status;
			}
		}
	}
}

/**
 * this method called everytime this page is loaded
 * it check if this user has a validate mobile number or not
 * if this user has a validate number (responseText = 'true') => create a "Generate" button for user generating a new random password
 * otherwise (responseText = 'false') => do nothing
 */
shouldProvideGeneratingNewPasswordForThisUser();
function shouldProvideGeneratingNewPasswordForThisUser(){
	var ajax2;
	if (window.XMLHttpRequest) {// code for IE7+, Firefox, Chrome, Opera, Safari
		ajax2 = new XMLHttpRequest();
	} else {// code for IE6, IE5
		ajax2 = new ActiveXObject("Microsoft.XMLHTTP");
	}
	

	ajax2.open("POST", "ChangePassword", true);
	ajax2.setRequestHeader("Content-Type","application/x-www-form-urlencoded");
	ajax2.setRequestHeader("Accept", "text/xml, application/xml, text/plain");
	var params = "rqst=" + "ShouldAllowGeneratingPsw";
	ajax2.send(params);

	// handling ajax state
	ajax2.onreadystatechange = function() {
		// handling once request responded
		if (ajax2.readyState == 4) {
			// if response "OK"
			if (ajax2.status == 200) {
				if(ajax2.responseText === "true"){
					document.getElementById("generateButton").innerHTML = '<div style="padding-top:20px"> <img src="css/images/swish.gif" alt="#" /></div>' + 
						'<div class="row" style="padding-top:20px; font-family:Arial, Helvetica, sans-serif;"><b> Generate a random password and send a text message to the user. </b></div>' +
			            '<div class="Buttons" style="text-align: center; clear: none;  width: 180px; height: 20px;">' +
							'<a class="Button" href="#" onclick="javascript: generateRandomPassword()">Generate</a>' +
						'</div>';
					return;
				}
				
				if(ajax2.responseText !== "false"){
					document.getElementById("generateButton").innerHTML = ajax2.responseText;
				}
			} 
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
                                <div align="center"><img src="css/images/logos/supporttracker.gif" alt="Support Tracker Logo" /></div>
					            <h1>Password Management</h1>
					            <h2> Update a password for: <%=user.getDisplayName() %> </h2>
					            <img src="css/images/swish.gif" alt="#" />
					            <br />
<%	if( session.getAttribute("error") != null){ %>
								<div align="center" class="failed">
		<%=session.getAttribute("error") %>
		<%session.removeAttribute("error"); %>
								</div>
<%	}else{ %>

				                <div style="width: 600px; padding: 5px; margin: 5px auto ";>
				                    <form name="form" method="post" action="ChangePassword">
				                        <div class="row">
                                            <span class="label2">New Password:</span>
                                            <span class="formw"><input type="password" name="password01" id="password01" 
                                                size="20" maxlength="12" onblur="javascript: validatePwd01();" style="width: 200px"/></span>
                                            <span class="msg" id="pwd_msg01"></span>
                                        </div>
				                        <div class="row">
                                            <span class="label2">Confirm Password:</span>
                                            <span class="formw"><input type="password" name="password02" id="password02" 
                                                size="20" maxlength="12" onblur="javascript: validatePwd02();" style="width: 200px"/></span>
                                            <span class="msg" id="pwd_msg02"></span>
                                        </div>
                                        <div class="row"></div>
				                        <div class="Buttons" style="text-align: center; clear: none; padding-top: 20px; width: 180px; height: 20px;">
					                        <a class="Button" href="#" onclick="javascript: SubmitForm()">Submit</a>
					                        <a class="Button" href="#" onclick="javascript: ResetForm()">Reset</a>
					                    </div>
					                </form>
					                
					                <!-- this div used to decide whether the page should shows the "Generate" button, where user can use to generate a new
					                password for the user on this page. If the user on this page doesn't have a mobile phone number or has an invalid one
					                stored in Support Tracker DB, then this button would not show. it shows only otherwise.
					                The decision made in ChangePasswordServlet -->
					                <div id="generateButton"> </div>
					            </div>
<%	} %>

								<div align="center" class="passed" id="passed">
								
<%	if( session.getAttribute("passed") != null){ %>
 		<%=session.getAttribute("passed")%>
		<%session.removeAttribute("passed");
	}%>
								</div>

	 
	
								<div align="center" class="failed" id="failed">
<% if( session.getAttribute("failed") != null){ %>
		<%=session.getAttribute("failed") %>

<%		session.removeAttribute("failed");
	}
%>
								</div>

								<div align="center" class="error"><span class="msg" id="global_msg"></span></div>
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