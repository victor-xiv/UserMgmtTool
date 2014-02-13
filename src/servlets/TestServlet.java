package servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import tools.ConcertoAPI;

@SuppressWarnings("serial")
public class TestServlet extends HttpServlet {
	public void doGet(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException
	{
		ConcertoAPI.testGetClientUser("JackM");
		//ConcertoAPI.testAddClientUser("testuser22");
		//String redirectURL = response.encodeRedirectURL("Test.jsp");
		//response.sendRedirect(redirectURL);
	}
	public void doPost(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException
	{
		doGet(request, response); 
	}
}