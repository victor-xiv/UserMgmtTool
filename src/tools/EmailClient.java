package tools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import ldap.EmailConstants;
import ldap.LdapProperty;
//import ldap.LdapTool;


import org.apache.log4j.Logger;

public class EmailClient {
	private static Properties mailServerConfig = new Properties();
	
	
	
	// all below constants are used for reading the values from configuration file
	public static final String REJECTED_SUBJECT = "rejected.subject";
	public static final String REJECTED_BODY_FILE_PATH = "rejected.body.file.path";

	public static final String APPROVED_SUBJECT = "approved.subject";
	public static final String APPROVED_GENERATEDPSW_BODY_FILE_PATH = "approved.generatedpsw.body.file.path";
	public static final String APPROVED_MANUALPSW_BODY_FILE_PATH = "approved.manualpsw.body.file.path";
	public static final String APPROVED_PASSWORD_FILE_PATH = "approved.password.file.path";

	public static final String REPLACEKEY_RECIPIENTNAME = "UsrMgmt_RECIPIENTNAME";
	public static final String REPLACEKEY_USERNAME = "UsrMgmt_USERNAME";
	public static final String REPLACEKEY_PASSWORD = "UsrMgmt_PASSWORD";
	
	
	
	/**
	 * prepare the sms content of the new password and send to the given user
	 * @param mobile : mobile phone number of the receiver
	 * @param recipientName : the reciever name (it can be an empty string, but not null)
	 * @param newPsw : new password that the receiver should use to log in into support tracker
	 * @throws MessagingException :if the sms could not be sent through
	 */
	public static void sendNewPasswordToSMS(String mobile, String recipientName, String newPsw) throws MessagingException{
		// prepare the content
		String smsBody = "Your new password is\n\r" + newPsw + "\n\rPlease use this password to login to Support Tracker. For more information, please contact " + LdapProperty.getProperty("mail.from");
		sendSMSto(mobile, recipientName, smsBody);
	}
	
