package beans;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

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

import tools.CountryCode;
import tools.LoggerTool;
import tools.SupportTrackerJDBC;

public class AccountRequestsBean {
	Logger logger = Logger.getRootLogger(); // initiate as a default root logger
	
	ArrayList<HashMap<String, String>> requests = new ArrayList<HashMap<String, String>>();

	
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
	public void setRequests(ArrayList<HashMap<String, String>> requests){
		this.requests = requests;
	}
	
	
	/**
	 * return all the account requests that requested to be created.
	 * @return all account requests
	 */
	public ArrayList<HashMap<String, String>> getRequests(){
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
	 * @throws IOException if it failed to create DocumentBuilder object 
	 *         or failed to parse the DocumentBuilder contents into a DOM object
	 *         or failed to read the account requests from the account requests storage folder
	 */
	private void extractRequests() throws IOException{
		requests = new ArrayList<HashMap<String, String>>();
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
			
			File[] xmlFiles = outFolder.listFiles();
			//ADDITIONAL CODE - SPT-446
			//Handle null case by creating empty array
			if (xmlFiles == null) {
				xmlFiles = new File[0];
			}

			for(int i = 0; i < xmlFiles.length; i++){
				// read xml file, put the content into DocumentBuilder object
				File file = xmlFiles[i];
				if(file.getName().endsWith(".xml")){
					// parse docBuilder contents into a DOM object (doc)
					Document doc = docBuilder.parse(file);
					NodeList fields = doc.getElementsByTagName("field");
					logger.info("Filename: "+file.getName());

					// get the data from DOM and store into HashMap
					HashMap<String, String> maps = new HashMap<String, String>();  
					for( int j = 0; j < fields.getLength(); j++ ){
						maps.put(fields.item(j).getAttributes().item(0).getTextContent(), fields.item(j).getTextContent());
						logger.info(fields.item(j).getAttributes().item(0).getTextContent()+"|"+fields.item(j).getTextContent());
					}
					maps.put("filename", file.getName());
					String countryCode = maps.get("c");
					if( !countryCode.equals("") && countryCode != null){
						maps.put("co", CountryCode.getCountryByCode(countryCode));
					}
					// put HashMap data into request (array of hashmap)
					requests.add(maps);
				}
			}
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
