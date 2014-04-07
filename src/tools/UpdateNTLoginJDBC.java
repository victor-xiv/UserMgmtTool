package tools;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.apache.log4j.Logger;

import ldap.DBConstants;
import ldap.ErrorConstants;
import ldap.LdapProperty;

public class UpdateNTLoginJDBC {
	private static Properties props = LdapProperty.getConfiguration();
	private static String jdbcUrl;
	private static String jdbcUser;
	private static String jdbcPassword;
	
	private static Logger logger = LoggerTool.setupDefaultRootLogger();
	
	
	/**
	 * update usingNTPassword to 1 (in the PasswordStatus column) for any uniqueID that belong to given username. 
	 * 
	 * @param username
	 * @return true if the query is successful otherwise return false.
	 * @throws SQLException if there is an exception in either connecting to DB or execute the sql query.
	 */
	public static boolean enableNT(String username) throws SQLException{
		
		LoggerTool.setupRootLogger(username);
		
		StringBuffer query = new StringBuffer();
		query.append("UPDATE PasswordStatus ");
		query.append(   "SET usingNTPassword = 1 ");
		query.append( "WHERE cUser IN ");
		query.append(        "(SELECT uniqueId FROM cUser WHERE name = '"+username+"')");

		Connection con = getConnection();
		
		int updateResult = -1;
		if(con != null){
			try {
				Statement st = con.createStatement();
				updateResult = st.executeUpdate(query.toString());
				
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
		if(updateResult == 1)
			return true;
		return false;
	}
	
	
	
	/**
	 * get connection object to the Concerto Database
	 * @return the Object represent the connection if it is successfully connected to DB
	 * @throws SQLException if there is an exception before/during the connection.
	 */
	private static Connection getConnection() throws SQLException{
		// read the concerto DB url, usename and password from conf file
		jdbcUrl = props.getProperty(DBConstants.CONCERTO_JDBC_URL);
		jdbcUser = props.getProperty(DBConstants.CONCERTO_JDBC_USER);
		jdbcPassword = props.getProperty(DBConstants.CONCERTO_JDBC_PASSWORD);
		try {
			// connecting
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
