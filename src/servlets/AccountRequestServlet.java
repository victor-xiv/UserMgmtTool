package servlets;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import ldap.LdapConstants;
import ldap.LdapProperty;
import ldap.LdapTool;

import tools.ValidatedRequestHandler;

@SuppressWarnings("serial")
public class AccountRequestServlet extends HttpServlet {
	Logger logger = Logger.getRootLogger();
	
	public void doGet(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException
	{
		Hashtable<String, String> parameters = ValidatedRequestHandler.processRequest(request);
		HttpSession session = request.getSession(true);
		if(parameters.containsKey("error")){
			session.setAttribute("error", parameters.get("error"));
		}else if(parameters.containsKey("userDN")){
			String userDN = parameters.get("userDN");
			session.setAttribute("userDN", parameters.get("userDN"));
			LdapTool lt = new LdapTool();
			String company = lt.getUserCompany(userDN);
			session.setAttribute("company", company);
		}else{
			session.setAttribute("error", "This page can only be accessed from within Concerto.");
		}
		String redirectURL = response.encodeRedirectURL("AccountRequestForm.jsp");
		response.sendRedirect(redirectURL);
	}

	@SuppressWarnings("unchecked")
	public void doPost(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException
	{
		HttpSession session = request.getSession(true);
		String username = request.getParameter("sAMAccountName");
		//Email (ADDITIONAL VARIABLE)
		String email = request.getParameter("mail");
		LdapTool lt = new LdapTool();
		//ADDITIONAL CODE - SPT-320 and SPT-314
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
		} else /*ADDITIONAL CODE ENDS*/ if(outputRequest((HashMap<String,String[]>)request.getParameterMap())){
			session.setAttribute("passed", "Your request has been submitted.");
		}else{
			session.setAttribute("error", "An error occured upon submitting your request. "+
					"Please contact the server administrator for assistance or email your "+
					"request to support@orionhealth.com");
		}
		lt.close();
		String redirectURL = response.encodeRedirectURL("AccountRequestForm.jsp");
		response.sendRedirect(redirectURL);
	}
	
	private boolean outputRequest(HashMap<String, String[]> paramMaps){
		SimpleDateFormat sdf = new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		Date date = new Date();
		StringBuffer filenameBuffer = new StringBuffer();
		sdf.format(date, filenameBuffer, new FieldPosition(0));
		if(!paramMaps.containsKey("userDN")){
			logger.error("userDN not found");
			return false;
		}
		File outFolder = new File(LdapProperty.getProperty(LdapConstants.OUTPUT_FOLDER));
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
		}catch(IOException e){
			logger.error(e.toString());
			e.printStackTrace();
			return false;
		}
		return true;
	}
}