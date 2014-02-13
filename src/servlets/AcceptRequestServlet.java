package servlets;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import ldap.LdapConstants;
import ldap.LdapProperty;
import ldap.LdapTool;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import tools.ConcertoAPI;
import tools.EmailClient;
import tools.SupportTrackerJDBC;

@SuppressWarnings("serial")
public class AcceptRequestServlet extends HttpServlet {
	Logger logger = Logger.getRootLogger();
	
	public void doGet(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException
	{
		String sAMAccountName = request.getParameter("username");
		logger.info("Username: "+sAMAccountName);
		String filename = request.getParameter("filename");
		String action = request.getParameter("action");
		File outFolder = new File(LdapProperty.getProperty(LdapConstants.OUTPUT_FOLDER));
		File file = new File(outFolder, filename);
		if( filename == null ){
			response.getWriter().write("false|This request no longer exists.");
		}else if(action.equals("decline")){
			Map<String, String[]> maps = processFile(file);
			if(file.delete()){
				EmailClient.sendEmailRejected(maps.get("mail")[0], maps.get("displayName")[0]);
				response.getWriter().write("true|Account request has been declined.");
			}
		}else{
			//MODIFIED CODE - SPT-447
			//Handle code for null and blank usernames SEPARATELY
			//Previously handled together risking Null Pointer Exception
			if( sAMAccountName == null ){
				response.getWriter().write("false|User was not added with invalid username.");
				return;
			}
			if( sAMAccountName.equals("") ){
				response.getWriter().write("false|User was not added with invalid username.");
				return;
			}
			//MODIFIED CODE ENDS
			Map<String, String[]> maps = processFile(file);
			maps.put("sAMAccountName", new String[]{sAMAccountName});
			int clientAccountId = SupportTrackerJDBC.addClient(maps);
			if( clientAccountId > 0 ){
				maps.put("info", new String[]{Integer.toString(clientAccountId)});
				LdapTool lt = new LdapTool();
				boolean addStatus = lt.addUser(maps);
				if( addStatus ){
					file.delete();
					String fullname = "";
					if(maps.get("displayName")[0] != null){
						fullname = maps.get("displayName")[0];
					}else{
						fullname = maps.get("givenName")[0] + " " + maps.get("sn")[0];
					}
					ConcertoAPI.addClientUser(maps.get("sAMAccountName")[0], Integer.toString(clientAccountId), fullname, maps.get("description")[0], maps.get("mail")[0]);
					EmailClient.sendEmailApproved(maps.get("mail")[0], maps.get("displayName")[0], maps.get("sAMAccountName")[0], maps.get("password01")[0]);
					response.getWriter().write("true|User "+maps.get("displayName")[0]+" was added successfully with user id: "+maps.get("sAMAccountName")[0]);
				}else{
					response.getWriter().write("false|User "+maps.get("displayName")[0]+" was not added.");
				}
			}else{
				response.getWriter().write("false|User "+maps.get("displayName")[0]+" was not added to database.");
			}
		}
	}
	public void doPost(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException
	{
		doGet(request, response);
	}
	
	private Map<String, String[]> processFile(File file){
		try {
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(file);
			NodeList fields = doc.getElementsByTagName("field");
			logger.info("Filename: "+file.getName());
			HashMap<String, String[]> maps = new HashMap<String, String[]>();  
			for( int j = 0; j < fields.getLength(); j++ ){
				maps.put(fields.item(j).getAttributes().item(0).getTextContent(), new String[]{fields.item(j).getTextContent()});
			}
			maps.put("password01", new String[]{"password"});
			//maps.put("sAMAccountName", new String[]{maps.get("givenName")[0].toLowerCase()+String.valueOf(maps.get("sn")[0].charAt(0)).toLowerCase()});
			maps.put("filename", new String[]{file.getName()});
			maps.put("isLdapClient", new String[]{"true"});
			return maps;
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
		return new HashMap<String, String[]>();
	}
}
