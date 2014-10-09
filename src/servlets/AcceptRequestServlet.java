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
import tools.SupportTrackerJDBC;

@SuppressWarnings("serial")
public class AcceptRequestServlet extends HttpServlet {
	
	Logger logger = Logger.getRootLogger(); // initiate as a default root logger
	
	
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
		logger.debug("AcceptRequestServlet processing Get request: " + request.getQueryString());
		
		// reading the account request file
		String filename = request.getParameter("filename");
		if( filename == null){ //if given filename is null 
			response.getWriter().write("false|This request no longer exists.");
		}
		
		String action = request.getParameter("action");
		File outFolder = new File(LdapProperty.getProperty(LdapConstants.OUTPUT_FOLDER));
		File file = new File(outFolder, filename);
		// if file doesn't exist or can't be read from the folder configured in ldap.properties (value of output.folder)
		// there can be an issue that the configured property in ldap.propreties is not correct (Please double check, if u run into file doesn't exist problem).
		if(!file.exists()){
			logger.error(ErrorConstants.FAIL_READING_ACCT_REQUEST + file.getName());
			response.getWriter().write("false|"+ErrorConstants.FAIL_READING_ACCT_REQUEST + file.getName());
			return;
		}
		
		Map<String, String[]> maps = null; // used to stored attributes for the new account that will be created on ldap server
		try{
			maps = processFile(file); //get all the user's properties from the file
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
			// delete file and send rejected email to client
			if(file.delete()){
				try{
					EmailClient.sendEmailRejected(maps.get("mail")[0], maps.get("displayName")[0]);
				} catch (Exception e){
					logger.error("Account request has been declined. But it couldn't send out a rejected email to: " + maps.get("mail")[0] + ". Because: " + e.getMessage(), e);
					response.getWriter().write("true|Account request has been declined. But it couldn't send out a rejected email to: " + maps.get("mail")[0] + ". Because: " + e.getMessage());
				}
				
				response.getWriter().write("true|Account request has been declined.");
			} else {
				response.getWriter().write("false|Failed to delete the stored request.");
				return;
			}
			
		// if account request is accepted
		}else{
				
			
			
			/**
			 * From here to the end of this method is duplicated with servlet.AddUserSevlet.doPost().
			 * I can't refactor and make this part to a single method and let them use a single method.
			 * because, they are too strong couple to their respective .jsp.
			 * 
			 * So, if you update this part, please double check servlet.AddUserSevlet.doPost(), you might
			 * also need to update that part as well.
			 */
			
			
			// sAMAccountName used as LDAP logon (i.e username)
			// it is allowed to have only these special chars:  ( ) . - _ ` ~ @ $ ^  and other normal chars [a-zA-Z0-9]
			String sAMAccountName = request.getParameter("username");
			
			//MODIFIED CODE - SPT-447
			//Handle code for null and blank usernames SEPARATELY
			//Previously handled together risking Null Pointer Exception
			// validate the sAMAccountName
			if( sAMAccountName == null ){
				response.getWriter().write("false|User was not added with invalid username.");
				return;
			}else if( sAMAccountName.trim().equals("") ){
				response.getWriter().write("false|User was not added with invalid username.");
				return;
			}
			//MODIFIED CODE ENDS
			
			// check if sAMAccountName contains any prohibited chars
			String temp = sAMAccountName.replaceAll("[\\,\\<\\>\\;\\=\\*\\[\\]\\|\\:\\~\\#\\+\\&\\%\\{\\}\\?]", "");
			if(temp.length() < sAMAccountName.length()){
				response.getWriter().write("false|Username contains some forbid speical characters. The special characters allowed to have in username are: ( ) . - _ ` ~ @ $ ^");
				return;
			}
			logger.debug("Username: "+sAMAccountName);
			
			maps.put("sAMAccountName", new String[]{sAMAccountName});
			
			
			// connecting to LDAP server
			LdapTool lt = null;
			try {
				lt = new LdapTool();
			} catch (FileNotFoundException fe){	
				response.getWriter().write("false|Could not connect to LDAP server due to: "+fe.getMessage());
				return;
				//no need to log, the error has been logged in LdapTool()
			} catch (NamingException e) {
				response.getWriter().write("false|Could not connect to LDAP server due to: "+ e.getMessage());
				return;
				//no need to log, the error has been logged in LdapTool()
			}
			if( lt == null){
				logger.error("Unknown Error while connecting to LDAP server");
				response.getWriter().write("false|Unknown Error while connecting to LDAP server");
				return;
			}
			
			// if company doesn't exist in LDAP's "Client" => add the company into "Client"
			if(!lt.companyExists(maps.get("company")[0])){
				try {
					if(!lt.addCompany(maps.get("company")[0])){
						// if companyName doesn't exist in "Client" and can't be added, just return false;
						response.getWriter().write("false|The organisation of requesting user doesn't exist and couldn't be added into LDAP's Clients.");
						return;
					}
				} catch (NamingException e) {
					response.getWriter().write("false|The organisation of requesting user doesn't exist and couldn't be added into LDAP's Clients.");
					return;
				}
			}
			// if company doesn't exist in LDAP's "Groups"
			if(!lt.companyExistsAsGroup(maps.get("company")[0])){
				try {
					//  add the company into "Groups"
					if(!lt.addCompanyAsGroup(maps.get("company")[0])){
						// if adding company into group failed
						response.getWriter().write("false|The organisation of requesting user doesn't exist and couldn't be added into LDAP's Groups.");
						return;
					}
				} catch (NamingException e) {
					response.getWriter().write("false|The organisation of requesting user doesn't exist and couldn't be added into LDAP's Groups.");
					return;
				}
			}
			
			// fullname is used to check whether this name exist in LDAP and concerto.
			// and used to add into concerto
			String fullname = "";
			if(maps.get("displayName")[0] != null) 	fullname = maps.get("displayName")[0];
			else 	fullname = maps.get("givenName")[0] + " " + maps.get("sn")[0];
			
			// these variable used for adding a user into ConcertoAPI
			String firstName = maps.get("givenName")[0];
			String lastName = maps.get("sn")[0];
			String userName = maps.get("sAMAccountName")[0];
			String description = maps.get("description")[0];
			String mail = maps.get("mail")[0];

			// check if username exist in LDAP or Concerto
			boolean usrExistsInLDAP = lt.usernameExists(fullname, maps.get("company")[0]);
			boolean usrExistsInConcerto = false;
			try {
				usrExistsInConcerto = ConcertoAPI.doesUserExist(userName);
			} catch (Exception e) {
				response.getWriter().write("false|Cannot connect to Concerto server. Reason: " + e.getMessage());
				return;
			}
			
			if(usrExistsInLDAP){
				response.getWriter().write("false|Requesting user already exists in LDAP server");
				return;
			} else if(usrExistsInConcerto){
				response.getWriter().write("false|Requesting user already exists in Concerto server");
				return;
			}
			
			// ADDING USER ACCOUNT CODE STARTS FROM HERE \\
			
			int clientAccountId = -1;
			try {
				clientAccountId = SupportTrackerJDBC.addClient(maps);
			} catch (SQLException e1) {
				e1.printStackTrace();
				response.getWriter().write("false|"+e1.getMessage());
				return;
				//no need to log, the error has been logged in SupportTrackerJDBC.addClient(maps)
			}
			
			// if add a user into Supprt Tracker successfully
			if( clientAccountId > 0 ){
				maps.put("info", new String[]{Integer.toString(clientAccountId)});
				
				// add user into LDAP server
				boolean addStatus = false;
				
				try{
					addStatus = lt.addUser(maps);
				} catch (Exception e){
					response.getWriter().write("false|User "+maps.get("displayName")[0]+" was not added, due to: " + e.getMessage());
				}
				
				if( addStatus ){ // add a user into Ldap successfully
					// delete the file from the disk
					file.delete();
					
					try {
						ConcertoAPI.addClientUser(userName, firstName, lastName, fullname, description, mail, Integer.toString(clientAccountId));
					} catch (Exception e) {
						response.getWriter().write("false|User "+maps.get("displayName")[0]+" was added to LDAP and Support Tracker. But it couldn't be added to Concerto Portal.");
					}
					
					try{
						EmailClient.sendEmailApproved(maps.get("mail")[0], maps.get("displayName")[0], maps.get("sAMAccountName")[0], maps.get("password01")[0]);
					} catch (Exception e){
						
						String rsp = "true|User "+maps.get("displayName")[0]+" was added successfully with user id: "+maps.get("sAMAccountName")[0] + ". " +
								"But, it couldn't send out an approval email to: " + maps.get("mail")[0] +". Because: " + e.getMessage();
						logger.error(rsp, e);
						response.getWriter().write(rsp);
					}
					response.getWriter().write("true|User "+maps.get("displayName")[0]+" was added successfully with user id: "+maps.get("sAMAccountName")[0]);
				
				}else{ // add a user into Ldap is not successful
					
					// remove the previous added user from Support Tracker DB
					deletePreviouslyAddedClientFromSupportTracker(clientAccountId);
					
					response.getWriter().write("false|User "+maps.get("displayName")[0]+" was not added, due to the failure in adding the user into LDAP server.");
				}
			}else{
				response.getWriter().write("false|User "+maps.get("displayName")[0]+" was not added, due to the failure in adding the user into Support Tracker DB.");
			}
			
			/**
			 * duplicated codes end here
			 */
			
			
		}
	}
	public void doPost(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException
	{
		doGet(request, response);
	}
	
	
	
	/**
	 * a helper method to help doGet() to delete clientAccountId from the Support Tracker DB. (this method is used to avoid duplicate code only)
	 * It was successful to add a user into support tracker (and the clientAccountId was returned from support tracker).
	 * But, it was unsuccessful to add that user to LDAP. So, we need to delete this newly added clientAccountId from Support Tracker.
	 * @param clientAccountId
	 */
	public static void deletePreviouslyAddedClientFromSupportTracker(int clientAccountId){
		Logger logger = Logger.getRootLogger(); // initiate as a default root logger
		
		// remove the previous added user from Support Tracker DB
		try {
			SupportTrackerJDBC.deleteClient(clientAccountId);
		} catch (SQLException e) {
			logger.error("An exception occured while deleting this clientAccountId: " + clientAccountId);
		}
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
		logger.debug("about to process the file: " + file.getName());
		
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
			logger.debug("Filename: "+file.getName());
			
			// read DOM attributes and put into a HashMap
			HashMap<String, String[]> maps = new HashMap<String, String[]>();  
			for( int j = 0; j < fields.getLength(); j++ ){
				maps.put(fields.item(j).getAttributes().item(0).getTextContent(), new String[]{fields.item(j).getTextContent()});
			}
			maps.put("password01", new String[]{"password"});
			//maps.put("sAMAccountName", new String[]{maps.get("givenName")[0].toLowerCase()+String.valueOf(maps.get("sn")[0].charAt(0)).toLowerCase()});
			maps.put("filename", new String[]{file.getName()});
			maps.put("isLdapClient", new String[]{"true"});
			
			logger.debug("Finished processing the file: " + file.getName());
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
