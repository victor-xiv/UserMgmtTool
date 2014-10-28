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
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ldap.DBConstants;
import ldap.ErrorConstants;
import ldap.LdapProperty;

import org.apache.log4j.Logger;

public class SupportTrackerJDBC {
	private static String jdbcUrl;
	private static String jdbcUser;
	private static String jdbcPassword;
	
	
	/**
	 * retrieving the mobile phone number of the give username from Support Tracker DB 
	 * @param username is the value of the column loginName of ClientAccount table. it is the same as sAMAccountName of Ldap server
	 * @return the mobile phone number of the given username, if there is one. otherwise, null is returned.
	 * the return mobile phone number is the value that is stored in contactPersonMobile column of ClientAccount table. 
	 * the return value has not been modified or validate.
	 * @throws SQLException
	 */
	public static String getRawMobilePhoneOfUser(String username) throws SQLException {
		if(username==null || username.trim().isEmpty()) return null;
		
		Logger logger = Logger.getRootLogger();
		logger.debug("Started querying for a mobile number of a user: " + username);
		
		String query = "SELECT contactPersonMobile FROM ClientAccount WHERE loginName = '"+username+"'";
		
		ResultSet rs = runGivenQueryOnSupportTrackerDB(query);
		
		String result = null;
		if(rs!=null && rs.next()){
			result = rs.getString("contactPersonMobile");
		}
		
		logger.debug("mobile number of " + username + " is: " + result);
		return result;
	}
	
	
	/**
	 * retrieving the mobile phone number of the give username from Support Tracker DB.
	 *  Upon the retrieval, the number is validated. If it is an invalid number, the method will return null.
	 * @param username is the value of the column loginName of ClientAccount table. it is the same as sAMAccountName of Ldap server
	 * @return the phone number of the given username, if that phone number is validate and can be used to send SMS programatically.
	 * Otherwise, null is returned.
	 */
	public static String validateAndGetMobilePhoneOfUser(String username) {
		Logger logger = Logger.getRootLogger();
		logger.debug("Start retrieving and validating the mobile phone of user: " + username);
		
		String mobile = null;
		try{
			mobile = getRawMobilePhoneOfUser(username);
			if(mobile == null || mobile.trim().isEmpty()) 
				throw new SQLException("This user: " + username + " deson't exist in Suppor Tracker DB");
		} catch (SQLException e){
			logger.error("could not get mobile phone for " + username + " from Support Tracker DB.", e);
			return null;
		}
	
	// clean and validate the retrieved mobile number
		// 1). removing (0) empty_space ( ) and -
		mobile = mobile.replaceAll("(\\(0\\))|[\\s-()]", "");
		
		// 2). after replacing those unused chars, 
		// the valid mobile number should be at least 8 digits and it should not be more than 18 digits
		if(mobile.length() < 8 || mobile.length() > 18){
			logger.error("This user: "+ username + " has an invalid mobile phone (its length either < 8 digits or > 18 digits): " + mobile);
			return null;
		}
		
		// 3). if there is only one "+" at the beginning => keep checking
		// 		if there is a "+" that is not at the beginning the  mobile.indexOf("+", 1) will return >= 1. then it means mobile is not valid
		if(mobile.indexOf("+", 1) == -1){
			Pattern pt = Pattern.compile("[^0-9+]");
			Matcher matcher = pt.matcher(mobile);
			
			// 4). if there is at least a character that is not a number (0-9)
			if(matcher.find()){
				logger.error("This user: "+ username + " has an invalid mobile phone: " + mobile);
				return null;
			} else {
				logger.debug("This user: " + username + " has a valid mobile phone: " + mobile);
				return mobile;
			}
		} else {
			logger.error("This user: "+ username + " has an invalid mobile phone: " + mobile);
			return null;
		}
	}
	
	
	/**
	 * return a list of all of the mobile phone numbers stored in Support Tracker DB (ClientAccount table)
	 * @return a list of all of the mobile phone numbers. or an empty list if there's no row in the table 
	 * @throws SQLException if DB connection failed during the process.
	 */
	public static List<String> getAllMobilePhones() throws SQLException {

		List<String> results = new ArrayList<String>();
		String query = "SELECT contactPersonMobile FROM ClientAccount";

		ResultSet rs = runGivenQueryOnSupportTrackerDB(query);

		while (rs.next()) {

			String result = rs.getString("contactPersonMobile");
			if (result != null && !result.trim().isEmpty()) {
				results.add(result);
			}
		}

		return results;
	}
	
	
	/**
	 * a helper method to create a connection with Support Tracker DB and run any given query
	 * @param query SQL query
	 * @return ResultSet object which is the result of the execution of the given query
	 * @throws SQLException if DB connection failed during the process.
	 */
	public static ResultSet runGivenQueryOnSupportTrackerDB(String query) throws SQLException{
		Logger.getRootLogger().debug("started querying for: " + query);
		Connection con = null;
		con = getConnection();
		
		ResultSet rs = null;
		if(con != null){
			// execute the delete query, return false if there's no recorded deleted
			Statement st = con.createStatement();
			 rs = st.executeQuery(query);
		}
		
		Logger.getRootLogger().debug("finished querying for: " + query);
		return rs;
	}
	
	
	
	
	
