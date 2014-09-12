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
	
	
	
	// all below constants are used for reading the values from configuration file
	public static final String REJECTED_SUBJECT = "rejected.subject";
	public static final String REJECTED_BODY = "rejected.body";
	
	public static final String APPROVED_SUBJECT = "approved.subject";
	public static final String APPROVED_BODY = "approved.body";
	
	public static final String REPLACEKEY_RECIPIENTNAME = "UsrMgmt_RECIPIENTNAME";
	public static final String REPLACEKEY_USERNAME = "UsrMgmt_USERNAME";
	public static final String REPLACEKEY_PASSWORD = "UsrMgmt_PASSWORD";
	
	
	
	// comment out the password part
	// Recipient name
	// username
	// template of the email contents (prefer reading from file)
	// reading from config file:
		// subject
		//
	public static void sendEmailApproved(String mailTo, String recipientName, String username, String password){
		init();
		Logger logger = Logger.getRootLogger(); // initiate as a default root logger
		
		String mailSubject = LdapProperty.getProperty(APPROVED_SUBJECT);
		String mailBody = LdapProperty.getProperty(APPROVED_BODY);
		mailBody = mailBody.replace(REPLACEKEY_RECIPIENTNAME, recipientName);
		mailBody = mailBody.replace(REPLACEKEY_USERNAME, username);
		mailBody = mailBody.replace(REPLACEKEY_PASSWORD, password);
		
		Session session = Session.getDefaultInstance(mailServerConfig, null);
		MimeMessage message = new MimeMessage(session);
		try {
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(mailTo));
			message.setFrom(new InternetAddress(LdapProperty.getProperty(EmailConstants.MAIL_FROM)));
			message.setSubject(mailSubject);
			message.setText(mailBody);
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
		Logger logger = Logger.getRootLogger(); // initiate as a default root logger
		
		String mailSubject = LdapProperty.getProperty(REJECTED_SUBJECT);
		String mailBody = LdapProperty.getProperty(REJECTED_BODY);
		mailBody = mailBody.replace(REPLACEKEY_RECIPIENTNAME, recipientName);
		
		Session session = Session.getDefaultInstance(mailServerConfig, null);
		MimeMessage message = new MimeMessage(session);
		
		try {
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(mailTo));
			message.setFrom(new InternetAddress(LdapProperty.getProperty(EmailConstants.MAIL_FROM)));
			message.setSubject(mailSubject);
			message.setText(mailBody);
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
		mailServerConfig.put("mail.smtp.host", LdapProperty.getProperty(EmailConstants.MAIL_HOST));
	}
}
