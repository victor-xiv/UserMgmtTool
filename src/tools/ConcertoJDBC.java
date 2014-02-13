package tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import ldap.DBConstants;
import ldap.LdapProperty;

public class ConcertoJDBC {
	private static Properties props = LdapProperty.getConfiguration();
	private static String jdbcUrl;
	private static String jdbcUser;
	private static String jdbcPassword;
	private static Logger logger = Logger.getRootLogger();
	
	public static Map<String,String> getUserDetails(String username){
		Map<String,String> userDetails = new HashMap<String,String>();
		StringBuffer query = new StringBuffer();
		query.append("SELECT CASE WHEN UA.name = 'ClientID' THEN 'info' ");
		query.append(       "ELSE CASE WHEN UA.name = 'Full Name.Family Name' ");
		query.append(       "THEN 'sn' ELSE 'givenName' END END, UA.cValue ");
		query.append(  "FROM UserAttribute UA ");
		query.append( "INNER JOIN cUser CU ON UA.cUser = CU.uniqueId ");
		query.append( "WHERE CU.name = '"+username+"' ");
		query.append(   "AND UA.cValue IS NOT NULL ");
		query.append(   "AND CAST(UA.cValue AS VARCHAR) <> '' ");
		query.append(   "AND UA.name IN ('Full Name.Family Name', ");
		query.append(       "'Full Name.Given Name(s)', 'ClientID') ");
		Connection con = getConnection();
		if(con != null){
			try {
				Statement st = con.createStatement();
				ResultSet rs = st.executeQuery(query.toString());
				logger.info("Found user details: "+username);
				while(rs.next()){
					userDetails.put(rs.getString(1), rs.getString(2));
					logger.info(rs.getString(1)+"|"+rs.getString(2));
				}
			} catch (SQLException e) {
				logger.error(e.toString());
				e.printStackTrace();
			} finally {
				try {
					con.close();
				} catch (SQLException e1) {
					logger.error(e1.toString());
					e1.printStackTrace();
				}
			}
		}
		return userDetails;
	}
	
	public static boolean toggleUserStatus(String username, boolean enabled){
		StringBuffer query = new StringBuffer("UPDATE cUser SET deleted = ");
		if(enabled){
			query.append("0");
		}else{
			query.append("1");
		}
		query.append(" WHERE name = '"+username+"'");
		Connection con = getConnection();
		int result = -1;
		if(con != null){
			try {
				Statement st = con.createStatement();
				result = st.executeUpdate(query.toString());
			} catch (SQLException e) {
				logger.error(e.toString());
				e.printStackTrace();
			} finally {
				try {
					con.close();
				} catch (SQLException e1) {
					logger.error(e1.toString());
					e1.printStackTrace();
				}
			}
		}
		return result > 0;
	}
	
	private static Connection getConnection(){
		jdbcUrl = props.getProperty(DBConstants.CONCERTO_JDBC_URL);
		jdbcUser = props.getProperty(DBConstants.CONCERTO_JDBC_USER);
		jdbcPassword = props.getProperty(DBConstants.CONCERTO_JDBC_PASSWORD);
		try {
			Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
			Connection con = DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword);
			return con;
		} catch (ClassNotFoundException e) {
			logger.error(e.toString());
			e.printStackTrace();
		} catch (SQLException e) {
			logger.error(e.toString());
			e.printStackTrace();
		}
		return null;
	}
}
