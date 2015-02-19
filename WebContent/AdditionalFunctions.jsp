<html>
<head>
<title>Additional Functions Console</title>
<link rel="stylesheet" href="css/concerto.css" type="text/css" />
<link rel="stylesheet" href="css/general.css" type="text/css" />


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

							<h1>Additional Functions</h1> <img src="css/images/swish.gif" alt="#" />


							
							<table>
							
								
								<tr>
									<td class="label2">Get all users who have no an email address</td> 
									<td class="passed" id="usersNoEmail"></td>  
									<td id="download"><a visibility='hidden'/></td>
									<td><a class="Button" href="AdditionalFunctions?rqst=getAllUsersNoEmail" >Get</a></td>
								</tr>
							
							
							</table>
							
							
							
							


							<div align="center" class="error">
								<span class="msg" id="global_msg"></span>
							</div> <img src="css/images/swish.gif" alt="#" />
							<div align="center" class="disclaimer2">
								
								
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