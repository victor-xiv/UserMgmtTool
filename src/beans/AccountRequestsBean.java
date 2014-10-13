package beans;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

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

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils.Collections;

import tools.CountryCode;
import tools.SupportTrackerJDBC;

public class AccountRequestsBean {
	Logger logger = Logger.getRootLogger(); // initiate as a default root logger
	
	TreeMap<String, List<Map<String, String>>> requests = new TreeMap<String, List<Map<String, String>>>();

	public static int getDisplayNameSizeLimit() {
		int size = 20;
		String temp = LdapProperty.getProperty("supporttracker.displayname.sizelimit");
		try {
			size = Integer.parseInt(temp);
		} catch (NumberFormatException | NullPointerException e) {
			Logger.getRootLogger().debug("Cannot pareInt of displayName size limit. " + temp);
		}
		return size;
	}
	
	
	/**
	 * re-read the account requests
	 * @throws IOException 
	 */
	public void refresh() throws IOException{
		extractRequests();
	}
	
	
	/**
	 * set all the account requests
	 * @param requests - account requests
	 */
	public void setRequests(TreeMap<String, List<Map<String, String>>> requests){
		this.requests = requests;
	}
	
	
	/**
	 * return all the account requests that requested to be created.
	 * @return all account requests
	 */
	public TreeMap<String, List<Map<String, String>>> getRequests(){
		return this.requests;
	}
	
