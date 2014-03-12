package tools;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

public class LoggerTool {
	
	/**
	 * Setup a root logger, set up the NDC context; and return the created logger.
	 * @param request - used to grab the ip-address and session id of request to setup the NDC.
	 * @return a ready-to-use root logger.
	 */
	public static Logger setupRootLogger(HttpServletRequest request){
		Logger logger = Logger.getRootLogger();
		
		// create ndc. ndc is containing requesterIPaddress-sessionID
		// e.g. ndc = "172.68.92.23-"
		String ndc = request.getRemoteAddr();
		HttpSession session = request.getSession(true);
		ndc += "-" + session.getId();
		
		// if the top ndc on the stack is the same as this ndc, means the context is from
		// the same person at the same time. don't need to do anything.
		if( NDC.getDepth() != 0 && NDC.peek().equals(ndc)){
			return logger;
		}
		
		// remove all the NDC contexts. avoiding duplicate pushing the same NDC contexts
		try{
			while( NDC.getDepth() != 0 ){
				NDC.peek();
			}
		} catch (Exception e) { // catch general exception, because NDC.pop() is not throwing any exception
								// so, if there is any exceptions, means something weird happened.
			logger.error("Exception occured while removing NDC properties.", e);
		}
		
		// push a ndc into the stack
		try{
			NDC.push(ndc);
		} catch (Exception e) { // catch general exception, because NDC.pop() is not throwing any exception
			// so, if there is any exceptions, means something weird happened.
			logger.error("Exception occured while removing NDC properties.", e);
		}
		
		return logger;
	}
	
	
	
	/**
	 * Setup a root logger, set up the NDC context; and return the created logger.
	 * @param ndc - suggested NDC context. But, if the NDC stack contains a previous context, then keep using the previous one without over writing.
	 * @return a ready-to-use root logger.
	 */
	public static Logger setupRootLogger(String ndc){
		Logger logger = Logger.getRootLogger();
		
		// if there is a previous context in the NDC stack
		// keep using the previous one
		if (NDC.getDepth() == 0){
			NDC.push(ndc);
		}
		
		return logger;
	}
	
	
	/**
	 * Return the default log4j's RootLogger.
	 * @return the default log4j's RootLogger
	 */
	public static Logger setupDefaultRootLogger(){
		return Logger.getRootLogger();
	}
}
