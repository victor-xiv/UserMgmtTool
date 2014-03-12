package servlets;

import javax.naming.NamingException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import java.io.FileNotFoundException;
import java.io.IOException;

import com.concerto.sdk.security.ValidatedRequest;
import com.concerto.sdk.security.InvalidRequestException;

import ldap.*;

@SuppressWarnings("serial")
public class ChangePasswordServlet extends HttpServlet {
	public void doGet(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException
    {
		String userDN = "";
		HttpSession session = request.getSession(true);
		if(session.getAttribute("dn") != null){
			userDN = (String)session.getAttribute("dn");
		}else{
			try{
				ValidatedRequest req = new ValidatedRequest(request, LdapProperty.getProperty(LdapConstants.CONCERTO_VALIDATOR));
				if( req.getParameter("userDN") != null ){
					userDN = req.getParameter("userDN");
				}else{
					session.setAttribute("error", "<font color=\"red\">Non-ldap user cannot change username via this menu.</font>");
					String redirectURL = response.encodeRedirectURL("ChangeUserPassword.jsp");
					response.sendRedirect(redirectURL);
				}
			}catch(InvalidRequestException ex){
				session.setAttribute("error", "<font color=\"red\">This page can only be accessed from within Concerto.</font>");
			}
		}
		session.setAttribute("userDN", userDN);
		String redirectURL = response.encodeRedirectURL("ChangeUserPassword.jsp");
		response.sendRedirect(redirectURL);
    }
	
	public void doPost(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException
    {
		HttpSession session = request.getSession(true);
		String userDN = (String)session.getAttribute("userDN");
		if( userDN == null )
			userDN = request.getParameter("userDN");
		String password01 = request.getParameter("password01");
		
		
		
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
		
		// TODO
		if( lt == null){
			
		}
		
		
		
		if(lt.changePassword(userDN, password01)){
			session.setAttribute("passed", "The password was changed successfully.");
		}else{
			session.setAttribute("failed", "The password change has failed.");
		}
		lt.close();
		session.setAttribute("userDN", userDN);
		String redirectURL = response.encodeRedirectURL("ChangeUserPassword.jsp");
		response.sendRedirect(redirectURL);
	}
}
