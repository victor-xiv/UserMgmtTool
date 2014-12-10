package tools;

import java.io.File;
import java.io.FileInputStream;
import java.util.Enumeration;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.log4j.Logger;

public class CountryCode {
	
	
	/**
	 * read CountryCode.xml (pathToTomcatConfFolder/CountryCode.xml) assign the key,value pair into countries TreeMap
	 * @return a TreeMap that contains key,value pair of all countries. i.e. {(AF, Afghanistan), ...}
	 */
	public static TreeMap<String,String> getCountryNameMap(){
		Logger logger = Logger.getRootLogger(); // initiate as a default root logger
		logger.debug("reading country code from pathToTomcatConfFolder/ContryCode.xml");
		
		Properties props = new Properties();
		File home = new File(getCatalinaBase());
		File conf = new File(home, "conf");
        File properties = new File(conf, "CountryCode.xml");
        TreeMap<String,String> countries = new TreeMap<String,String>();
        try{
        	FileInputStream fis = new FileInputStream(properties);
        	props.loadFromXML(fis);
        	
        	for(Enumeration<Object> e = props.keys(); e.hasMoreElements(); ){
    			String key = (String)e.nextElement();
    			countries.put(props.getProperty((String)key), key);
    		}
		}catch(Exception ex){
			logger.error(ex);
		}
        
        logger.debug("finished reading country code from pathToTomcatConfFolder/ContryCode.xml");
        
		return countries;
	}
	
	
	
	/**
	 * read CountryCode.xml (pathToTomcatConfFolder/CountryCode.xml) assign the key,value pair into countries TreeMap
	 * @return a TreeMap that contains key,value pair of all countries. i.e. {(AF, Afghanistan), ...}
	 */
	public static TreeMap<String,String> getCountryCodeMap(){
		Logger logger = Logger.getRootLogger(); // initiate as a default root logger
		
		logger.debug("getting country code in a Map object");
		
		Properties props = new Properties();
		File home = new File(getCatalinaBase());
		File conf = new File(home, "conf");
        File properties = new File(conf, "CountryCode.xml");
        TreeMap<String,String> countries = new TreeMap<String,String>();
        try{
        	FileInputStream fis = new FileInputStream(properties);
        	props.loadFromXML(fis);
        	
        	for(Enumeration<Object> e = props.keys(); e.hasMoreElements(); ){
    			String key = (String)e.nextElement();
    			countries.put(key, props.getProperty((String)key));
    		}
		}catch(Exception ex){
			logger.error(ex);
		}
        
        logger.debug("finished getting country code in a Map object");
        
		return countries;
	}
	
	
	
	/**
	 * return a string of country that matches to the given country name
	 * @param name : requested Country name
	 * @return Country name that matches to the given name
	 */
	public static String getCountryByName(String name){
		TreeMap<String,String> countries = getCountryNameMap();
		return countries.get(name);
	}
	
	
	
	/**
	 * retrun a string of country that matches to the given country code
	 * @param code : requested country code
	 * @return Country code that matches to the given country code.
	 */
	public static String getCountryByCode(String code){
		TreeMap<String,String> countries = getCountryCodeMap();
		return countries.get(code);
	}
	
	
	
	/**
	 * get the path (in String) to Tomcat conf folder
	 * @return path to Tomcat conf folder (in String)
	 */
	private static String getCatalinaBase() {
        return System.getProperty("catalina.base", getCatalinaHome());
    }
	
	
	
	/**
	 * get the path (in String) to Tomcat folder
	 * @return path to Tomcat folder (in String)
	 */
    private static String getCatalinaHome() {
        return System.getProperty("catalina.home",
                                  System.getProperty("user.dir"));
    }
}