	/**
	 * get the Orion Staff's name, who responsible to manage the company given
	 * @param companyName used to find the Orion Staff's name who responsible for this company
	 * @return Orion Staff's Name who responsbile to manage the given companyName
	 * @throws SQLException
	 */
	public static String getResponsibleStaff(String companyName) throws SQLException {
		if(companyName == null || companyName.trim().isEmpty()) return null;
		
		Logger logger = Logger.getRootLogger();
		logger.debug("selecting resposible staff for company: " + companyName);
		

		String gnLabel = "givenname";
		String fnLabel = "familyName";
		String query = String.format( 
				" SELECT [%s], [%s] "
						+ "FROM [SupportTracker].[dbo].[Staff]"
						+ "where [staffId] = ("
								+ "SELECT TOP 1 [responsibleStaffId]"
								+ "FROM [SupportTracker].[dbo].[ClientApplication]"
								+ "where [clientId] = ("
										+ "SELECT [clientId] "
										+ "FROM [SupportTracker].[dbo].[Client] "
										+ "where rtrim(ltrim([Client].[companyName])) = \'%s\' "
										+ "COLLATE SQL_Latin1_General_CP1_CI_AS "
								+ ")"
								+ "group by [responsibleStaffId]"
								+ "order by count([responsibleStaffId]) DESC"
				+ ")"
			,
			gnLabel, fnLabel, companyName );
		
		String result = null;
		
		ResultSet rs = runGivenQueryOnSupportTrackerDB(query);
		if(rs!=null && rs.next()){
			result = rs.getString(gnLabel) + " " +rs.getString(fnLabel);
		}

		logger.debug("finished selecting resposible staff for company: " + companyName);
		return result;
	}
	

	/**
	 * get the detail of the given user
	 * @param username that his/her detail needed to be returned
	 * @return a Map object that stored the detail of the given username
	 * @throws SQLException if the connection failed, query execution failed or closing connection failed.
	 */
	public static Map<String,String> getUserDetails(String username) throws SQLException{
		Logger logger = Logger.getRootLogger(); // initiate as a default root logger
		
		logger.debug("about to get the details of user from Support Tracker DB " + username);
		
		Map<String,String> userDetails = new HashMap<String,String>();
		// building query
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
		
		// connecting to Database server
		Connection con = getConnection();
		
		
		if(con != null){
			try {
				// executing the query
				Statement st = con.createStatement();
				ResultSet rs = st.executeQuery(query.toString());
				ResultSetMetaData meta = rs.getMetaData();
				meta.getColumnCount();
				while(rs.next()){
					logger.debug("Found user details: "+username);
					// put user info (from the query results) into a Map object (userDetails)
					for(int i = 1; i <= meta.getColumnCount(); i++){
						userDetails.put(meta.getColumnName(i), rs.getString(i));
						logger.debug(meta.getColumnName(i)+"|"+rs.getString(i));
					}
				}
			} catch (SQLException e) {
				logger.error(ErrorConstants.FAIL_QUERYING_DB, e);
				throw new SQLException(ErrorConstants.FAIL_QUERYING_DB);
			} finally {
				try {
					// closing the connection
					con.close();
				} catch (SQLException e1) {
					logger.error(ErrorConstants.FAIL_CLOSING_DB_CONNECT, e1);
				}
			}
		}
		
		logger.debug("finished getting the details of user from Support Tracker DB " + username);
		
		return userDetails;
	}
	
