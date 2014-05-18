package servlets;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import ldap.ErrorConstants;
import ldap.LdapConstants;
import ldap.LdapProperty;
import ldap.LdapTool;

import org.apache.log4j.Logger;

import tools.LoggerTool;
import tools.ValidatedRequestHandler;

@SuppressWarnings("serial")
public class AccountRequestServlet extends HttpServlet {
	Logger logger = LoggerTool.setupDefaultRootLogger(); // initialize as a default root logger
	
	
	
	/**
	 * Serve the client's request by presenting the Account Request Form page:
	 * + First Validate and decrypt the request
	 * + Connect to Ldap server to get the company name
	 * + redirect the reqeust to AccountRequestForm.jsp
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException
	{
		logger = LoggerTool.setupRootLogger(request);
		
		Hashtable<String, String> parameters = ValidatedRequestHandler.processRequest(request);
		HttpSession session = request.getSession(true);
		if(parameters.containsKey("error")){
			session.setAttribute("error", parameters.get("error"));
			// don't need to log here because, ValidatedRequestHandler.processRequest() has
			// already logged this error.
			
		}else if(parameters.containsKey("userDN")){
			String userDN = parameters.get("userDN");
			session.setAttribute("userDN", parameters.get("userDN"));
			
			LdapTool lt = null;
			try {
				lt = new LdapTool();

				// configuration file is not found
			} catch (FileNotFoundException fe){
				session.removeAttribute("userDN");
				session.setAttribute("error", fe.getMessage());
				// no need to log, the error has been logged in LdapTool()
				
			// connection fail
			} catch (NamingException e) {
				session.removeAttribute("userDN");
				session.setAttribute("error", e.getMessage());
				// no need to log, the error has been logged in LdapTool()
			} catch (Exception e){
				session.removeAttribute("userDN");
				session.setAttribute("error", e.getMessage());
				logger.error("Unknown Exception during connecting to LDAP server.", e);
			}
			
			// if lt is null, means the connection is fail => do nothing
			// the lt is not null, the connection is successfully established
			if( lt != null){
				String company = lt.getUserCompany(userDN);
				session.setAttribute("company", company);
				lt.close();
			}
			
		}else{
			session.setAttribute("error", "This page can only be accessed from within Concerto.");
			logger.error(ErrorConstants.UNKNOWN_ERR + " The page might be accessed outside Concerto.");
			
		}
		String redirectURL = response.encodeRedirectURL("AccountRequestForm.jsp");
		response.sendRedirect(redirectURL);
	}

	
	
	/**
	 * Received the detail of the account (that user requesting to be created) through Request's parameters
	 * Process that account detail and write it to an .xml file.
	 * that account request file is the concatenation of LdapConstants.OUTPUT_FOLDER and the processing date/time
	 * e.g. /opt/LDAP/2014-03-13T10:58:32.995+1300.xml
	 */
	@SuppressWarnings("unchecked")
	public void doPost(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException
	{
		HttpSession session = request.getSession(true);
		String username = request.getParameter("sAMAccountName");
		//Email (ADDITIONAL VARIABLE)
		String email = request.getParameter("mail");
		
		/**
		 * connecting to ldap server
		 */
		LdapTool lt = null;
		try {
			lt = new LdapTool();

			// configuration file is not found
		} catch (FileNotFoundException fe){
			session.setAttribute("error", fe.getMessage());
			// no need to log, the error has been logged in LdapTool()
			
		// connection fail
		} catch (NamingException e) {
			session.setAttribute("error", e.getMessage());
			// no need to log, the error has been logged in LdapTool()
			
		} catch (Exception e){
			session.setAttribute("error", e.getMessage());
			logger.error("Unknown Exception during connecting to LDAP server.", e);
		}
		
		// if lt is null, means the connection is fail => do nothing
		// the lt is not null, the connection is successfully established
		if( lt != null){
			
			//Check if userDN already exists
			if (lt.userDNExists(request.getParameter("displayName"), request.getParameter("company"))) {
				//If so, create error message and return as message to display
				String message = "<font color=\"red\">";
				message = "Unable to create user account. ";
				message += "An account has already been created for this user. <br />";
				message += "Please contact Orion Health Support: <ul>";
				message += "<li>Phone </li>";
				message += "<li>Email <a href=\"mailto:support@orionhealth.com\">support@Orionhealth.com</a> </li>";
				message += "<li>Raise a Ticket </li></ul>";
				message += "</font>";
				session.setAttribute("error", message);
				logger.info("Username '"+username+"' already exists.");
			}
			//Check if email already used in account
			else if (lt.emailExists(email, request.getParameter("company"))) {
				//If so, create error message and return as message to display
				String message = "<font color=\"red\">";
				message = "Unable to create user account. ";
				message += "An account with this email address ("+email+") already exists. <br />";
				message += "Please contact Orion Health Support: <ul>";
				message += "<li>Phone </li>";
				message += "<li>Email <a href=\"mailto:support@orionhealth.com\">support@Orionhealth.com</a> </li>";
				message += "<li>Raise a Ticket </li></ul>";
				message += "</font>";
				session.setAttribute("error", message);
				logger.info("Email '"+email+"' already in use.");
			}
			
			else{
				// write the request out into the persistant file
				try{
					outputRequest((HashMap<String,String[]>)request.getParameterMap());
				}catch(IOException e){
					String errorMessage = e.getMessage() 
							+ "Please contact the server administrator for assistance or email your "
							+ "request to support@orionhealth.com";
					
					session.setAttribute("error", errorMessage);
					// no need to log, the error has been logged in outputRequest()
					
				} catch (Exception e){
					session.setAttribute("error", e.getMessage());
					logger.error("Unknown Exception during connecting to LDAP server.", e);
				}
				
				// if the outputRequest didn't through an exception, 
				// then there's no attribute "error" in the session
				if( session.getAttribute("error") == null){
					session.setAttribute("passed", "Your request has been submitted.");
				}
				
			}
			lt.close();
		}
		
		String redirectURL = response.encodeRedirectURL("AccountRequestForm.jsp");
		response.sendRedirect(redirectURL);
	}
	
	
	
	/**
	 * write the request account into outFolder.
	 * outFolder is the concatenation of LdapConstants.OUTPUT_FOLDER and the processing date/time
	 * e.g. /opt/LDAP/2014-03-13T10:58:32.995+1300.xml
	 * @param paramMaps
	 * @return true if the method wrote out to the file successfully, false otherwise.
	 * @throws IOException 
	 */
	private boolean outputRequest(HashMap<String, String[]> paramMaps) throws IOException{
		// the request that need to be written out must contains "userDN"
		if(!paramMaps.containsKey("userDN")){
			logger.error("userDN not found");
			return false;
		}
		
		// create an output filename (stored in filenameBuffer)
		// the filename is the date and time which the request is being process
		SimpleDateFormat sdf = new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		Date date = new Date();
		StringBuffer filenameBuffer = new StringBuffer();
		sdf.format(date, filenameBuffer, new FieldPosition(0));		
				
		// open the stream file and write the request into that file
		File outFolder = new File(LdapProperty.getProperty(LdapConstants.OUTPUT_FOLDER));
		// if the OUTPUT_FOLDER doesn't exist, try to make one
		if(!outFolder.exists()){ 
			if(!outFolder.mkdirs()){
				logger.error(ErrorConstants.FAIL_MKDIR + LdapProperty.getProperty(LdapConstants.OUTPUT_FOLDER));
				throw new FileNotFoundException(ErrorConstants.FAIL_MKDIR + LdapProperty.getProperty(LdapConstants.OUTPUT_FOLDER) + ". ");
			}
		}
		
		try {
			FileWriter fw = new FileWriter(new File(outFolder, filenameBuffer.toString())+".xml");
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write("<?xml version=\"1.0\"?>");
			bw.newLine();
			bw.write("<request>");
			bw.newLine();
			Iterator<Map.Entry<String, String[]>> it = paramMaps.entrySet().iterator();
			while(it.hasNext()){
				Map.Entry<String, String[]> map = (Map.Entry<String, String[]>)it.next();
				//MODIFIED CODE - SPT-445
				String val = map.getValue()[0];
				val = val.replaceAll("\\&", "&amp;");
				bw.write("\t<field name=\""+map.getKey()+"\">"+val+"</field>");
				//MODIFIED CODE
				bw.newLine();
				logger.info(map.getKey()+"="+map.getValue()[0]);
			}
			bw.write("</request>");
			bw.close();
			fw.close();
		
		// if the FileWriter object can't be created 
		}catch(FileNotFoundException e){
			String filename = outFolder + filenameBuffer.toString()+".xml";
			logger.error(ErrorConstants.FAIL_MK_FILE + filename, e);
			throw new FileNotFoundException(ErrorConstants.FAIL_MK_FILE + filename + ". ");
			
		// IOException can be occurred during opening file stream, writing the contents out or closing the stream.
		}catch(IOException e){
			String filename = outFolder + filenameBuffer.toString()+".xml";
			logger.error(ErrorConstants.FAIL_WRITE_ACCT_REQUEST + filename, e);
			throw new IOException(ErrorConstants.FAIL_WRITE_ACCT_REQUEST + filename);
		}
		return true;
	}
}