	/**
	 * Use the given firstname and surname to produce a set of possible and available names
	 * @param firstname
	 * @param surname
	 * @return an array of String of possible and available name
	 * @throws SQLException if it failed to connect to DB server, failed to execute the queries or failed to close the connectioin
	 */
	public String[] getAvailableNames(String firstname, String surname) throws SQLException{
		return SupportTrackerJDBC.getAvailableUsernames(firstname, surname);
	}
	
	
	/**
	 * Read the account requests from the .xml files that are stored in the 
	 * LdapProperty.getProperty(LdapConstants.OUTPUT_FOLDER)
	 * and stored those requests into this.requests 
	 * 
	 * 1). read each .xml file
	 * 2). get the responsible staff for that request from Support Tracker DB
	 * 3). create a HashMap object to store this request
	 * 4). add this request (HashMap object) into the list that response by this staff
	 * 5). add responsileStaff as the key of the requests object
	 *          and the list of the requests that this staff responsible as the value of the requests object
	 * 
	 * @return a map object, where:
	 *  + its key is the name of the staff that responsible for the requests
	 *  + its value is the list of the request that this staff responsible for
	 *  => each request in this list is the Map object, where key is the attribute of the user, and value is the value of that attribute
	 *  
	 * @Note: if you want to see, how requests object look like, please uncomment the print lines in this method.
	 *   
	 * @throws IOException if it failed to create DocumentBuilder object 
	 *         or failed to parse the DocumentBuilder contents into a DOM object
	 *         or failed to read the account requests from the account requests storage folder
	 */
	private void extractRequests() throws IOException{
		logger.debug("About to read the request *.xml file from the file system: " + LdapProperty.getProperty(LdapConstants.OUTPUT_FOLDER));
		requests = new TreeMap<String, List<Map<String, String>>>();
		try {
			// building DocumentBuilder
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			File outFolder = new File(LdapProperty.getProperty(LdapConstants.OUTPUT_FOLDER));
			
			// outFolder doesn't exist because of the incorrect configured in ldap.properties
			// 								   or it failed to read from that folder
			if(!outFolder.exists()){
				String foldername = LdapProperty.getProperty(LdapConstants.OUTPUT_FOLDER);
				logger.error(ErrorConstants.FAIL_READING_ACCT_REQUEST + foldername );
				throw new IOException(ErrorConstants.FAIL_READING_ACCT_REQUEST + foldername);
			}
			
			// we need to order the request based on the date it created.
			// thats why, we convert the array of files to TreeSet object.
			TreeSet<File> xmlFiles = new TreeSet<File>(Arrays.asList(outFolder.listFiles()));
			
			//ADDITIONAL CODE - SPT-446
			//Handle null case by creating empty array
			if (xmlFiles == null) {
				xmlFiles = new TreeSet<File>();
			}

			for(File file : xmlFiles){
				// read xml file, put the content into DocumentBuilder object
				//File file = xmlFiles[i];
				if(file.getName().endsWith(".xml")){
					// parse docBuilder contents into a DOM object (doc)
					Document doc = docBuilder.parse(file);
					NodeList fields = doc.getElementsByTagName("field");
					logger.debug("Reading a file called: "+file.getName());

					// get the data from DOM and store into HashMap
					String responsibleStaff = null;
					HashMap<String, String> maps = new HashMap<String, String>();  
					for( int j = 0; j < fields.getLength(); j++ ){
						String key = fields.item(j).getAttributes().item(0).getTextContent(); 
						maps.put(key, fields.item(j).getTextContent());
						
						// looking for the company field
						// get the company name from the company field
						// find the staff who responsible for this request
						if(key.equalsIgnoreCase("company")){
							try {
								responsibleStaff = SupportTrackerJDBC.getResponsibleStaff(fields.item(j).getTextContent());
							} catch (SQLException e) {
								responsibleStaff = null;
							}
						}
						logger.debug(fields.item(j).getAttributes().item(0).getTextContent()+"|"+fields.item(j).getTextContent());
					}
					
					
					String createdDate = null;
					try {
						createdDate = file.getName().replace(".xml", "");
						SimpleDateFormat sdf = new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
						Date date = sdf.parse(createdDate);
						
						sdf = new SimpleDateFormat ("yyyy-MM-dd");
						createdDate = sdf.format(date);
					} catch (Exception e) {
						BasicFileAttributes attrs = Files.readAttributes(Paths.get(file.getPath()), BasicFileAttributes.class);
						createdDate = attrs.creationTime().toString();
						createdDate = createdDate.substring(0, createdDate.indexOf('T'));
					}
					maps.put("createdDate", createdDate);
					maps.put("filename", file.getName());
					String countryCode = maps.get("c");
					if( !countryCode.equals("") && countryCode != null){
						maps.put("co", CountryCode.getCountryByCode(countryCode));
					}
					
					// if there's no resposibleStaff stored in support tracker DB (when responsibleStaff==null)
					// set responsibleStaff as "Others"
					if(responsibleStaff ==  null) responsibleStaff = "Others";

					// add this request into its corresponding list
					ArrayList<Map<String, String>> requestResponsibleByThisStaff = null;
					if(requests.containsKey(responsibleStaff)){
						requestResponsibleByThisStaff = (ArrayList<Map<String, String>>) requests.get(responsibleStaff);
					} else {
						requestResponsibleByThisStaff = new ArrayList<Map<String, String>>();
					}
					
					requestResponsibleByThisStaff.add(maps);
					requests.put(responsibleStaff, requestResponsibleByThisStaff);
				}
			}
			
		// Note: if you want to see, how requests object look like, pls uncomment these lines
//			for(Map.Entry<String, List<Map<String, String>>> rqls : requests.entrySet()){
//				String responsibleStaff = rqls.getKey();
//				System.out.println(responsibleStaff);
//				List<Map<String, String>> requestList = rqls.getValue();
//				int i = 1;
//				for(Map<String, String> request : requestList){
//					System.out.println("\t" + i++ + ").");
//					for(Map.Entry<String, String> attr_value : request.entrySet()){
//						System.out.println("\t" + attr_value.getKey() + " : " + attr_value.getValue());
//					}
//				}
//			}
			
			logger.debug("Finished reading the request *.xml.");
			
		} catch (ParserConfigurationException e) {
			String foldername = LdapProperty.getProperty(LdapConstants.OUTPUT_FOLDER);
			logger.error("Failed to create DocumentBuilder", e);
			new IOException(ErrorConstants.FAIL_READING_ACCT_REQUEST + foldername);
		} catch (SAXException e) {
			String foldername = LdapProperty.getProperty(LdapConstants.OUTPUT_FOLDER);
			logger.error("Failed to parse docBuilder contents into DOM object", e);
			new IOException(ErrorConstants.FAIL_READING_ACCT_REQUEST + foldername);
		} catch (IOException e) {
			String foldername = LdapProperty.getProperty(LdapConstants.OUTPUT_FOLDER);
			logger.error("Failed to parse docBuilder contents into DOM object", e);
			new IOException(ErrorConstants.FAIL_READING_ACCT_REQUEST + foldername);
		}
	}
}
