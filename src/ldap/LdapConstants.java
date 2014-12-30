package ldap;

public class LdapConstants {
	public static String CONCERTO_VALIDATOR = "validator";
	public static String LDAP_CLASS = "ldap.class";
	public static String LDAP_URL = "ldap.url";
	public static String LDAP_DOMAIN = "ldap.domain";
	public static String ADMIN_DN = "admin.baseDN";
	public static String ADMIN_PWD = "admin.password";
	public static String SSL_ENABLED = "ssl.enabled";
	public static String SSL_CERT_LOC = "ssl.trustStore";
	public static String SSL_CERT_PWD = "ssl.trustStorePassword";
	public static String GROUP_DEFAULT = "group.default";
	public static String CLIENT_DN = "client.baseDN";
	public static String GROUP_OBJECT = "group.object";
	public static String GROUP_ATTR = "group.attribute";
	public static String USER_OBJECT = "user.object";
	public static String USER_ATTR = "user.attribute";
	public static String OUTPUT_FOLDER = "output.folder";
	public static String GROUP_LDAP_CLIENT = "group.ldap.client";
	public static String GROUP_LDAP_USER="group.ldap.user";
	//ADDITIONAL CONSTANTS - SPT-312
	//Code for 'Groups' entity (not 'Clients')
	public static String BASEGROUP_DN = "basegroup.baseDN";
	//Attribute type for a security group
	public static String BASEGROUP_ATTR = "basegroup.attribute";
	public static String ORION_HEALTH_ORG_NAME = "orionhealthOrganisationName";
	public static String ACCT_BROKEN_DETAILS_LINK = "acct.broken.details.link";
	public static String DEFAULT_ORION_STAFF_POSITION="default.orion.staff.position";
}
