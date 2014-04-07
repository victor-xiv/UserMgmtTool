package ldap;

public class ErrorConstants {
	// general error
	public static String UNKNOWN_ERR = "An unknown error occured, please check the log for the stack trace.";	
		
	// errors related to concerto request validation and decryption
	public static final String CONCERTO_ACCESS_ONLY = "This page can only be accessed from within Concerto.";
	public static final String INCORRECT_SHAREDKEY = "The given Shared Key is either incorrect or null.";
	public static final String VALIDATION_FAILED = "It failed to decrypt the request.";
	public static final String EXPIRED_REQUEST = "This request has been expired.";
	
	// errors related to LDAP conncetion
	public static final String CONFIG_FILE_NOTFOUND = "Configuration file not found. Please contact Administrator.";
	public static final String FAIL_CONNECTING_LDAP = "Failed to connect to LDAP server.";
	public static final String LDAP_PORT_CLOSED = "The connection port on LDAP server is closed. It might caused by the incorrect configuration in ldap.properties file.";
	public static final String INCORRECT_LDAP_PORT = "The configured LDAP connection port is incorrect. i.e. connection through SSL should be on port 636.";
	public static final String LDAP_SSL_HANDSHAKE_FAIL = "LDAP connection failed because of the failure of SSL handshake.";
	
	// error related to LDAP orocesses
	public static final String NO_USERDN_SPECIFIED = "No userDN specified in the HttpRequest parameter.";
	public static final String NO_USERNAME_SPECIFIED = "No username specified in the HttpRequest parameter.";
	public static final String FAIL_QUERY_LDAP = "Failed to get data from LDAP server.";
	public static final String FAIL_UPDATE_LDAP = "Failed to update data in LDAP server.";

	
	// errors related to reading and writing files
	public static final String FAIL_MKDIR = "Failed to make directory ";
	public static final String FAIL_MK_FILE = "Failed to create a file named: ";
	public static final String FAIL_WRITE_ACCT_REQUEST = "Failed to write the requested account info into: ";
	public static final String FAIL_READING_ACCT_REQUEST = "Failed to read account requests from: ";
	
	
	// errors related to connection to database
	public static final String FAIL_CONNECTING_DB = "Failed to connect to the Database server.";
	public static final String FAIL_CONNECT_DB_CLASSNOTFOUND = "Failed to connect to the Database server due to SQLServerDriver class not found.";
	public static final String FAIL_INITIALIZATION_CONNECT_DB = "Failed to connect to the Database server due to the failure of SQLServerDriver object initialization.";
	public static final String FAIL_LINKAGE_DB = "The linkage connection to Database server is failed.";
	public static final String FAIL_CLOSING_DB_CONNECT = "Failed to close the connection with Database server.";
	public static final String FAIL_QUERYING_DB = "Failed to execute the database query statements.";
	
	// errors related to Concerto
	public static final String FAIL_UPDATE_CONCERTO = "Failed to update Concerto."; 
	
}
