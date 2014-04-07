package servlets;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.rpc.ServiceException;

import ldap.ErrorConstants;
import ldap.LdapConstants;
import ldap.LdapProperty;
import ldap.LdapTool;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import tools.ConcertoAPI;
import tools.EmailClient;
import tools.LoggerTool;
import tools.SupportTrackerJDBC;

@SuppressWarnings("serial")
public class AcceptRequestServlet extends HttpServlet {
	
	Logger logger = LoggerTool.setupDefaultRootLogger(); // initiate as a default root logger
	
	
	/**
	 * Receive the request,
	 * validate the requested account detail (account detail attached with request's parameters)
	 * + If request is declined => delete the .xml file that correspond to that account and send rejected email
	 * + If request is accepted => create the account and send accepted mail (if account created successfully) 
	 */
	@SuppressWarnings("unused")
	public void doGet(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException
	{
		logger = LoggerTool.setupRootLogger(request);
		
		String sAMAccountName = request.getParameter("username");
		logger.info("Username: "+sAMAccountName);
		
		// reading the account request file
		String filename = request.getParameter("filename");
		if( filename == null){ //if filename was deleted from the disk
			response.getWriter().write("false|This request no longer exists.");
		}
		String action = request.getParameter("action");
		File outFolder = new File(LdapProperty.getProperty(LdapConstants.OUTPUT_FOLDER));
		File file = new File(outFolder, filename);
		// if file doesn't exist or can't be read
		if(!file.exists()){
			logger.error(ErrorConstants.FAIL_READING_ACCT_REQUEST + file.getName());
			response.getWriter().write("false|"+ErrorConstants.FAIL_READING_ACCT_REQUEST + file.getName());
			return;
		}

		
		Map<String, String[]> maps = null;
		try{
			processFile(file);
		} catch (IOException e){
			response.getWriter().write("false|"+ErrorConstants.FAIL_READING_ACCT_REQUEST + file.getName());
			return;
		}
		if(maps == null){
			response.getWriter().write("false|"+ErrorConstants.FAIL_READING_ACCT_REQUEST + file.getName());
			return;
		}
		
		// if account request is declined
		if(action.equals("decline")){
			if(file.delete()){
				EmailClient.sendEmailRejected(maps.get("mail")[0], maps.get("displayName")[0]);
				response.getWriter().write("true|Account request has been declined.");
			}
			
		// if account request is accepted
		}else{
			//MODIFIED CODE - SPT-447
			//Handle code for null and blank usernames SEPARATELY
			//Previously handled together risking Null Pointer Exception
			// validate the sAMAccountName
			if( sAMAccountName == null ){
				response.getWriter().write("false|User was not added with invalid username.");
				return;
			}else if( sAMAccountName.equals("") ){
				response.getWriter().write("false|User was not added with invalid username.");
				return;
			}
			//MODIFIED CODE ENDS
			
			maps.put("sAMAccountName", new String[]{sAMAccountName});
			
			int clientAccountId = -1;
			try {
				clientAccountId = SupportTrackerJDBC.addClient(maps);
			} catch (SQLException e1) {
				e1.printStackTrace();
				response.getWriter().write("false|"+e1.getMessage());
				return;
				//no need to log, the error has been logged in SupportTrackerJDBC.addClient(maps)
			}
			
			if( clientAccountId > 0 ){
				maps.put("info", new String[]{Integer.toString(clientAccountId)});
				
				
				LdapTool lt = null;
				try {
					lt = new LdapTool();
				} catch (FileNotFoundException fe){		
					response.getWriter().write("false|"+fe.getMessage());
					return;
					//no need to log, the error has been logged in LdapTool()
				} catch (NamingException e) {
					response.getWriter().write("false|"+e.getMessage());
					return;
					//no need to log, the error has been logged in LdapTool()
				}
				if( lt == null){
					logger.error("Unknown Error while connecting to LDAP server");
					response.getWriter().write("false|Unknown Error while connecting to LDAP server");
					return;
				}
				
				
				boolean addStatus = lt.addUser(maps);
				if( addStatus ){
					file.delete();
					String fullname = "";
					if(maps.get("displayName")[0] != null){
						fullname = maps.get("displayName")[0];
					}else{
						fullname = maps.get("givenName")[0] + " " + maps.get("sn")[0];
					}
					
					try {
						ConcertoAPI.addClientUser(maps.get("sAMAccountName")[0], Integer.toString(clientAccountId), fullname, maps.get("description")[0], maps.get("mail")[0]);
					} catch (ServiceException e) {
						response.getWriter().write("false|"+e.getMessage());
						return;
						//no need to log, the error has been logged in ConcertoAPI.addClientUser()
					}
					
					EmailClient.sendEmailApproved(maps.get("mail")[0], maps.get("displayName")[0], maps.get("sAMAccountName")[0], maps.get("password01")[0]);
					response.getWriter().write("true|User "+maps.get("displayName")[0]+" was added successfully with user id: "+maps.get("sAMAccountName")[0]);
				}else{
					response.getWriter().write("false|User "+maps.get("displayName")[0]+" was not added.");
				}
			}else{
				response.getWriter().write("false|User "+maps.get("displayName")[0]+" was not added to database.");
			}
		}
	}
	public void doPost(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException
	{
		doGet(request, response);
	}
	
	
	
	
	
	/**
	 * read the given file and store all the attributes in that file into HashMap object, and return it.
	 * @param file given .xml file that stores the account request info
	 * @return a HashMap object that stores all the account request info
	 * @throws IOException IOException if it failed to create DocumentBuilder object 
	 *         or failed to parse the DocumentBuilder contents into a DOM object
	 *         or failed to read the account requests from the account requests storage folder
	 *         or the given file doesn't exist or a null object
	 */
	private Map<String, String[]> processFile(File file) throws IOException{
		if(file==null || !file.exists()){
			logger.error(ErrorConstants.FAIL_READING_ACCT_REQUEST + file.getName());
			throw new IOException(ErrorConstants.FAIL_READING_ACCT_REQUEST + file.getName());
		}
		
		try {
			// build DocumentBuilder object and parse the file contents to DOM
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(file);
			NodeList fields = doc.getElementsByTagName("field");
			logger.info("Filename: "+file.getName());
			
			// read DOM attributes and put into a HashMap
			HashMap<String, String[]> maps = new HashMap<String, String[]>();  
			for( int j = 0; j < fields.getLength(); j++ ){
				maps.put(fields.item(j).getAttributes().item(0).getTextContent(), new String[]{fields.item(j).getTextContent()});
			}
			maps.put("password01", new String[]{"password"});
			//maps.put("sAMAccountName", new String[]{maps.get("givenName")[0].toLowerCase()+String.valueOf(maps.get("sn")[0].charAt(0)).toLowerCase()});
			maps.put("filename", new String[]{file.getName()});
			maps.put("isLdapClient", new String[]{"true"});
			return maps;
			
		} catch (ParserConfigurationException e) {
			logger.error("Failed to create DocumentBuilder from file: "+file.getName(), e);
			new IOException(ErrorConstants.FAIL_READING_ACCT_REQUEST + file.getName());
		} catch (SAXException e) {
			logger.error("Failed to parse docBuilder contents into DOM object from file: "+file.getName(), e);
			new IOException(ErrorConstants.FAIL_READING_ACCT_REQUEST + file.getName());
		} catch (IOException e) {
			logger.error("Failed to parse docBuilder contents into DOM object from file: "+file.getName(), e);
			new IOException(ErrorConstants.FAIL_READING_ACCT_REQUEST + file.getName());
		}
		return new HashMap<String, String[]>();
	}
}
