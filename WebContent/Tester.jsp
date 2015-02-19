<html>
<head>
<title>Tester Console</title>
<link rel="stylesheet" href="css/concerto.css" type="text/css" />
<link rel="stylesheet" href="css/general.css" type="text/css" />


<script type="text/javascript" language="javascript" src="./js/ajaxgen.js"></script>
<script type="text/javascript" language="javascript">



/*inform user that we are not supporting IE8 or older*/
if( navigator.appVersion.indexOf("MSIE 8.")!=-1 || navigator.appVersion.indexOf("MSIE 7")!=-1
		|| navigator.appVersion.indexOf("MSIE 6")!=-1 || navigator.appVersion.indexOf("MSIE 5")!=-1
		|| navigator.appVersion.indexOf("MSIE 4")!=-1) {
	// check if it is not enterprise mode
	// (if spellcheck feature availbale means that it is enterprise mode of IE11. we are supporting IE11 enterprise mode)
	if(  !('spellcheck' in document.createElement('textarea'))  ){
		window.onload = function(){ document.write('Internet Explorer 8 is not supported. Please use another browser.') };
	}
}
	
	
	
/**
 * getting and displaying the version number at the bottom of the page
 */
gettingVersion();
	function gettingVersion() {
		var ajax1;
		if (window.XMLHttpRequest) {// code for IE7+, Firefox, Chrome, Opera, Safari
			ajax1 = new XMLHttpRequest();
		} else {// code for IE6, IE5
			ajax1 = new ActiveXObject("Microsoft.XMLHTTP");
		}

		ajax1.open("POST", "Test", true);
		ajax1.setRequestHeader("Content-Type",
				"application/x-www-form-urlencoded");
		ajax1
				.setRequestHeader("Accept",
						"text/xml, application/xml, text/plain");
		var params = "rqst=" + "UserMgmt-Version";
		ajax1.send(params);

		// handling ajax state
		ajax1.onreadystatechange = function() {
			// handling once request responded
			if (ajax1.readyState == 4) {
				// if response "OK"
				if (ajax1.status == 200) {
					document.getElementById("UsrMgmtVersion").innerHTML = ajax1.responseText;
				} else {
					document.getElementById("UsrMgmtVersion").innerHTML = "Server failed to response. " + ajax1.status;
				}
			}
		}
	}

/*
 * Test Security Provider (Test Bouncy Castle Security)
 */
	function securityProvider() {
		var ajax2;
		if (window.XMLHttpRequest) {// code for IE7+, Firefox, Chrome, Opera, Safari
			ajax2 = new XMLHttpRequest();
		} else {// code for IE6, IE5
			ajax2 = new ActiveXObject("Microsoft.XMLHTTP");
		}
		
		document.getElementById("secProvider").innerHTML = "<font color='red'>Security provider test started.</font>";

		ajax2.open("POST", "Test", true);
		ajax2.setRequestHeader("Content-Type","application/x-www-form-urlencoded");
		ajax2.setRequestHeader("Accept", "text/xml, application/xml, text/plain");
		var params = "rqst=" + "securityProvider";
		ajax2.send(params);

		// handling ajax state
		ajax2.onreadystatechange = function() {
			// handling once request responded
			if (ajax2.readyState == 4) {
				// if response "OK"
				if (ajax2.status == 200) {
					document.getElementById("secProvider").innerHTML = ajax2.responseText;
				} else {
					document.getElementById("secProvider").innerHTML = "Server failed to response. Response code is: "
							+ ajax2.status;
				}
			}
		}

	}

/**
 * test ldap connection
 */
	function ldapConnection() {
		var ajax3;
		if (window.XMLHttpRequest) {// code for IE7+, Firefox, Chrome, Opera, Safari
			ajax3 = new XMLHttpRequest();
		} else {// code for IE6, IE5
			ajax3 = new ActiveXObject("Microsoft.XMLHTTP");
		}
		
		document.getElementById("ldapRslt").innerHTML = "<font color='red'>LDAP connection test started.</font>";

		ajax3.open("POST", "Test", true);
		ajax3.setRequestHeader("Content-Type","application/x-www-form-urlencoded");
		ajax3.setRequestHeader("Accept","text/xml, application/xml, text/plain");
		var params = "rqst=" + "ldapConnection";
		ajax3.send(params);

		// handling ajax state
		ajax3.onreadystatechange = function() {
			// handling once request responded
			if (ajax3.readyState == 4) {
				// if response "OK"
				if (ajax3.status == 200) {
					document.getElementById("ldapRslt").innerHTML = ajax3.responseText;
				} else {
					document.getElementById("ldapRslt").innerHTML = "Server failed to response. Response code is: "
							+ ajax3.status;
				}
			}
		}
	}