	//ADDITIONAL FUNCTION - SPT-316
	/**
	 * Get the sorted list of organisations being supported
	 * @return sorted list of organisations being supported
	 * @throws SQLException if the connection failed, query execution failed or closing connection failed.
	 */
	public static List<String> getOrganisations() throws SQLException{
		Logger logger = Logger.getRootLogger(); // initiate as a default root logger
		
		logger.debug("about to get all the organisations from Support Tracker DB ");
		
		// building query
		List<String> orgs = new ArrayList<String>();
		StringBuffer query = new StringBuffer();
		query.append("SELECT ORG.companyName as name ");
		query.append(  "FROM Client ORG ");
		
		// connecting to Database server
		Connection con = null;
		try {
			con = getConnection();
		} catch (SQLException e1) {
			throw e1;
			// no need to log, it has been logged inthe getConnection()
		}
		
		if(con != null){
			try {
				logger.debug("about to query support tracker db: " + query.toString());
				// executing the query
				Statement st = con.createStatement();
				ResultSet rs = st.executeQuery(query.toString());
				while(rs.next()){
					orgs.add(rs.getString(1));
				}
				logger.debug("query successfully completed");
			} catch (SQLException e) {
				logger.error(ErrorConstants.FAIL_QUERYING_DB, e);
				throw new SQLException(ErrorConstants.FAIL_QUERYING_DB);
			} finally {
				try {
					// closing connection
					con.close();
				} catch (SQLException e1) {
					logger.error(ErrorConstants.FAIL_CLOSING_DB_CONNECT, e1);
				}
			}
		}
		Collections.sort(orgs);
		
		logger.debug("finished getting all the organisations from Support Tracker DB ");
		
		return orgs;
	}
	
