package ldap;

public class ErrorConstants {
	public static String CONCERTO_ACCESS_ONLY = "This page can only be accessed from within Concerto.";
	public static String INCORRECT_SHAREDKEY = "The given Shared Key is either incorrect or null.";
	public static String VALIDATION_FAILED = "It failed to decrypt the request.";
	public static String EXPIRED_REQUEST = "This request has been expired.";
	public static String UNKNOWN_ERR = "An unknown error occured, please check the log for the stack trace.";
	public static String CONFIG_FILE_NOTFOUND = "Configuration file not found. Please contact Administrator.";
	public static String FAIL_CONNECTING_LDAP = "Fail to connect to LDAP server.";
	public static String LDAP_PORT_CLOSED = "The connection port on LDAP server is closed. It might caused by the incorrect configuration in ldap.properties file.";
	public static String INCORRECT_LDAP_PORT = "The configured LDAP connection port is incorrect. i.e. connection through SSL should be on port 636.";
	public static String LDAP_SSL_HANDSHAKE_FAIL = "LDAP connection failed because of the failure of SSL handshake.";
	public static String FAIL_MKDIR = "Failed to make directory ";
	public static String FAIL_MK_FILE = "Failed to create a file named: ";
	public static String FAIL_WRITE_ACCT_REQUEST = "Failed to write the requested account info into: ";
	public static String FAIL_READING_ACCT_REQUEST = "Failed to read account requests from: ";
	public static String FAIL_CONNECTING_DB = "Failed to connect to the Database server.";
	public static String FAIL_CLOSING_DB_CONNECT = "Failed to close the connection with Database server.";
	public static String FAIL_QUERYING_DB = "Failed to execute the database query statements.";
}
