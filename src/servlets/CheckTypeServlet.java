package servlets;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import ldap.EmailConstants;
import ldap.LdapProperty;
import ldap.LdapTool;
import ldap.UserMgmtConstants;

import org.apache.log4j.Logger;

import tools.ConcertoAPI;

import com.concerto.sdk.security.AuthenticatedRequest;

public class CheckTypeServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	//Set up logging
	Logger logger = Logger.getRootLogger();
    
    /**
     * @see HttpServlet#HttpServlet()
     */
    public CheckTypeServlet() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		//If unauthorised access, redirect to initial form
		response.sendRedirect("ForgotPassword.jsp");
	}
	

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		//Initialise constants
		//String conserver = "supporttracker.orionhealth.com";
		String conserver = LdapProperty.getProperty(UserMgmtConstants.CONCERTO_URL);//"jonathanf-vm";
		String smtp = LdapProperty.getProperty(EmailConstants.MAIL_HOST);//"zimbra.orion.internal";
		String appserver = request.getServerName()+":"+request.getServerPort();//"http://jonathanf-vm";
		String supportMail = "support@orionhealth.com";
		
		//Get http objects
		HttpSession session = request.getSession();
		//Get parameters
		String username = request.getParameter("user");
		String organisation = request.getParameter("org");
		boolean ldap = false;
		String err = null;
		
		//Search LDAP users
		if (true) {
			logger.info("Connecting to LDAP server.");
			
			LdapTool lt = null;
			try {
				lt = new LdapTool();
			} catch (FileNotFoundException fe){
				logger.error("Cannot connect to LDAP server.", fe);
				
				session.setAttribute("message", 
						"A LDAP error occured upon submitting your request: "+fe.getMessage()+
						"<br />Please contact the server administrator for assistance or email your "+
						"request to <a href=mailto:support@orionhealth.com>support@orionhealth.com</a>");
			} catch (NamingException e) {
				logger.error("Cannot connect to LDAP server.", e);
				
				session.setAttribute("message", 
						"A LDAP error occured upon submitting your request: "+e.getMessage()+
						"<br />Please contact the server administrator for assistance or email your "+
						"request to <a href=mailto:support@orionhealth.com>support@orionhealth.com</a>");
			}
			
			//Check if this user actually exists
			String name = lt.getName(username, organisation);
			String mail = lt.getEmail(username, organisation);
			if (name!=null) {
				//If user exists, email link to password change screen.
				String userDN = "CN="+name+",OU="+organisation+",OU=Clients,DC=orion,DC=dmz";
				session.setAttribute("message", sendMail(smtp, mail, userDN, name, appserver, supportMail) );
				ldap = true;
			}
			//Close LdapTool
			lt.close();
		}
		
		if (!ldap) {
			//If not found in LDAP, check against concerto users
			// Get the locator for the web service
			try {
				
				if(ConcertoAPI.doesUserExist(username)){
					//If successful, user is a Concerto user. Redirect to concerto password utility
					response.sendRedirect(conserver+"/password/ForgotPassword.action");
					return;
				} else {
					// No user was found matching the user ID
					session.setAttribute("message", 
							"User not found. Please ensure your username and organisation are correct. ");
				}
				
			}  catch( Exception e ) {
				e.printStackTrace();
				session.setAttribute("message", 
							"A Concerto error occured upon submitting your request: "+e.getMessage()+
							"<br />Please contact the server administrator for assistance or email your "+
							"request to <a href=mailto:support@orionhealth.com>support@orionhealth.com</a>");
			}
		}
		
		//Redirect to original page
		response.sendRedirect("ForgotPassword.jsp");
	}
	
	//Print HTML Header
	//Parameters: out - handle to print output
	//conserver - URL of concerto server
	protected void printHeader(PrintWriter out, String conserver) {
		out.println("<html>");
		out.println("<head>");
		out.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\">");
		out.println("<title>Checking Details...</title>");
		out.println("<link type=\"text/css\" rel=\"stylesheet\" href=\"http://<%= conserver %>/concerto/common/css/core.css\">");
		out.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"http://<%= conserver %>/concerto/common/css/widget.css\">");
		out.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"http://<%= conserver %>/concerto/css/inputtypes.css\">");
		out.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"http://<%= conserver %>/concerto/css/concerto.css\">");
		out.println("<script src=\"http://"+conserver+"/concerto/javascript/Concerto.js\" type=\"text/javascript\"></script>");
		out.println("<script src=\"http://"+conserver+"/concerto/javascript/Dialog.js\" type=\"text/javascript\"></script>");
		out.println("</head>");
		out.println("<body>");
	}
	
	//Print HTML Footer
	//Parameter out - handle to print output
	protected void printFooter(PrintWriter out) {
		out.println("</body>");
		out.println("</html>");
	}
	
	//Email link to password reset page
	//Parameters:
	//* smtp - specifies the smtp used to send mail
	//* to - specifies the email address of the user
	//* userDN - the user's LDAP distinct name
	//* name - the user's name
	//* appserver - the application server hosting PasswordConfirmer.jsp
	//* supportMail - the sender's address, e.g. support@orionhealth.com
	//Returns message to be displayed.
	protected String sendMail(String smtp, String to, String userDN, String name, 
			String appserver, String supportMail) {
		//Created new encrypted request and set up
		AuthenticatedRequest code = new AuthenticatedRequest();
        code.setUrl("http://"+appserver+"/UserMgmt/PasswordConfirmer.jsp");
        //Ensure that this is the same as in PasswordConfirmer.jsp
        code.setSecretKey("8290A7F6A2DD5A79C32CC0EF2F9D9280");
        //Valid for 5 minutes
        code.setValidityTime(300);
        code.setMethod("GET");
        //Include user as query parameter
        code.addQueryParameter("user", userDN);
        try {
        	//Encrypt message
        	code.encrypt();
        	//If error, return error message
        } catch (Exception e) {
        	return "An encryption error occured upon submitting your request: "+e.getMessage()+
			"<br />Please contact the server administrator for assistance or email your "+
			"request to <a href=mailto:support@orionhealth.com>support@orionhealth.com</a>";
        }
        //Set up link to password reset page
		String resetLink = "<a href="+code.getQueryURL()+">"+code.getQueryURL()+"</a>";
		
		//Set up link to support email
		String supportLink = "<a href=mailto:"+supportMail+">"+supportMail+"</a>";
		
		//Set up session with default properties + specified smtp mail host
		java.util.Properties properties = System.getProperties();
		properties.setProperty("mail.smtp.host", smtp);
		Session session = Session.getDefaultInstance(properties);
		
		//Create and set up new email message
		MimeMessage mail = new MimeMessage(session);
		try {
			//Set sender
			mail.setFrom(new InternetAddress(supportMail));
			mail.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
			mail.setSubject("Password Change Authorized");
			mail.setContent("Hello "+name+" <br /><br />To reset your Support Tracker password visit"+
					" the link below. <br /><br />"+resetLink+"<br /><br />If clicking on the link does "+
					"not work, copy and paste it into a new web browser window instead. "+
					"<br /><br />If you did not request to change your password, please contact "+
					"the help desk immediately at "+supportLink+".<br /><br />"+
					"This link will expire 5 minutes after this message was sent." +
					"<br /><br />NOTE: This message "+
					"was sent from an unmonitored email address. Replies sent to this "+
					"address will not be answered.<br />", "text/html");
			//Send email
			Transport.send(mail);
			//On error, print error and quit
		} catch (AddressException e) {
			e.printStackTrace();
			return "A mailing error occured upon submitting your request: "+e.getMessage()+
					"<br />Please contact the server administrator for assistance or email your "+
					"request to <a href=mailto:support@orionhealth.com>support@orionhealth.com</a>";
		} catch (MessagingException e) {
			e.printStackTrace();
			return "A messaging error occured upon submitting your request: "+e.getMessage()+
					"<br />Please contact the server administrator for assistance or email your "+
					"request to <a href=mailto:support@orionhealth.com>support@orionhealth.com</a>";
		}
		//Print success and quit
		return "Password change request submitted. A link to the password change web page "
				+"has been emailed to you.";
	}
}