/**
 * test portal connection
 */
	function portalConnection() {
		var ajax4;
		if (window.XMLHttpRequest) {// code for IE7+, Firefox, Chrome, Opera, Safari
			ajax4 = new XMLHttpRequest();
		} else {// code for IE6, IE5
			ajax4 = new ActiveXObject("Microsoft.XMLHTTP");
		}
		
		document.getElementById("portalRslt").innerHTML = "<font color='red'>Portal connection test started.</font>";

		ajax4.open("POST", "Test", true);
		ajax4.setRequestHeader("Content-Type","application/x-www-form-urlencoded");
		ajax4.setRequestHeader("Accept",
						"text/xml, application/xml, text/plain");
		var params = "rqst=" + "portalConnection";
		ajax4.send(params);

		// handling ajax state
		ajax4.onreadystatechange = function() {
			// handling once request responded
			if (ajax4.readyState == 4) {
				// if response "OK"
				if (ajax4.status == 200) {
					document.getElementById("portalRslt").innerHTML = ajax4.responseText;
				} else {
					document.getElementById("portalRslt").innerHTML = "Server failed to response. Response code is: "
							+ ajax4.status;
				}
			}
		}
	}


/**
 * test support tracker DB connection
 */
	function supportTrackerDBConnection() {
		var ajax5;
		if (window.XMLHttpRequest) {// code for IE7+, Firefox, Chrome, Opera, Safari
			ajax5 = new XMLHttpRequest();
		} else {// code for IE6, IE5
			ajax5 = new ActiveXObject("Microsoft.XMLHTTP");
		}
		
		document.getElementById("sptDBRslt").innerHTML = "<font color='red'>Support Tracker DB connection test started.</font>";

		ajax5.open("POST", "Test", true);
		ajax5.setRequestHeader("Content-Type",
				"application/x-www-form-urlencoded");
		ajax5.setRequestHeader("Accept",
						"text/xml, application/xml, text/plain");
		var params = "rqst=" + "supportTrackerDBConnection";
		ajax5.send(params);

		// handling ajax state
		ajax5.onreadystatechange = function() {
			// handling once request responded
			if (ajax5.readyState == 4) {
				// if response "OK"
				if (ajax5.status == 200) {
					document.getElementById("sptDBRslt").innerHTML = ajax5.responseText;
				} else {
					document.getElementById("sptDBRslt").innerHTML = "UsrMgmt Server failed to response. Response code is: "
							+ ajax5.status;
				}
			}
		}
	}


/**
 * test sending an email to the client
 */
	function sendingAnEmail(email) {
		var ajax6;
		if (window.XMLHttpRequest) {// code for IE7+, Firefox, Chrome, Opera, Safari
			ajax6 = new XMLHttpRequest();
		} else {// code for IE6, IE5
			ajax6 = new ActiveXObject("Microsoft.XMLHTTP");
		}
		
		document.getElementById("emilRslt").innerHTML = "<font color='red'>Sending an email test started.</font>";

		ajax6.open("POST", "Test", true);
		ajax6.setRequestHeader("Content-Type",
				"application/x-www-form-urlencoded");
		ajax6.setRequestHeader("Accept",
						"text/xml, application/xml, text/plain");
		var params = "rqst=" + "emailSending" + "&mailTo="
				+ encodeURI(document.getElementById("emailAdd").value);
		ajax6.send(params);

		// handling ajax state
		ajax6.onreadystatechange = function() {
			// handling once request responded
			if (ajax6.readyState == 4) {
				// if response "OK"
				if (ajax6.status == 200) {
					document.getElementById("emilRslt").innerHTML = ajax6.responseText;
				} else {
					document.getElementById("emilRslt").innerHTML = "UsrMgmt Server failed to response. Response code is: "
							+ ajax6.status;
				}
			}
		}
	}


