//done

package tools;

import java.util.Hashtable;

import javax.crypto.SecretKey;
import javax.naming.ldap.Rdn;
import javax.servlet.http.HttpServletRequest;

import ldap.ErrorConstants;
import ldap.LdapConstants;
import ldap.LdapProperty;

import org.apache.log4j.Logger;

import com.orchestral.common.net.RequestParameter;
import com.orchestral.common.net.RequestParameterList;
import com.orchestral.common.net.http.HttpRequest;
import com.orchestral.security.CryptoProvider;
import com.orchestral.security.concerto4.ExpiredException;
import com.orchestral.security.concerto4.ValidationException;
import com.orchestral.servlet.security.Concerto4xSecurityHttpServletRequestMarshaller;

public class ValidatedRequestHandler {
	
	/**
	 * It received an HttpServletRequest that contains encrypted parameters
	 * It validates and decrypts this request. The decrypted parameter name-value pairs are added into the return HashTable object
	 * @param request: contains encrypted request (http://hostname:port/servletname?encryptedValue=596F79C5F207BDD7A...5E9686F6&mac=018A2F282...98C872C&expiry=000001443DC26658)
	 * @return a Hashtable object that contains name-value pairs of the decrypted request's paramters. 
	 */
	public static Hashtable<String, String> processRequest(HttpServletRequest request){
		Logger logger = Logger.getRootLogger(); // initiate as a default root logger
		
		
		Hashtable<String, String> parameters = new Hashtable<String, String>();	
		HttpRequest req = null;
		
		try{ // validate and decrypt the encrypted request
			
			logger.info("Attempt to validate and decrypt the encrypted request");
			
			String sharedKeyStr = LdapProperty.getProperty(LdapConstants.CONCERTO_VALIDATOR);
			
			// if the LDAP configuration file is not found, the sharedKeyStr is null
			if ( sharedKeyStr == null ){
				parameters.put("error", "LDAP " + ErrorConstants.CONFIG_FILE_NOTFOUND);
				// logger has logged this error in the LdapProperty.getProperty
				// so, we don't need to re-log it again.
				
			} else {
				// initialize the shared key from the key value
				final SecretKey key = CryptoProvider.getInstance().generateKey(sharedKeyStr);
				// validity time of one minute (doesn't matter as long as it is non-zero)
				// if validity time is zero, means expiry would not be checked.
				final long VALIDITY_TIME = 60000;
				req = Concerto4xSecurityHttpServletRequestMarshaller.unmarshal(request, key, VALIDITY_TIME);
				logger.info("Request validation and decryption has been done successfully.");
			}
		
		} catch (IllegalArgumentException | SecurityException e) {
			parameters.put("error", ErrorConstants.INCORRECT_SHAREDKEY);
			logger.error("Validation Error", e);
			
		} catch (final ValidationException e) {
			parameters.put("error", ErrorConstants.VALIDATION_FAILED);
			logger.error("Validation Error", e);
			
		} catch (final ExpiredException e) {
			parameters.put("error", ErrorConstants.EXPIRED_REQUEST);
			logger.error("Validation Error", e);
			
		} catch (Exception e) {
			parameters.put("error", ErrorConstants.UNKNOWN_ERR);
			logger.error("Validation Error", e);
		}
		
		if (req == null){
			// req is null, if and only if decryption fail and threw an exception => parameters must contains one key "error".
			// so, if req is null and parameters is empty => means some thing wrong without exception. 
			if (parameters.isEmpty()){
				parameters.put("error", ErrorConstants.UNKNOWN_ERR);
				logger.error("Validation is completed without exception being thrown, but the decrypted request is null.");
				return parameters;
			}
			
		} else {
			RequestParameterList reqParaList = req.getQueryParameters();
			for ( RequestParameter rp : reqParaList.getAllParameters()) {
				String paraName = rp.getName();
				String paraValue = rp.getValue();
				if(paraName.equals("userDN")){
					// if paraName="userDN" cotains an empty String paraValue, then we don't need to put it into the parameters
					if(!paraValue.trim().isEmpty()){
						// if the paraName is "userDN". its value is a complete dn-name that sent from portal.
						// this dn-name is a name that has been escaped the reserve chars. (e.g. 
						// so, we need to clean up that escaped chars, before using them. (e.g. after clean up: CN=Lisa, She/pherd,OU=Hospira Pty limited *Project*,OU=Clients,DC=orion,DC=dmz)
						paraValue = (String) Rdn.unescapeValue(paraValue);
						parameters.put(paraName, paraValue);
					}
				} else {
					// we put any other keys (paraName) regardless of its value (paraValue)
					parameters.put(paraName, paraValue);
				}
				logger.info("Put name-value pair into parameter list: " + paraName+"-"+paraValue);
			}
		}
		
		return parameters;
	}
}
