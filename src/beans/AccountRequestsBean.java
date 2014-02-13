package beans;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import tools.CountryCode;
import tools.SupportTrackerJDBC;

import ldap.LdapConstants;
import ldap.LdapProperty;

public class AccountRequestsBean {
	Logger logger = Logger.getRootLogger();
	
	ArrayList<HashMap<String, String>> requests = new ArrayList<HashMap<String, String>>();

	public void refresh(){
		extractRequests();
	}
	
	public void setRequests(ArrayList<HashMap<String, String>> requests){
		this.requests = requests;
	}
	public ArrayList<HashMap<String, String>> getRequests(){
		return this.requests;
	}
	
	public String[] getAvailableNames(String firstname, String surname){
		return SupportTrackerJDBC.getAvailableUsernames(firstname, surname);
	}
	
	private void extractRequests(){
		requests = new ArrayList<HashMap<String, String>>();
		try {
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			File outFolder = new File(LdapProperty.getProperty(LdapConstants.OUTPUT_FOLDER));
			File[] xmlFiles = outFolder.listFiles();
			//ADDITIONAL CODE - SPT-446
			//Handle null case by creating empty array
			if (xmlFiles == null) {
				xmlFiles = new File[0];
			}
			//ADDITIONAL CODE ENDS
			for(int i = 0; i < xmlFiles.length; i++){
				File file = xmlFiles[i];
				if(file.getName().endsWith(".xml")){
					Document doc = docBuilder.parse(file);
					NodeList fields = doc.getElementsByTagName("field");
					logger.info("Filename: "+file.getName());
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
					requests.add(maps);
				}
			}
		} catch (ParserConfigurationException e) {
			logger.error(e.toString());
			e.printStackTrace();
			System.exit(0);
		} catch (SAXException e) {
			logger.error(e.toString());
			e.printStackTrace();
		} catch (IOException e) {
			logger.error(e.toString());
			e.printStackTrace();
		}
	}
}
