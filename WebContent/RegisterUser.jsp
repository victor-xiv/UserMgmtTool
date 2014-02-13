<html>
  <head>
    <title>Register User</title>
    <link rel="stylesheet" href="css/concerto.css" type="text/css" />
    <link rel="stylesheet" href="css/general.css" type="text/css" />
    <script type="text/javascript" language="javascript">
function ClickYes(){
    document.form.submit();
}
function ClickNo(){
	//location.reload(true);
	history.go(-2);
}
function Logout(){
//	window.open('RegisterPopup.jsp', '', 'width=600,height=400');
//	setTimeout("",10000);
//	window.location = "/concerto/Logout.htm?breakCommonContext=true&clientTimedOut=false";
//	top.location.href = "/concerto/Logout.htm?breakCommonContext=true&clientTimedOut=false";
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
<%	if(session.getAttribute("passed") != null){
		session.removeAttribute("passed");	%>
		        <div align="center"><img src="http://supporttracker.orionhealth.com/concerto/images/logos/supporttracker.gif" alt="" /></div>
		        <img src="css/images/swish.gif" alt="There should be an image here...." />
                <br />
                <div class="passed" style="width: 500px">
                  <span style="font-size:medium; color: #808000;">You have been registered successfully.</span>
                  <br />
                  <br />
                  <span style="font-size:medium; color: black">Please logout then login again for changes to take effect.</span>
                </div>
<%	}else{	%>
                <div align="center"><img src="http://supporttracker.orionhealth.com/concerto/images/logos/supporttracker.gif" alt="" /></div>
                <h1>Registration for Access to<br/>Orion Product Knowledge Base </h1>
                <img src="css/images/swish.gif" alt="There should be an image here...." />
                <br />
<%  if(session.getAttribute("error") != null){ %>
                <div class="error"><%=session.getAttribute("error") %></div>
<%  session.removeAttribute("error"); }else{ %>
                <div style="width: 600px; padding: 5px; margin: 5px auto ";>
                  <form name="form" method="post" action="RegisterUserConfirmation.jsp">
                    <div class="row">
                      <span id="msg" style="width: auto; text-align: center">Granting this access will integrate your Support Tracker login with the KB system.<br />Would you like to continue?</span>
                    </div>
                    <div class="row">
                      <div class="Buttons" style="text-align: center; clear: none; padding-top: 5px; width: 180px; height: 20px;">
                        <a class="Button" href="#" onclick="javascript: ClickYes()">Yes</a>
                        <a class="Button" href="#" onclick="javascript: ClickNo()">No</a>
                      </div>
                    </div>
                  </form>
                </div>
                <div align="center" class="error"><span class="msg" id="global_msg"></span></div>
<%	}	%>
                <img src="css/images/swish.gif" alt="There should be an image here...." />
                <div align="center" class="disclaimer2">Having problems?<br/>Email <a href="mailto:support@orionhealth.com">Support@Orionhealth.com</a><br /></div>
              </td>
            </tr>
          </table>
        </td>
      </tr>
    </table>
<%	}	%>
  </body>
</html>