	//ADDITIONAL FUNCTION - SPT-311
	/**
	 * Get sorted list of Orion Health staff emails
	 * @return a sorted list of Orion Health staff emails
	 * @throws SQLException if the connection failed, query execution failed or closing connection failed.
	 */
	public static List<String> getEmails() throws SQLException{
		Logger logger = Logger.getRootLogger(); // initiate as a default root logger
		
		logger.debug("about to get all the emails from Support Tracker DB ");
		
		// building query
		List<String> orgs = new ArrayList<String>();
		StringBuffer query = new StringBuffer();
		query.append("SELECT email ");
		query.append(  "FROM Staff ");
		
		// connecting to Database server
		Connection con = null;
		try {
			con = getConnection();
		} catch (SQLException e1) {
			throw e1;
			// no need to log, it has been logged inthe getConnection()
		}
		
		if(con != null){
			try {
				//executing query
				Statement st = con.createStatement();
				ResultSet rs = st.executeQuery(query.toString());
				while(rs.next()){
					orgs.add(rs.getString(1).toLowerCase());
				}
			} catch (SQLException e) {
				logger.error(ErrorConstants.FAIL_QUERYING_DB, e);
				throw new SQLException(ErrorConstants.FAIL_QUERYING_DB);
			} finally {
				try {
					con.close();
				} catch (SQLException e1) {
					logger.error(ErrorConstants.FAIL_CLOSING_DB_CONNECT, e1);
				}
			}
		}
		Collections.sort(orgs);
		
		logger.debug("fnished getting all the organisations from Support Tracker DB ");
		
		return orgs;
	}
	
	
	/**
	 * Add client account information (the info stored in the given maps) into clientAccount table 
	 * and return the new generated clientAccountID
	 * @param maps - storing the account information
	 * @return clientAccountID that just generated when inserting client account info into clientAccount table
	 * @throws SQLException if the connection failed, query execution failed or closing connection failed.
	 */
	public static int addClient(Map<String, String[]> maps) throws SQLException{
		Logger logger = Logger.getRootLogger(); // initiate as a default root logger
		
		logger.debug("about to add client to Support Tracker DB: ");
		for(Map.Entry<String, String[]> es : maps.entrySet()){
			logger.debug(es.getKey() + " : " + es.getValue()[0]);
		}
		
		// creating query to select clientId that belong to the given companyName (stored in maps)
		StringBuffer query = new StringBuffer();
		String companyName = maps.get("company")[0];
		
		
		// handle special case, which the companyName is Orion Health
		// the account being created is for the internal staff
		if(companyName.trim().equalsIgnoreCase("Orion Health")){
			return addStaffAccount(maps);
		}
		
		
		
		
		
		// e.g. example of a final query      SELECT clientId FROM Client WHERE companyName = 'Aamal Medical Co'
		query.append("SELECT clientId FROM Client WHERE rtrim(ltrim(companyName)) = '"
				+ companyName + "'");
		int clientId = -1;
		
		// connecting to Database server
		Connection con = null;
		try {
			con = getConnection();
		} catch (SQLException e1) {
			throw e1;
			// no need to log, it has been logged in the getConnection()
		}

		if (con != null) {
			// executing the query and trying to get the clientId of the companyName to store in clientID
			try {
				Statement st = con.createStatement();
				ResultSet rs = st.executeQuery(query.toString());
				while(rs.next()){
					clientId = rs.getInt(1);
				}
			} catch (SQLException e) {
				logger.error(ErrorConstants.FAIL_CONNECTING_DB, e);
				throw new SQLException(ErrorConstants.FAIL_CONNECTING_DB);
			}
				
			//if there is a companyName in the DB (is not -1), insert a client account with this clientId
			//the client account info contained in given maps
			int status = -1;
			if( clientId != -1 ){
				// building a query (in String)
				// e.g example of a query:   INSERT INTO ClientAccount (contactPersonName, contactPersonDepartment, contactPersonPosition, contactPersonPhone, contactPersonFax, contactPersonEmail, contactPersonMobile, loginName, clientId, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
				query = new StringBuffer();
				query.append("INSERT INTO ClientAccount (contactPersonName, contactPersonDepartment, ");
				query.append("contactPersonPosition, contactPersonPhone, contactPersonFax, ");
				query.append("contactPersonEmail, contactPersonMobile, loginName, clientId, active) ");
				query.append("VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
				try {
					// building parameterized query
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
					// execute the query
					status = pst.executeUpdate();
					logger.debug(String.format("Added user with name: %s and clientId %d successfully",  maps.get("displayName")[0], clientId));
				} catch (SQLException e) {
					logger.error(ErrorConstants.FAIL_CONNECTING_DB, e);
					throw new SQLException(ErrorConstants.FAIL_CONNECTING_DB);
				}
			} else {
				// throw new exception because the caller of this method will use this message (of this exception) as the result to inform to the user.
				throw new SQLException("This company name " + companyName + " deosn't exist in Support Tracker database.");
			}
			
			// this client has been added, try to get and return its clientAccountId  
			int clientAccountId = -1;
			if( status > 0 ){
				// building a query to query for clientAccountId from ClientAccount table
				// the clientAccountID which just created with the insertion query earlier
				query = new StringBuffer();
				query.append("SELECT clientAccountId FROM ClientAccount WHERE loginName = '"+maps.get("sAMAccountName")[0]+"'");
				try {
					Statement st = con.createStatement();
					ResultSet rs = st.executeQuery(query.toString());
					// get the clientAccountID from the query result
					while(rs.next()){
						clientAccountId = rs.getInt(1);
					}
				} catch (SQLException e) {
					logger.error(ErrorConstants.FAIL_CONNECTING_DB, e);
					throw new SQLException(ErrorConstants.FAIL_CONNECTING_DB);
				}
			}
			
			// closing connection
			try {
				con.close();
			} catch (SQLException e) {
				logger.error(ErrorConstants.FAIL_CONNECTING_DB, e);
			}
			
			logger.debug("finished adding client to Support Tracker DB");
			
			return clientAccountId;
			//Extended } to encompass large block, to prevent null pointer exceptions on con - SPT-448
		}
		return -1;
	}
	
	
	/**
	 * use the given maps object to add a row into Staff table of Support Tracker DB
	 * @param maps must have * keys, and its value is an element array, where the element at index 0 is the value
	 * e.g. maps.put("givenName", new String[]{"test given name"});
	 * The maps have * keys: Surname = maps.get("sn")[0], GivenName = maps.get("givenName")[0], 
	 * Email Address = maps.get("mail")[0], Phone = maps.get("telephoneNumber")[0], 
	 * Mobile Phone = maps.get("mobile")[0], Login Name = maps.get("sAMAccountName")[0]
	 * @return the unique staffId, if the process was successful, otherwise -1 is returned.
	 * @throws SQLException
	 */
	public static int addStaffAccount(Map<String, String[]> maps) throws SQLException {
		Logger logger = Logger.getRootLogger(); // initiate as a default root logger
		
		logger.debug("about to add staff to Support Tracker DB: ");
		for(Map.Entry<String, String[]> es : maps.entrySet()){
			logger.debug(es.getKey() + " : " + es.getValue()[0]);
		}
		
		// connecting to Database server
		Connection con = null;
		try {
			con = getConnection();
		} catch (SQLException e1) {
			throw e1;
			// no need to log, it has been logged in the getConnection()
		}

		int status = 0;
		if (con != null) {
				String query = "INSERT INTO Staff "
						+ "(positionCodeId, familyName, givenName, email, workPhone, "
						+ "mobile, page, recordStatus, userPrivilegeId, loginName, "
						+ "receiveSms) "
						+ "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
				
				try {
					// building parameterized query
					PreparedStatement pst = con.prepareStatement(query);
					pst.setInt(1, 14);
					pst.setString(2, maps.get("sn")[0]);
					pst.setString(3, maps.get("givenName")[0]);
					pst.setString(4, maps.get("mail")[0]);
					pst.setString(5, maps.get("telephoneNumber")[0]);
					pst.setString(6, maps.get("mobile")[0]);
					pst.setString(7, ""); // page column
					pst.setString(8, "Y"); // recordStatus column
					pst.setInt(9, 2);   // userPrivilegeId column
					pst.setString(10, maps.get("sAMAccountName")[0]);
					pst.setString(11, "N");
										
					// execute the query
					status = pst.executeUpdate();
					
					logger.debug(String
							.format("Added user with name: %s successfully",
									maps.get("displayName")[0]));
				} catch (SQLException e) {
					logger.error(ErrorConstants.FAIL_CONNECTING_DB, e);
					throw new SQLException(ErrorConstants.FAIL_CONNECTING_DB);
				}
			
			// this staff account has been added, try to get and return its
			// staffId
			int staffId = -1;
			if (status > 0) {
				// building a query to query for staffId from
				// Staff table
				// the staffId which just created with the insertion
				// query earlier
				query = "SELECT staffId FROM Staff WHERE loginName = '" + 
						maps.get("sAMAccountName")[0] + "'"; 

				try {
					Statement st = con.createStatement();
					ResultSet rs = st.executeQuery(query.toString());
					// get the staffId from the query result
					while (rs.next()) {
						staffId = rs.getInt(1);
					}
				} catch (SQLException e) {
					logger.error(ErrorConstants.FAIL_CONNECTING_DB, e);
					throw new SQLException(ErrorConstants.FAIL_CONNECTING_DB);
				}
			}

			// closing connection
			try {
				con.close();
			} catch (SQLException e) {
				logger.error(ErrorConstants.FAIL_CONNECTING_DB, e);
			}
			
			logger.debug("finished adding staff to Support Tracker DB: ");
			
			return staffId;
		}
		
		return -1;
	}
	
	
	
	/**
	 * delete client (using clientAccountId) from Support Tracker Database
	 * @param clientAccountId that need to be deleted
	 * @return false if there's no record has been deleted or true if at least 1 record has been deleted
	 * @throws SQLException if there's any exception occur before, during and after connection and query execution
	 */
	public static boolean deleteClient(int clientAccountId) throws SQLException{
		Logger logger = Logger.getRootLogger(); // initiate as a default root logger
		
		logger.debug("deleting client that has ID from support tracker DB " + clientAccountId);
		
		String query = String.format("DELETE ClientAccount WHERE clientAccountId = %d", clientAccountId);
		
		Connection con = null;
		try {
			con = getConnection();
		} catch (SQLException e) {
			throw e;
			// don't need to log here, it has been logged in getConnection()
		}
		
		if(con != null){
			// execute the delete query, return false if there's no recorded deleted
			Statement st = con.createStatement();
			
			if(st.executeUpdate(query)!=0){
				logger.debug(String.format("Deleted clientAccountId: %d successfully", clientAccountId));
				return true;
			} else {
				logger.debug(String.format("There's no row affected when attampted to delete clientAccountId: %d.", clientAccountId));
				return false;
			}
		}
		
		logger.debug("finished deleting client that has ID from support tracker DB " + clientAccountId);
		
		return false;
	}
	
	
	
	/**
	 * Toggle user status ("enable" and "disable") in the database
	 * @param username whose his/her status needed to be changed.
	 * @param enabled new status value, either "Y" for enable or "N" for disable.
	 * @return true if there are some users (status) have been changed, false otherwise
	 * @throws SQLException if the connection to DB failed or SQL query failed to be executed
	 */
	public static boolean toggleUserStatus(String username, boolean enabled) throws SQLException{
		Logger logger = Logger.getRootLogger(); // initiate as a default root logger
		
		logger.debug("toggling user in support tracker DB " + username + " to: " + enabled);
		
		// building SQL query
		StringBuffer query = new StringBuffer("UPDATE ClientAccount SET active = ");
		if(enabled){
			query.append("'Y'");
		}else{
			query.append("'N'");
		}
		query.append(" WHERE loginName = '" + username + "'");
		
		// connecting to Database server
		Connection con = null;
		try {
			con = getConnection();
		} catch (SQLException e1) {
			throw e1;
			// no need to log, it has been logged inthe getConnection()
		}
		
		int result = -1;
		if(con != null){
			// execute the SQL
			try {
				Statement st = con.createStatement();
				result = st.executeUpdate(query.toString());
			} catch (SQLException e) {
				logger.error(ErrorConstants.FAIL_QUERYING_DB, e);
				throw new SQLException(ErrorConstants.FAIL_QUERYING_DB);
			} finally {
				// closing connection
				try {
					con.close();
				} catch (SQLException e) {
					logger.error(ErrorConstants.FAIL_CLOSING_DB_CONNECT, e);
				}
			}
		}
		
		logger.debug("finished toggling user in support tracker DB " + username + " to: " + enabled);
		
		return result > 0;
	}
	
	/**
	 * Use the given firstname and surname to produce a set of possible and available names
	 * @param firstname - given first name to produce the possible names
	 * @param surname - give surname to produce the possible names
	 * @return an array of String of all possible and available names
	 * @throws SQLException if it failed to connect to DB server, failed to execute the queries or failed to close the connectioin 
	 */
	public static String[] getAvailableUsernames(String firstname, String surname) throws SQLException{
		Logger logger = Logger.getRootLogger(); // initiate as a default root logger
		
		logger.debug("getting all available username for: " + firstname + ", " + surname );
		
		// remove any special characters that are not allowed to be in user name.
		// those characters are: " ' , < > : = * [ ] | : ! # + & % { } ? \
		firstname = firstname.replaceAll("[\\\"\\\'\\,\\<\\>\\;\\=\\*\\[\\]\\|\\:\\~\\#\\+\\&\\%\\{\\}\\?\\\\]", "");
		surname = surname.replaceAll("[\\,\\<\\>\\;\\=\\*\\[\\]\\|\\:\\~\\#\\+\\&\\%\\{\\}\\?]", "");
		// create all possible names and stored them into a TreeMap
		TreeSet<String> names = new TreeSet<String>();
		names.add((firstname+"."+surname).toLowerCase()); //joe.alan
		names.add((surname+"."+firstname).toLowerCase()); //alan.joe
		names.add((firstname+surname.charAt(0)).toLowerCase()); //joea
		names.add((surname+firstname.charAt(0)).toLowerCase()); //alanj
		
		// create a query that contains all those 4 possible names
		StringBuffer query = new StringBuffer();
		query.append("SELECT loginName FROM ClientAccount WHERE loginName IN ('");
		query.append(names.first());
		Iterator<String> it = names.iterator();
		it.next();
		while( it.hasNext() ){
			query.append("', '" + it.next());
		}
		query.append("')");
		logger.debug(query.toString());
		
		// connecting to Database server
		Connection con= null;
		try {
			con = getConnection();
		} catch (SQLException e1) {
			throw e1;
			//no need to log, it has been logged inthe getConnection()
		}
		
		if(con != null){
			try {
				// query those possible names from the database
				// remove any names that have already been in the database
				Statement st = con.createStatement();
				ResultSet rs = st.executeQuery(query.toString());
				while(rs.next()){
					String str = rs.getString(1);
					names.remove(str);
					logger.debug("Found: "+str);
				}
			} catch (SQLException e) {
				logger.error(ErrorConstants.FAIL_QUERYING_DB, e);
				throw new SQLException(ErrorConstants.FAIL_QUERYING_DB);
			}
			//MODIFIED CODE - NEEDS TO BE INSIDE IF - SPT-448
			//Close connection (IF CONNECTION IS NOT NULL)
			try {
				con.close();
			} catch (SQLException e) {
				logger.error(ErrorConstants.FAIL_CLOSING_DB_CONNECT, e);
			}
			//END MODIFIED CODE
		}
		String[] strNames = new String[names.size()];
		strNames = names.toArray(strNames);
		
		logger.debug("finished getting all available username for: " + firstname + ", " + surname );
		
		// return all possible names that have not been stored in the database
		return strNames;
	}
	
	
	/**
	 * connect to a database specified in ldap.properties configure file
	 * and return an Object that represent that connection
	 * @return an Object represent the connection to the database server
	 * @throws SQLException if the connection failed.
	 */
	private static Connection getConnection() throws SQLException{
		Logger logger = Logger.getRootLogger(); // initiate as a default root logger
		
		logger.debug("About to connect to Support Tracker Database");
		
		jdbcUrl = LdapProperty.getProperty(DBConstants.ST_JDBC_URL);
		jdbcUser = LdapProperty.getProperty(DBConstants.ST_JDBC_USER);
		jdbcPassword = LdapProperty.getProperty(DBConstants.ST_JDBC_PASSWORD);
		try {
			Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
			Connection con = DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword);
			
			if(con != null) logger.debug("Successfully connected to Support Tracker Database");
			else logger.error("Fail in connecting to Support Tracker Database. Fail to define the failture reason.");
			
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