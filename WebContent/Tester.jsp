<html>
<head>
<title>Tester Console</title>
<link rel="stylesheet" href="css/concerto.css" type="text/css" />
<link rel="stylesheet" href="css/general.css" type="text/css" />


<script type="text/javascript" language="javascript" src="./js/ajaxgen.js"></script>
<script type="text/javascript" language="javascript">


gettingVersion();
function gettingVersion(){
	ajax.open("POST", "Test", true);
	ajax.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
    ajax.setRequestHeader("Accept", "text/xml, application/xml, text/plain");
    var params = "rqst=" + "UserMgmt-Version";
    ajax.send(params);
    
    // handling ajax state
    ajax.onreadystatechange = function(){
    	// handling once request responded
    	if(ajax.readyState == 4){
    		// if response "OK"
    		if(ajax.status == 200){
    			document.getElementById("UsrMgmtVersion").innerHTML=ajax.responseText;
    		}
    	}
    }
}


function securityProvider(){
	document.getElementById("secProvider").innerHTML = "<font color='red'>Security provider test started.</font>";
	
	ajax.open("POST", "Test", true);
	ajax.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
    ajax.setRequestHeader("Accept", "text/xml, application/xml, text/plain");
    var params = "rqst=" + "securityProvider";
    ajax.send(params);
    
    // handling ajax state
    ajax.onreadystatechange = function(){
    	// handling once request responded
    	if(ajax.readyState == 4){
    		// if response "OK"
    		if(ajax.status == 200){
    			document.getElementById("secProvider").innerHTML=ajax.responseText;
    		} else {
    			document.getElementById("secProvider").innerHTML="Server failed to response. Response code is: " + ajax.status;        	        		
    		}
    	}
    }
    
	
	
}

function ldapConnection(){
	document.getElementById("ldapRslt").innerHTML = "<font color='red'>LDAP connection test started.</font>";
	
	ajax.open("POST", "Test", true);
	ajax.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
    ajax.setRequestHeader("Accept", "text/xml, application/xml, text/plain");
    var params = "rqst=" + "ldapConnection";
    ajax.send(params);
    
    // handling ajax state
    ajax.onreadystatechange = function(){
    	// handling once request responded
    	if(ajax.readyState == 4){
    		// if response "OK"
    		if(ajax.status == 200){
    			document.getElementById("ldapRslt").innerHTML=ajax.responseText;
    		} else {
    			document.getElementById("ldapRslt").innerHTML="Server failed to response. Response code is: " + ajax.status;        	        		
    		}
    	}
    }
}

function portalConnection(){
	document.getElementById("portalRslt").innerHTML="<font color='red'>Portal connection test started.</font>";
	
	ajax.open("POST", "Test", true);
	ajax.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
    ajax.setRequestHeader("Accept", "text/xml, application/xml, text/plain");
    var params = "rqst=" + "portalConnection";
    ajax.send(params);
    
    // handling ajax state
    ajax.onreadystatechange = function(){
    	// handling once request responded
    	if(ajax.readyState == 4){
    		// if response "OK"
    		if(ajax.status == 200){
    			document.getElementById("portalRslt").innerHTML=ajax.responseText;
    		} else {
    			document.getElementById("portalRslt").innerHTML="Server failed to response. Response code is: " + ajax.status;        	        		
    		}
    	}
    }
}

function supportTrackerDBConnection(){
	document.getElementById("sptDBRslt").innerHTML="<font color='red'>Support Tracker DB connection test started.</font>";
	
	ajax.open("POST", "Test", true);
	ajax.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
    ajax.setRequestHeader("Accept", "text/xml, application/xml, text/plain");
    var params = "rqst=" + "supportTrackerDBConnection";
    ajax.send(params);
    
    // handling ajax state
    ajax.onreadystatechange = function(){
    	// handling once request responded
    	if(ajax.readyState == 4){
    		// if response "OK"
    		if(ajax.status == 200){
    			document.getElementById("sptDBRslt").innerHTML=ajax.responseText;
    		} else {
    			document.getElementById("sptDBRslt").innerHTML="UsrMgmt Server failed to response. Response code is: " + ajax.status;        	        		
    		}
    	}
    }
}

function sendingAnEmail(email){
	document.getElementById("emilRslt").innerHTML="dummy result";
	alert("test sending an email to: " + email);
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
								<img
									src="http://supporttracker.orionhealth.com/concerto/images/logos/supporttracker.gif">
							</div>

							<h1>Tester Console</h1> <img src="css/images/swish.gif" alt="#" />


							
							<table>
							
								<tr>
									<td class="label2">Security Provider (Bouncy Castle)</td> 
									<td class="passed" id="secProvider"></td>  
									<td><a class="Button" href="#" onclick="javascript: securityProvider()">Test</a></td>
								</tr>
								
								<tr>
									<td class="label2">Ldap Connection</td> 
									<td class="passed" id="ldapRslt"></td>  
									<td><a class="Button" href="#" onclick="javascript: ldapConnection()">Test</a></td>
								</tr>
							
								<tr>
									<td class="label2">Concerto Portal Connection</td> 
									<td class="passed" id="portalRslt"></td>  
									<td><a class="Button" href="#" onclick="javascript: portalConnection()">Test</a></td>
								</tr>
								
								<tr>
									<td class="label2">Support Tracker Database Connection</td> 
									<td class="passed" id="sptDBRslt"></td>  
									<td><a class="Button" href="#" onclick="javascript: supportTrackerDBConnection()">Test</a></td>
								</tr>
								
								<tr>
									<td class="label2">Sending an email</td> 
									<td class="passed" id="emilRslt"></td>  
									<td><a class="Button" href="#" onclick="javascript: sendingAnEmail()">Test</a></td>
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