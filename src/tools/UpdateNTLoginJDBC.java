package tools;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.apache.log4j.Logger;

import ldap.DBConstants;
import ldap.LdapProperty;

public class UpdateNTLoginJDBC {
	private static Properties props = LdapProperty.getConfiguration();
	private static String jdbcUrl;
	private static String jdbcUser;
	private static String jdbcPassword;
	private static Logger logger = Logger.getRootLogger();
	
	public static boolean enableNT(String username){
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
		if(updateResult == 1)
			return true;
		return false;
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
