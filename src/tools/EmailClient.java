package tools;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;

import ldap.EmailConstants;
import ldap.LdapProperty;
//import ldap.LdapTool;

public class EmailClient {
	private static Properties mailServerConfig = new Properties();
	private static String mailSubject;
	private static StringBuffer mailBody = new StringBuffer();
	private static Logger logger = Logger.getRootLogger();
	
	
	// comment out the password part
	// Recipient name
	// username
	// template of the email contents (prefer reading from file)
	// reading from config file:
		// subject
		//
	public static void sendEmailApproved(String mailTo, String recipientName, String username, String password){
		init();
		mailSubject = "Support Tracker Access Approved";
		mailBody.append("Dear "+recipientName+",\n\n");
		mailBody.append("Your account request to Support Tracker has been approved.\n");
		mailBody.append("Please note your login details are:\n\n");
		mailBody.append("Username: "+username+"\n");
		mailBody.append("Password: "+password+"\n\n");
		mailBody.append("You can now access http://supporttracker.orionhealth.com with the above login.\n\n");
		mailBody.append("Kind Regards,\n");
		mailBody.append("Orion Health Support");
		Session session = Session.getDefaultInstance(mailServerConfig, null);
		MimeMessage message = new MimeMessage(session);
		try {
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(mailTo));
			message.setSubject(mailSubject);
			message.setText(mailBody.toString());
			Transport.send(message);
		} catch (AddressException e) {
			logger.error(e.toString());
			e.printStackTrace();
		} catch (MessagingException e) {
			logger.error(e.toString());
			e.printStackTrace();
		}
	}
	
	public static void sendEmailRejected(String mailTo, String recipientName){
		init();
		mailSubject = "Support Tracker Access Rejected";
		mailBody.append("Dear "+recipientName+",\n\n");
		mailBody.append("Your account request to Support Tracker has been declined.\n");
		mailBody.append("Please email your request to support@orionhealth.com\n\n");
		mailBody.append("Kind Regards,\n");
		mailBody.append("Orion Health Support");
		Session session = Session.getDefaultInstance(mailServerConfig, null);
		MimeMessage message = new MimeMessage(session);
		try {
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(mailTo));
			message.setSubject(mailSubject);
			message.setText(mailBody.toString());
			Transport.send(message);
		} catch (AddressException e) {
			logger.error(e.toString());
			e.printStackTrace();
		} catch (MessagingException e) {
			logger.error(e.toString());
			e.printStackTrace();
		}
	}
	
	private static void init(){
		mailBody = new StringBuffer();
		mailServerConfig.put("mail.host", LdapProperty.getProperty(EmailConstants.MAIL_HOST));
		mailServerConfig.put("mail.from", LdapProperty.getProperty(EmailConstants.MAIL_FROM));
	}
}
