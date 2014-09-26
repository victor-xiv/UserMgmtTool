package tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import ldap.DBConstants;
import ldap.ErrorConstants;
import ldap.LdapProperty;

import org.apache.log4j.Logger;

public class ConcertoJDBC {
	private static String jdbcUrl;
	private static String jdbcUser;
	private static String jdbcPassword;
	
	
	
	/**
	 * select the user detail based on the given username.
	 * @param username
	 * @return a Map object that stores the user's detail (key is columnname, value is the column value of that row).
	 * @throws SQLException if there is an exception in either connecting to DB or execute the sql query.
	 */
	public static Map<String,String> getUserDetails(String username) throws SQLException{
		Logger logger = Logger.getRootLogger(); // initiate as a default root logger
		
		Map<String,String> userDetails = new HashMap<String,String>();
		
		// building query statement
		StringBuffer query = new StringBuffer();
		query.append("SELECT CASE WHEN UA.name = 'ClientID' THEN 'info' ");
		query.append(       "ELSE CASE WHEN UA.name = 'Full Name.Family Name' ");
		query.append(       	       "THEN 'sn' "
							    + "ELSE 'givenName' "
						        + "END "
						  + "END, UA.cValue ");
		query.append(  "FROM UserAttribute UA ");
		query.append( "INNER JOIN cUser CU ON UA.cUser = CU.uniqueId ");
		query.append( "WHERE CU.name = '"+username+"' ");
		query.append(   "AND UA.cValue IS NOT NULL ");
		query.append(   "AND CAST(UA.cValue AS VARCHAR) <> '' ");
		query.append(   "AND UA.name IN ('Full Name.Family Name', ");
		query.append(       "'Full Name.Given Name(s)', 'ClientID') ");
		
		// connecting
		Connection con = getConnection();
		
		// executing the query
		if(con != null){
			try {
				Statement st = con.createStatement();
				ResultSet rs = st.executeQuery(query.toString());
				logger.info("Found user details: "+username);
				
				// put the key,value from the results into Map Object
				while(rs.next()){
					userDetails.put(rs.getString(1), rs.getString(2));
					logger.info(rs.getString(1)+"|"+rs.getString(2));
				}
				
			} catch (SQLException e) {
				// because we rethrow the exception. So, we need to close the connection first.
				try {
					con.close();
				} catch (SQLException e1) {
					logger.error(ErrorConstants.FAIL_CLOSING_DB_CONNECT, e1);
					// we are not re-throwing this exception, because we're just trying to close the connection.
				}
				
				// catch this exception and re-throw it, because we need a clean exception message
				// to present on the web browser
				logger.error(ErrorConstants.FAIL_QUERYING_DB, e);
				throw new SQLException(ErrorConstants.FAIL_QUERYING_DB);
				
			}
			
			// if there's no any exception in executing the query, then close the connection
			try {
				con.close();
			} catch (SQLException e1) {
				logger.error(ErrorConstants.FAIL_CLOSING_DB_CONNECT, e1);
				// we are not re-throwing this exception, because we're just trying to close the connection.
			}
		}
		return userDetails;
	}
	
	
	
	/**
	 * set [dbo].[cUser].[deleted] to either 0 (if given enabled==true) or 1 (enabled==false)
	 * for any rows that contains name == the given username
	 * @param username that want to apply the query on
	 * @param enabled define the value of deleted. deleted is 0 if enabled==true, otherwise deleted is 1
	 * @return true if the query was successfully executed, false otherwise
	 * @throws SQLException if there is an exception before/during the connection, or exception during the query execution.
	 */
	public static boolean toggleUserStatus(String username, boolean enabled) throws SQLException{
		Logger logger = Logger.getRootLogger(); // initiate as a default root logger
		
		// building query statement
		StringBuffer query = new StringBuffer("UPDATE cUser SET deleted = ");
		if(enabled){
			query.append("0");
		}else{
			query.append("1");
		}
		query.append(" WHERE name = '"+username+"'");
		
		// connecting
		Connection con = getConnection();
		
		// execute the query
		int result = -1;
		if(con != null){
			try {
				Statement st = con.createStatement();
				result = st.executeUpdate(query.toString());
				
			} catch (SQLException e) {
				// because we rethrow the exception. So, we need to close the connection first.
				try {
					con.close();
				} catch (SQLException e1) {
					logger.error(ErrorConstants.FAIL_CLOSING_DB_CONNECT, e1);
					// we are not re-throwing this exception, because we're just trying to close the connection.
				}
				
				// catch this exception and re-throw it, because we need a clean exception message
				// to present on the web browser
				logger.error(ErrorConstants.FAIL_QUERYING_DB, e);
				throw new SQLException(ErrorConstants.FAIL_QUERYING_DB);
				
			}
			
			// if there's no any exception in executing the query, then close the connection
			try {
				con.close();
			} catch (SQLException e1) {
				logger.error(ErrorConstants.FAIL_CLOSING_DB_CONNECT, e1);
				// we are not re-throwing this exception, because we're just trying to close the connection.
			}
		}
		return result > 0;
	}
	
	
	
	/**
	 * get connection object to the Concerto Database
	 * @return the Object represent the connection if it is successfully connected to DB
	 * @throws SQLException if there is an exception before/during the connection.
	 */
	private static Connection getConnection() throws SQLException{
		Logger logger = Logger.getRootLogger(); // initiate as a default root logger
		
		// read the concerto DB url, usename and password from conf file
		jdbcUrl = LdapProperty.getProperty(DBConstants.CONCERTO_JDBC_URL);
		jdbcUser = LdapProperty.getProperty(DBConstants.CONCERTO_JDBC_USER);
		jdbcPassword = LdapProperty.getProperty(DBConstants.CONCERTO_JDBC_PASSWORD);
		try {
			// connecting to the database
			Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
			Connection con = DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword);
			return con;
			
		} catch (ClassNotFoundException e) {
			logger.error(ErrorConstants.FAIL_CONNECT_DB_CLASSNOTFOUND, e);
			throw new SQLException(ErrorConstants.FAIL_CONNECT_DB_CLASSNOTFOUND);
			
		} catch (ExceptionInInitializerError e){
			logger.error(ErrorConstants.FAIL_INITIALIZATION_CONNECT_DB, e);
			throw new SQLException(ErrorConstants.FAIL_INITIALIZATION_CONNECT_DB);
			
		} catch (LinkageError e){
			logger.error(ErrorConstants.FAIL_LINKAGE_DB, e);
			throw new SQLException(ErrorConstants.FAIL_LINKAGE_DB);
			
		} catch (SQLException e) {
			logger.error(ErrorConstants.FAIL_CONNECTING_DB, e);
			throw new SQLException(ErrorConstants.FAIL_CONNECTING_DB);
			
		}
	}
}
