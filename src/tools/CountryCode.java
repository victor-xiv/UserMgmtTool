package tools;

import java.io.File;
import java.io.FileInputStream;
import java.util.Enumeration;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.log4j.Logger;

public class CountryCode {
	private static Logger logger = Logger.getRootLogger();
	public static TreeMap<String,String> getCountryNameMap(){
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
			logger.error(ex.toString());
			ex.printStackTrace();
		}
		return countries;
	}
	
	public static TreeMap<String,String> getCountryCodeMap(){
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
			logger.error(ex.toString());
			ex.printStackTrace();
		}
		return countries;
	}
	
	public static String getCountryByName(String name){
		TreeMap<String,String> countries = getCountryNameMap();
		return countries.get(name);
	}
	
	public static String getCountryByCode(String code){
		TreeMap<String,String> countries = getCountryCodeMap();
		return countries.get(code);
	}
	
	private static String getCatalinaBase() {
        return System.getProperty("catalina.base", getCatalinaHome());
    }
	
    private static String getCatalinaHome() {
        return System.getProperty("catalina.home",
                                  System.getProperty("user.dir"));
    }
}
