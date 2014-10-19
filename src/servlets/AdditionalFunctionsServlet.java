package servlets;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;

import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ldap.LdapTool;

import org.apache.log4j.Logger;



@SuppressWarnings("serial")
public class AdditionalFunctionsServlet extends HttpServlet {
	
	private final int BYTES_DOWNLOAD = 1024;
	
	// set up logger
	private Logger logger = Logger.getRootLogger();
	
	public void doGet(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException
	{
		logger.debug("AdditionalFunctionsServlet about to process Get request" + request.getQueryString());
		
		String rqst = request.getParameter("rqst");
		String rslt = "";
		
		logger.debug("Session: " + request.getSession(true) + " is about to execute " + rqst);
		
		// switch the request according to what the user wants to do
		switch (rqst) {
			case "getAllUsersNoEmail" :  // test the Security Provider (Bouncy Castle)
				generateListAllUsersNoEmail(response);
				break;
				
			default:
				response.getWriter().write("Your requested cannot be found.");
		}
	}
	
	
	
	public void doPost(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException
	{
		
	}
	
	
	private void generateListAllUsersNoEmail(HttpServletResponse response) throws IOException{
		logger.debug("Started generating a list of users who have no email.");
		String content = "";
		try {
			LdapTool lt = new LdapTool();
			Set<String> clients = lt.getUserGroupDNs();
			ArrayList<String> users = new ArrayList<String>();
			for(String dn : clients){
				users.addAll(lt.getAllUsersDonotHaveEmailForClient(dn));
			}
			
			for(String s : users){
				content += s + "\n";
			}
					
		} catch (FileNotFoundException | NamingException e) {
			logger.error("Failed to generate list of users who have no email address", e);
			content = "Failed to generate list of users who have no email address";
		}
		
		response.setContentType("text/plain");
		response.setHeader("Content-Disposition",
	                     "attachment;filename=downloadname.txt");
		InputStream is = new ByteArrayInputStream(content.getBytes());
	 
		int read=0;
		byte[] bytes = new byte[BYTES_DOWNLOAD];
		OutputStream os = response.getOutputStream();
	 
		while((read = is.read(bytes))!= -1){
			os.write(bytes, 0, read);
		}
		os.flush();
		os.close();
		
	}
	
	
}