/**
 * test sending an sms
 */
	function sendingAnSms(){
		var ajax7;
		if (window.XMLHttpRequest) {// code for IE7+, Firefox, Chrome, Opera, Safari
			ajax7 = new XMLHttpRequest();
		} else {// code for IE6, IE5
			ajax7 = new ActiveXObject("Microsoft.XMLHTTP");
		}
		
		document.getElementById("smsRslt").innerHTML = "<font color='red'>Sending an sms test started.</font>";

		ajax7.open("POST", "Test", true);
		ajax7.setRequestHeader("Content-Type",
				"application/x-www-form-urlencoded");
		ajax7.setRequestHeader("Accept",
						"text/xml, application/xml, text/plain");
		var params = "rqst=" + "smsSending" + "&mobile="
				+ document.getElementById("mobile").value;
		ajax7.send(params);

		// handling ajax state
		ajax7.onreadystatechange = function() {
			// handling once request responded
			if (ajax7.readyState == 4) {
				// if response "OK"
				if (ajax7.status == 200) {
					document.getElementById("smsRslt").innerHTML = ajax7.responseText;
				} else {
					document.getElementById("smsRslt").innerHTML = "UsrMgmt Server failed to response. Response code is: "
							+ ajax7.status;
				}
			}
		}
	}
</script>

 
</head>
<body>
	<table align="center" border="0" style="border-color: #ef7224"
		cellspacing="1">
		<tr>
			<td bgcolor="#ef7224">
				<table bgcolor="#ffffff">
					<tr align="center">
						<td align="center">
							<div align="center">
								<img src="css/images/logos/supporttracker.gif" alt="Support Tracker Logo" />
							</div>

							<h1>Tester Console</h1> <img src="css/images/swish.gif" alt="#" />


							
							<table>
							
								<tr>
									<td class="label2">Security Provider (Bouncy Castle)</td> 
									<td> </td>
									<td class="passed" id="secProvider"></td>  
									<td><a class="Button" href="#" onclick="javascript: securityProvider()">Test</a></td>
								</tr>
								
								<tr>
									<td class="label2">Ldap Connection</td> 
									<td> </td>
									<td class="passed" id="ldapRslt"></td>  
									<td><a class="Button" href="#" onclick="javascript: ldapConnection()">Test</a></td>
								</tr>
							
								<tr>
									<td class="label2">Concerto Portal Connection</td> 
									<td> </td>
									<td class="passed" id="portalRslt"></td>  
									<td><a class="Button" href="#" onclick="javascript: portalConnection()">Test</a></td>
								</tr>
								
								<tr>
									<td class="label2">Support Tracker Database Connection</td> 
									<td> </td>
									<td class="passed" id="sptDBRslt"></td>  
									<td><a class="Button" href="#" onclick="javascript: supportTrackerDBConnection()">Test</a></td>
								</tr>
								
								<tr>
									<td class="label2">Sending an email</td> 
									<td> <form> </form><input type="text" id="emailAdd" placeholder="Receiver email address"> </form></td>
									<td class="passed" id="emilRslt"></td>  
									<td><a class="Button" href="#" onclick="javascript: sendingAnEmail()">Test</a></td>
								</tr>
							
								<tr>
									<td class="label2">Sending an sms</td> 
									<td> <form> </form><input type="text" id="mobile" placeholder="Receiver mobile number"> </form></td>
									<td class="passed" id="smsRslt"></td>  
									<td><a class="Button" href="#" onclick="javascript: sendingAnSms()">Test</a></td>
								</tr>
							
							</table>
							
							
							
							


							<div align="center" class="error">
								<span class="msg" id="global_msg"></span>
							</div> <img src="css/images/swish.gif" alt="#" />
							<div align="center" class="disclaimer2">
								version <span id="UsrMgmtVersion"></span> <br/>
								
								Having problems?<br />Email <a
									href="mailto:support@orionhealth.com">Support@Orionhealth.com</a><br />
							</div>
						</td>
					</tr>

				</table>

			</td>
		</tr>
	</table>
</body>
</html>