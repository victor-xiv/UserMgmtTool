package tools;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import ldap.LdapConstants;
import ldap.LdapProperty;

import com.concerto.sdk.security.InvalidRequestException;
import com.concerto.sdk.security.ValidatedRequest;
import com.concerto.security.RedirectionKey;

public class ValidatedRequestHandler {
	private static Logger logger = Logger.getRootLogger();

	public static Hashtable<String, String> processRequest(HttpServletRequest request){
		Hashtable<String, String> parameters = new Hashtable<String, String>();
		try{
			ValidatedRequest req = new ValidatedRequest(request, LdapProperty.getProperty(LdapConstants.CONCERTO_VALIDATOR));
			for(Enumeration<String> e = req.getParameterNames(); e.hasMoreElements(); ){
				String paramName = e.nextElement();
				parameters.put(paramName, req.getParameter(paramName));
				logger.info(paramName+": "+req.getParameter(paramName));
			}
		}catch(InvalidRequestException ex){
			parameters.put("error", "This page can only be accessed from within Concerto.");
		}
		return parameters;
	}
}