	/**
	 * send an SMS to the given mobile phone number, with the given smsBody
	 * @param mobile phone number that will receive SMS. e.g. +64213456789. This phone number
	 * must be a valid one. If it is an invalid, the method will try to send, but, it will go silent
	 * without returning back a feedback.
	 * @param recipientName : the reciever name (it can be an empty string, but not null)
	 * @param smsSubject the subject of the sms (it can be an empty string, but not null)
	 * @param smsBody the body of the txt message
	 * @throws MessagingException if the given mobile is not valid or some error occured during the sending process
	 */
	public static void sendSMSto(String mobile, String recipientName, String smsBody) throws MessagingException{
		init();
		Logger logger = Logger.getRootLogger(); // initiate as a default root logger
		
		logger.debug("Preparing to send an SMS to: " + recipientName + " who has: " + mobile);
		
		// validate mobile number
		mobile = SupportTrackerJDBC.cleanUpAndValidateMobilePhone(mobile);
		if(mobile == null){
			throw new MessagingException("The given mobile phone is not valid");
		}
		
		// prepare the mobile address. mobile address = mobile number + the domain of the msg server
		// the domain of the msg server is configured in ldap.properties
		String mobileAsSMS4UmailAddress = mobile + LdapProperty.getProperty("txt.msg.server.domain");
		
		
		Session session = Session.getDefaultInstance(mailServerConfig, null);
		MimeMessage message = new MimeMessage(session);
		try {
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(mobileAsSMS4UmailAddress));
			message.setFrom(new InternetAddress(LdapProperty.getProperty("mail.from")));
			message.setSubject("");
			message.setText(smsBody);
			Transport.send(message);

		} catch (MessagingException e) {
			logger.error("Could not send out an sms", e);
			throw e;
		}
		logger.debug("finished sending sms");
	}
	
	
	/**
	 * 	 * This method should be used for only when the password is generated randomly
	 * and we have the user's valid mobile phone number
	 * 
	 * 1). Read the approved email template, prepare the approved email that contains the given username
	 *     and send it to the given email address (mailTo)
	 * 2). send the given password to the given mobile phone number via sms
	 * 
	 * @param mailTo : email address of the receiver
	 * @param recipientName : receiver name
	 * @param username : username that receiver should use to log in into support tracker
	 * @param mobile : mobile phone number of the receiver
	 * @param password : new password that the receiver should use to log in into support tracker
	 * @throws Exception if the email or sms could not be sent through
	 */
	public static void sendEmailForApprovedRequestWithGeneratedPsw(String mailTo, String recipientName, String username, String mobile, String password) throws Exception{
		Logger logger = Logger.getRootLogger(); // initiate as a default root logger
		
		logger.debug("preparing an email for approved request with generated password");
		
		String mailSubject = LdapProperty.getProperty(APPROVED_SUBJECT);
		
		String mailBodyFilePath = LdapProperty.getProperty(APPROVED_GENERATEDPSW_BODY_FILE_PATH);
		String mailBody = "";
		// reading mail body
		BufferedReader br = null;
		try{
			br = new BufferedReader(new FileReader(mailBodyFilePath));
			String s=null;
			while((s = br.readLine()) != null){
				mailBody += s;
			}
		} catch (IOException e) {
			logger.error("Couldn't read the template of mail body content file " + mailBodyFilePath, e);
		} finally {
			try {
				if(br!=null) br.close();
			} catch(IOException ex){
				logger.error("Couldn't close file: " + mailBodyFilePath, ex);
			}
		}
		// replace keys in mail body
		mailBody = mailBody.replace(REPLACEKEY_RECIPIENTNAME, recipientName);
		mailBody = mailBody.replace(REPLACEKEY_USERNAME, username);

		try {
			// send the approved email to the user
			sendEmail(mailTo, mailSubject, mailBody);
			// send new password to the given mobile phone number
			sendNewPasswordToSMS(mobile, recipientName, password);
		} catch (MessagingException e) {
			logger.error("Could not send out an email", e);
			throw e;
		}
		logger.debug("finished sending approved email.");
	}
	
	
	/**
	 * this method should be used when the password in manually updated by the admin user (not randomly generated)
	 * 
	 * @param mailTo : email address of the receiver
	 * @param recipientName : receiver name
	 * @param username : username that receiver should use to log in into support tracker
	 * @throws Exception if the email could not be sent through
	 */
	public static void sendEmailForApprovedRequestWithManualPsw(String mailTo, String recipientName, String username) throws Exception{
		Logger logger = Logger.getRootLogger(); // initiate as a default root logger
		
		logger.debug("preparing an email for approved request with manually assigned password");
		
		String mailSubject = LdapProperty.getProperty(APPROVED_SUBJECT);
		
		String mailBodyFilePath = LdapProperty.getProperty(APPROVED_MANUALPSW_BODY_FILE_PATH);
		String mailBody = "";
		// reading mail body
		BufferedReader br = null;
		try{
			br = new BufferedReader(new FileReader(mailBodyFilePath));
			String s=null;
			while((s = br.readLine()) != null){
				mailBody += s;
			}
		} catch (IOException e) {
			logger.error("Couldn't read the template of mail body content file " + mailBodyFilePath, e);
		} finally {
			try {
				if(br!=null) br.close();
			} catch(IOException ex){
				logger.error("Couldn't close file: " + mailBodyFilePath, ex);
			}
		}
		// replace keys in mail body
		mailBody = mailBody.replace(REPLACEKEY_RECIPIENTNAME, recipientName);
		mailBody = mailBody.replace(REPLACEKEY_USERNAME, username);

		try {
			sendEmail(mailTo, mailSubject, mailBody);
		} catch (MessagingException e) {
			logger.error("Could not send out an email", e);
			throw e;
		}
		logger.debug("finished sending approved email.");
	}
	
	
	
	/**
	 * send a rejected email to let user know that their requesting account has been rejected
	 * 
	 * @param mailTo : email address of the receiver
	 * @param recipientName : receiver name
	 * @throws Exception if the email could not be sent through
	 */
	public static void sendEmailRejected(String mailTo, String recipientName) throws Exception{
		Logger logger = Logger.getRootLogger(); // initiate as a default root logger
		logger.debug("preparing rejected email.");
		
		String mailSubject = LdapProperty.getProperty(REJECTED_SUBJECT);
		
		String mailBodyFilePath = LdapProperty.getProperty(REJECTED_BODY_FILE_PATH);
		String mailBody = "";
		// reading mail body
		BufferedReader br = null;
		try{
			br = new BufferedReader(new FileReader(mailBodyFilePath));
			String s=null;
			while((s = br.readLine()) != null){
				mailBody += s;
			}
		} catch (IOException e) {
			logger.error("Couldn't read mail body content file " + mailBodyFilePath, e);
		} finally {
			try {
				if(br!=null) br.close();
			} catch(IOException ex){
				logger.error("Couldn't close file: " + mailBodyFilePath, ex);
			}
		}
		// replace keys in mail body
		mailBody = mailBody.replace(REPLACEKEY_RECIPIENTNAME, recipientName);
		
		try {
			sendEmail(mailTo, mailSubject, mailBody);
		} catch (MessagingException e) {
			logger.error("Could not send out an email", e);
			throw e;
		}
		logger.debug("finished sending rejected email.");
	}
	
	private static void init(){
		mailServerConfig.put("mail.smtp.host", LdapProperty.getProperty(EmailConstants.MAIL_HOST));
	}
	
	
	/**
	 * a helper method used to send an email of the given content (mailBody) to the given email address (mailTo)
	 * @param mailTo : email address of the receiver
	 * @param mailSubject : subject of the email that is about to be sent
	 * @param mailBody : content of the email that is about to be sent
	 * @throws MessagingException if the email could not be sent through
	 */
	public static void sendEmail(String mailTo, String mailSubject, String mailBody) throws MessagingException{
		init();
		Logger logger = Logger.getRootLogger(); // initiate as a default root logger
		
		logger.debug("Sending an email that has subject: \"" + mailSubject + "\"" + " to: " + mailTo );
		
		Session session = Session.getDefaultInstance(mailServerConfig, null);
		MimeMessage message = new MimeMessage(session);
		try {
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(mailTo));
			message.setFrom(new InternetAddress(LdapProperty.getProperty("mail.from")));
			// sending approved email
			message.setSubject(mailSubject);
			message.setContent(mailBody, "text/html");
			message.saveChanges();
			Transport.send(message);
			
		} catch (MessagingException e) {
			logger.error("Could not send out an email",e);
			throw e;
		}
		logger.debug("finished sending email.");
	}
}
