package tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import ldap.DBConstants;
import ldap.LdapProperty;

public class SupportTrackerJDBC {
	private static Properties props = LdapProperty.getConfiguration();
	private static String jdbcUrl;
	private static String jdbcUser;
	private static String jdbcPassword;
	
	private static Logger logger = Logger.getRootLogger();

	public static Map<String,String> getUserDetails(String username){
		Map<String,String> userDetails = new HashMap<String,String>();
		StringBuffer query = new StringBuffer();
		query.append("SELECT CA.contactPersonName as displayName, ");
		query.append(       "CA.contactPersonDepartment as department, ");
		query.append(       "CA.contactPersonPosition as description, ");
		query.append(       "CA.contactPersonPhone as telephoneNumber, ");
		query.append(       "CA.contactPersonFax as facsimileTelephoneNumber, ");
		query.append(       "CA.contactPersonEmail as mail, ");
		query.append(       "CA.contactPersonMobile as mobile, ");
		query.append(       "CL.companyName as company, ");
		query.append(       "CL.companyAddress as streetAddress, ");
		query.append(       "CC.countryCode as c ");
		query.append(  "FROM ClientAccount CA ");
		query.append( "INNER JOIN Client CL ON CA.clientId = CL.clientId ");
		query.append(  "LEFT OUTER JOIN Client_Country CC ON CL.clientId = CC.clientId ");
		query.append(  "LEFT OUTER JOIN Country_Code AS CCode ON CC.countryCode = CCode.code ");
		query.append( "WHERE CA.loginName LIKE '"+username+"' ");
		Connection con = getConnection();
		if(con != null){
			try {
				Statement st = con.createStatement();
				ResultSet rs = st.executeQuery(query.toString());
				ResultSetMetaData meta = rs.getMetaData();
				meta.getColumnCount();
				while(rs.next()){
					logger.info("Found user details: "+username);
					for(int i = 1; i <= meta.getColumnCount(); i++){
						userDetails.put(meta.getColumnName(i), rs.getString(i));
						logger.info(meta.getColumnName(i)+"|"+rs.getString(i));
					}
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
	
	//ADDITIONAL FUNCTION - SPT-316
	//Get list of supported organisations
	public static List<String> getOrganisations(){
		List<String> orgs = new ArrayList<String>();
		StringBuffer query = new StringBuffer();
		query.append("SELECT ORG.companyName as name ");
		query.append(  "FROM Client ORG ");
		Connection con = getConnection();
		if(con != null){
			try {
				Statement st = con.createStatement();
				ResultSet rs = st.executeQuery(query.toString());
				while(rs.next()){
					orgs.add(rs.getString(1));
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
		Collections.sort(orgs);
		return orgs;
	}
	
	//ADDITIONAL FUNCTION - SPT-311
	//Get list of Orion Health staff emails
	public static List<String> getEmails(){
		List<String> orgs = new ArrayList<String>();
		StringBuffer query = new StringBuffer();
		query.append("SELECT email ");
		query.append(  "FROM Staff ");
		Connection con = getConnection();
		if(con != null){
			try {
				Statement st = con.createStatement();
				ResultSet rs = st.executeQuery(query.toString());
				while(rs.next()){
					orgs.add(rs.getString(1).toLowerCase());
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
		Collections.sort(orgs);
		return orgs;
	}
	
	public static int addClient(Map<String, String[]> maps){
		/*for(Map.Entry<String, String[]> entry:maps.entrySet()){
			logger.info(entry.getKey() + ":" + entry.getValue()[0]);
		}*/
		Connection con = getConnection();
		StringBuffer query = new StringBuffer();
		query.append("SELECT clientId FROM Client WHERE companyName = '"+maps.get("company")[0]+"'");
		int clientId = -1;
		if(con != null){
			try {
				Statement st = con.createStatement();
				ResultSet rs = st.executeQuery(query.toString());
				while(rs.next()){
					clientId = rs.getInt(1);
				}
			} catch (SQLException e) {
				logger.error(e.toString());
				e.printStackTrace();
			}
		//Old position of }
		int status = -1;
		if( clientId != -1 ){
			query = new StringBuffer();
			query.append("INSERT INTO ClientAccount (contactPersonName, contactPersonDepartment, ");
			query.append("contactPersonPosition, contactPersonPhone, contactPersonFax, ");
			query.append("contactPersonEmail, contactPersonMobile, loginName, clientId, active) ");
			query.append("VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			try {
				PreparedStatement pst = con.prepareStatement(query.toString());
				pst.setString(1, maps.get("displayName")[0]);
				pst.setString(2, maps.get("department")[0] == null ? "" : maps.get("department")[0]);
				pst.setString(3, maps.get("description")[0] == null ? "" : maps.get("description")[0]);
				pst.setString(4, maps.get("telephoneNumber")[0] == null ? "" : maps.get("telephoneNumber")[0]);
				pst.setString(5, maps.get("facsimileTelephoneNumber")[0] == null ? "" : maps.get("facsimileTelephoneNumber")[0]);
				pst.setString(6, maps.get("mail")[0]);
				pst.setString(7, maps.get("mobile")[0] == null ? "" : maps.get("mobile")[0]);
				pst.setString(8, maps.get("sAMAccountName")[0]);
				pst.setInt(9, clientId);
				pst.setString(10, "Y");
				status = pst.executeUpdate();
			} catch (SQLException e) {
				logger.error(e.toString());
				e.printStackTrace();
			}
		}
		int clientAccountId = -1;
		if( status > 0 ){
			query = new StringBuffer();
			query.append("SELECT clientAccountId FROM ClientAccount WHERE loginName = '"+maps.get("sAMAccountName")[0]+"'");
			try {
				Statement st = con.createStatement();
				ResultSet rs = st.executeQuery(query.toString());
				while(rs.next()){
					clientAccountId = rs.getInt(1);
				}
			} catch (SQLException e) {
				logger.error(e.toString());
				e.printStackTrace();
			}
		}
		try {
			con.close();
		} catch (SQLException e) {
			logger.error(e.toString());
			e.printStackTrace();
		}
		return clientAccountId;
		//Extended } to encompass large block, to prevent null pointer exceptions on con - SPT-448
		}
		return -1;
	}
	
	public static boolean toggleUserStatus(String username, boolean enabled){
		StringBuffer query = new StringBuffer("UPDATE ClientAccount SET active = ");
		if(enabled){
			query.append("'Y'");
		}else{
			query.append("'N'");
		}
		query.append(" WHERE loginName = '"+username+"'");
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
	
	public static String[] getAvailableUsernames(String firstname, String surname){
		TreeSet<String> names = new TreeSet<String>();
		names.add((firstname+"."+surname).toLowerCase()); //joe.alan
		names.add((surname+"."+firstname).toLowerCase()); //alan.joe
		names.add((firstname+surname.charAt(0)).toLowerCase()); //joea
		names.add((surname+firstname.charAt(0)).toLowerCase()); //alanj
		StringBuffer query = new StringBuffer();
		query.append("SELECT loginName FROM ClientAccount WHERE loginName IN ('");
		query.append(names.first());
		Iterator<String> it = names.iterator();
		it.next();
		while( it.hasNext() ){
			query.append("', '" + it.next());
		}
		query.append("')");
		logger.info(query.toString());
		Connection con = getConnection();
		if(con != null){
			try {
				Statement st = con.createStatement();
				ResultSet rs = st.executeQuery(query.toString());
				while(rs.next()){
					String str = rs.getString(1);
					names.remove(str);
					logger.info("Found: "+str);
				}
			} catch (SQLException e) {
				logger.error(e.toString());
				e.printStackTrace();
			}
			//MODIFIED CODE - NEEDS TO BE INSIDE IF - SPT-448
			//Close connection (IF CONNECTION IS NOT NULL)
			try {
				con.close();
			} catch (SQLException e) {
				logger.error(e.toString());
				e.printStackTrace();
			}
			//END MODIFIED CODE
		}
		String[] strNames = new String[names.size()];
		strNames = names.toArray(strNames);
		return strNames;
	}
	
	private static Connection getConnection(){
		jdbcUrl = props.getProperty(DBConstants.ST_JDBC_URL);
		jdbcUser = props.getProperty(DBConstants.ST_JDBC_USER);
		jdbcPassword = props.getProperty(DBConstants.ST_JDBC_PASSWORD);
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