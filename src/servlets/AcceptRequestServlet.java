package servlets;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import tools.AccountHelper;
import tools.EmailClient;
import tools.PasswordGenerator;

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
		
		if( filename == null || filename.trim().isEmpty()){ //if given filename is null 
			response.getWriter().write("false|This request no longer exists.");
			return;
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
		if(maps == null || maps.size() < 16){
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
				
			String sAMAccountName = AccountHelper.getLoginNameFromRequest(request);
			if(sAMAccountName.contains("false|")){
				// if sAMAccountName contains false => sAMAccountName is the reason of the failure in retreiving the sAMAccountName
				response.getWriter().write(sAMAccountName);
				return;
			}
			maps.put("sAMAccountName", new String[]{sAMAccountName});
			
			boolean genRandPsw = false;
			if(request.getParameter("psw")==null || request.getParameter("psw").trim().equals("GenPsw")){
				String psw = PasswordGenerator.generatePswForLength(8);
				maps.put("password01", new String[]{psw});
				genRandPsw = true;
			} else {
				maps.put("password01", new String[]{request.getParameter("psw")});
			}
			
			String result = AccountHelper.createAccount(maps, genRandPsw);
			
			if(result.contains("true")){
				//the user account has been created successfully => delete the file from the disk
				file.delete();
			}
			
			response.getWriter().write(result);
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
