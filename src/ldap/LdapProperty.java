package ldap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Properties;

import org.apache.log4j.Logger;

public class LdapProperty {
	private static Logger logger = Logger.getRootLogger();

	public static Properties getConfiguration(){
		Properties props = new Properties();
		File home = new File(getCatalinaBase());
		File conf = new File(home, "conf");
        File properties = new File(conf, "ldap.properties");
        try{
        	FileInputStream fis = new FileInputStream(properties);
        	props.load(fis);
		}catch(FileNotFoundException fe){
			props.setProperty("error", "LDAP " + ErrorConstants.CONFIG_FILE_NOTFOUND);
			logger.error("LDAP" + ErrorConstants.CONFIG_FILE_NOTFOUND, fe);
			fe.printStackTrace();
			
		}catch(Exception ex){
			props.setProperty("error", "LDAP " + ErrorConstants.CONFIG_FILE_NOTFOUND);
			logger.error("LDAP" + ErrorConstants.CONFIG_FILE_NOTFOUND, ex);
			ex.printStackTrace();
		}
        
		return props;
	}
	
	public static String getProperty(String name){
		Properties props = getConfiguration();
		return props.getProperty(name); 
	}
	
	private static String getCatalinaBase() {
        return System.getProperty("catalina.base", getCatalinaHome());
    }
	
    private static String getCatalinaHome() {
        return System.getProperty("catalina.home",
                                  System.getProperty("user.dir"));
    }
}