<html>
	<head>
		<title>Password Management Console</title>
		<link rel="stylesheet" href="css/concerto.css" type="text/css" />
		<link rel="stylesheet" href="css/general.css" type="text/css" />
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
 * submit the form
 */
function SubmitForm(){
	if(validatePwd01()){
    	document.form.submit();
    	return true;
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
					            </div>
<%	} %>
<%	if( session.getAttribute("passed") != null){ %>
								<div align="center" class="passed">
<%=session.getAttribute("passed")%>
								</div>
<%session.removeAttribute("passed");
	}
	if( session.getAttribute("failed") != null){ 
	%>
								<div align="center" class="failed">
<%=session.getAttribute("failed")%>
								</div>
<%		session.removeAttribute("failed");
	}%>
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