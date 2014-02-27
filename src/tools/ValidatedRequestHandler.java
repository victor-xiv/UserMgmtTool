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
			ex.printStackTrace();
		}catch(Exception e){
			e.printStackTrace();
		}
		return parameters;
	}
}

/*


try{
	final SecretKey key = CryptoProvider.getInstance().generateKey(keyString);
} catch (IllegalArgumentException ie) {
	// either algorithm or key is null or incorrect
}

try {
			httpRequest = Concerto4xSecurityHttpServletRequestMarshaller.unmarshal(request, key, VALIDITY_TIME);
		} catch (final ValidationException e) {
			throw new InvalidRequestException(e);
		} catch (final ExpiredException e) {
			throw new InvalidRequestException(e);
		}catch(java.lang.SecurityException sce){
			System.out.println("error=The server failed to decrypt the requests and authentication.\nThis failure caused by the Encryption Key Name/Value is not matched.");
			sce.printStackTrace();
		}


 */
// 
