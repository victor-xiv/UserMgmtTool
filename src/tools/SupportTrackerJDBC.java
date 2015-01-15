package tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ldap.DBConstants;
import ldap.ErrorConstants;
import ldap.LdapProperty;
import ldap.LdapTool;

import org.apache.log4j.Logger;

public class SupportTrackerJDBC {
	private static String jdbcUrl;
	private static String jdbcUser;
	private static String jdbcPassword;
	
	
	private static Connection con = null;
	
	/**
	 * retrieving the mobile phone number of the give username from Support Tracker DB 
	 * @param username is the value of the column loginName of ClientAccount table. it is the same as sAMAccountName of Ldap server
	 * @return the mobile phone number of the given username, if there is one. otherwise, null is returned.
	 * the return mobile phone number is the value that is stored in contactPersonMobile column of ClientAccount table. 
	 * the return value has not been modified or validate.
	 * @throws SQLException
	 */
	public static String getRawMobilePhoneOfUser(String username, String clientAccountId) throws SQLException {
		if(username==null || username.trim().isEmpty()) return null;
		
		Logger logger = Logger.getRootLogger();
		logger.debug("(SupportTrackerDB) Started querying for a mobile number of a user: " + username);
		
		String query = "SELECT contactPersonMobile " +
							"FROM ClientAccount " +
							"WHERE loginName = ? " +
							"and clientAccountId = ?";
		
		ResultSet rs = runGivenStatementWithParamsOnSupportTrackerDB(query, new String[]{username, clientAccountId});
		
		String result = null;
		while(rs!=null && rs.next()){
			result = rs.getString("contactPersonMobile");
		}
		
		// if there's no result from ClientAccount, try to query the Staff account table
		if(result == null){
			query = "SELECT mobile FROM Staff where loginName = ?";

			rs = runGivenStatementWithParamsOnSupportTrackerDB(query, new String[]{username});
			
			// if there are more than active records, => just pick the last one
			while(rs!=null && rs.next()){
				result = rs.getString("mobile");
			}
		}
		
		logger.debug("(SupportTrackerDB) mobile number of " + username + " is: " + result);
		return result;
	}
	
	
	/**
	 * retrieving the mobile phone number of the give username from Support Tracker DB.
	 *  Upon the retrieval, the number is validated. If it is an invalid number, the method will return null.
	 * @param username is the value of the column loginName of ClientAccount table. it is the same as sAMAccountName of Ldap server
	 * @return the phone number of the given username, if that phone number is validate and can be used to send SMS programatically.
	 * Otherwise, null is returned.
	 */
	public static String validateAndGetMobilePhoneOfUser(String username, String clientAccountId) {
		Logger logger = Logger.getRootLogger();
		logger.debug("Start retrieving and validating the mobile phone of user: " + username);
		
		String mobile = null;
		try{
			mobile = getRawMobilePhoneOfUser(username, clientAccountId);
			if(mobile == null || mobile.trim().isEmpty()) 
				throw new SQLException("This user: " + username + " deson't exist in Suppor Tracker DB");
		} catch (SQLException e){
			logger.error("could not get mobile phone for " + username + " from Support Tracker DB.", e);
			return null;
		}
		
		return cleanUpAndValidateMobilePhone(mobile);
	}
	
	
	/**
	 * clean up (the missused of "-" and "0") and validating the given mobile phone
	 * @param mobile : mobile phone number that need to be cleaned up and validated

The valid mobile phone should look like one of the below forms:
0225254566			NZ phone (without country code)
 64225254566		With country code, without "+" sign
+64225254566		without "0" infront of the carrier 
+640225254566		with "0" infront of the carrier
+064225254566		with "0" infornt of the country code
+0640225254566		with "0" infront of country code and infront of the carrier
+64-0225254566		"-" seperates country code and other parts 
+64-022-525-4566	"-" seperates parts
00640225254566		with "dial out" code
(64)0225254566		brackets wrapping country code
(064)0225254566		brackets wrapping country code and with "0" infornt of the country code
(+64)0225254566		brackets wrapping country code

	 * @return a string of cleanedUpAndValidatedNumber : if the given mobile phone is valid. null, otherwise 
	 */
	public static String cleanUpAndValidateMobilePhone(String mobile){
		Logger logger = Logger.getRootLogger();
		logger.debug("validating mobile number: " + mobile);
		
		if(mobile==null || mobile.trim().isEmpty()){
			logger.debug("This mobile number is invalid because it is: " + (mobile==null ? "null" : "empty string"));
			return null;
		}
		// clean and validate the retrieved mobile number
			// 1). removing (0) empty_space ( ) and -
			mobile = mobile.replaceAll("(\\(0\\))|[\\s-()]", "");
			
			// 2). after replacing those unused chars, 
			// the valid mobile number should be at least 8 digits and it should not be more than 18 digits
			if(mobile.length() < 8 || mobile.length() > 18){
				logger.error("This is an invalid mobile phone (it contains non-number chars or its length either < 8 digits or > 18 digits): " + mobile);
				return null;
			}
			
			// 3). if there is only one "+" at the beginning => keep checking
			// 		if there is a "+" that is not at the beginning the  mobile.indexOf("+", 1) will return >= 1. then it means mobile is not valid
			if(mobile.indexOf("+", 1) == -1){
				Pattern pt = Pattern.compile("[^0-9+]");
				Matcher matcher = pt.matcher(mobile);
				
				// 4). if there is at least a character that is not a number (0-9)
				if(matcher.find()){
					logger.error("This is an invalid mobile phone: " + mobile);
					return null;
				} else {
					logger.debug("This is a valid mobile phone: " + mobile);
					return mobile;
				}
			} else {
				logger.error("This is an invalid mobile phone: " + mobile);
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

		ResultSet rs = runGivenStatementWithParamsOnSupportTrackerDB(query, new String[]{});

		while (rs!=null && rs.next()) {

			String result = rs.getString("contactPersonMobile");
			if (result != null && !result.trim().isEmpty()) {
				results.add(result);
			}
		}

		return results;
	}
	
	
	/**
	 * a helper method to create a connection with Support Tracker DB and run any given query
	 * @param query SQL query in this query must contain "?" where this "?" will be replaced by the given params.
	 * if there are more than one "?" then it will be replaced by the params in the order of the element stored in the params.
	 * e.g. query="select * from clientAccount where loginName = ? and clientId = ?"
	 *      so, params must be: {"loginNameValueHere", "idNumberHere-e.g.17"}
	 * It means that the number of the "?" in the query must be exactly the same as the number of elements in the given params list.
	 * @param params is the list of the value that will be used to replace "?"
	 * @return ResultSet object which is the result of the execution of the given query
	 * @throws SQLException if DB connection failed during the process.
	 */
	public static ResultSet runGivenStatementWithParamsOnSupportTrackerDB(String query, String[] params) throws SQLException {
		Logger.getRootLogger().debug("(SupportTrackerDB) run query: " + query + " with params: " + Arrays.toString(params));
		
		
		Connection con = getConnection();
		PreparedStatement qryStm = con.prepareStatement(query);
		for (int i=0; i<params.length; i++) {
			String param = params[i];
			try{
				int num = Integer.parseInt(param);
				qryStm.setInt(i+1, num);
			} catch (NumberFormatException ne1){
				try{
					double d =  Double.parseDouble(param);
					qryStm.setDouble(i+1, d);
				} catch (NumberFormatException ne2){
					qryStm.setString(i+1, param);
				}
				
			}
		}
		ResultSet rs = qryStm.executeQuery();
		return rs;
	}
	
	/**
	 * a helper method to create a connection with Support Tracker DB and run update query
	 * @param query SQL query in this query must contain "?" where this "?" will be replaced by the given params.
	 * if there are more than one "?" then it will be replaced by the params in the order of the element stored in the params.
	 * e.g. query=""UPDATE ClientAccount SET active = 'N' WHERE loginName = ? and clientId = ?"
	 *      so, params must be: {"loginNameValueHere", "idNumberHere-e.g.17"}
	 * It means that the number of the "?" in the query must be exactly the same as the number of elements in the given params list.
	 * @param params is the list of the value that will be used to replace "?"
	 * @return the number of rows that have been updated
	 * @throws SQLException if DB connection failed during the process.
	 */
	public static int runUpdateOfGivenStatementWithParamsOnSupportTrackerDB(String updateQuery, String[] params) throws SQLException{
		Logger.getRootLogger().debug("(SupportTrackerDB) run query: " + updateQuery + " with params: " + Arrays.toString(params));
		
		Connection con = getConnection();
		PreparedStatement qryStm = con.prepareStatement(updateQuery);
		for (int i=0; i<params.length; i++) {
			String param = params[i];
			try{
				int num = Integer.parseInt(param);
				qryStm.setInt(i+1, num);
			} catch (NumberFormatException ne1){
				try{
					double d =  Double.parseDouble(param);
					qryStm.setDouble(i+1, d);
				} catch (NumberFormatException ne2){
					qryStm.setString(i+1, param);
				}
				
			}
		}
		return qryStm.executeUpdate();
	}
	
	
	public static int runUpdateOfGivenStatementWithStringParamsOnSupportTrackerDB(String updateQuery, String[] params) throws SQLException{
		Logger.getRootLogger().debug("(SupportTrackerDB) run query: " + updateQuery + " with params: " + Arrays.toString(params));
		
		Connection con = getConnection();
		PreparedStatement qryStm = con.prepareStatement(updateQuery);
		for (int i=0; i<params.length; i++) {
			String param = params[i];
			qryStm.setString(i+1, param);
		}
		return qryStm.executeUpdate();
	}
	
	
	
	/**
	 * get the detail of the given company
	 * @param companyName    
	 * @return a Map object that stored the detail of the given companyName.
	 *  if the companyName doesn't exist in ST DB, it will return an empty map. if there's an exception during the process it will return null.
	 */
	public static Map<String, String> getCompanyDetails(String companyName){
		Logger logger = Logger.getRootLogger(); // initiate as a default root logger
		
		logger.debug("(SupportTrackerDB) about to get the details of company from Support Tracker DB " + companyName);

		String query = "SELECT * FROM Client WHERE RTRIM(LTRIM(companyName))=?";		
		
		Map<String, String> companyDetails= new HashMap<>(); 
		
		try {
			// executing the query
			ResultSet rs = null;
			
			if(companyName != null && !companyName.trim().isEmpty()){
				rs = runGivenStatementWithParamsOnSupportTrackerDB(query, new String[]{companyName});
			} else {
				return companyDetails;
			}
			
			ResultSetMetaData meta = rs.getMetaData();
			meta.getColumnCount();
			while (rs != null && rs.next()) {
				for (int i = 1; i <= meta.getColumnCount(); i++) {
					String value = rs.getString(i) == null ? "" : rs.getString(i).trim();
					companyDetails.put(meta.getColumnName(i), value);
				}
			}
		} catch (SQLException e) {
			logger.error("(SupportTrackerDB) " + ErrorConstants.FAIL_QUERYING_DB, e);
			return null;
		}
		
		logger.debug("(SupportTrackerDB) finished getting the details of company: " + companyDetails + " from Support Tracker DB.");
		
		return companyDetails;
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
		logger.debug("(SupportTrackerDB) selecting resposible staff for company: " + companyName);
		

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
										+ "where rtrim(ltrim([Client].[companyName])) = ? "
										+ "COLLATE SQL_Latin1_General_CP1_CI_AS "
								+ ")"
								+ "group by [responsibleStaffId]"
								+ "order by count([responsibleStaffId]) DESC"
				+ ")"
			,
			gnLabel, fnLabel);
		
		String result = null;
		
		ResultSet rs = runGivenStatementWithParamsOnSupportTrackerDB(query, new String[]{companyName});
		if(rs!=null && rs.next()){
			result = rs.getString(gnLabel) + " " +rs.getString(fnLabel);
		}

		logger.debug("(SupportTrackerDB) finished selecting resposible staff for company: " + companyName);
		return result;
	}
	

	/**
	 * get the detail of the given user
	 * @param username (login name or sAMAccountName name for both support tracker and ldap server) that his/her detail needed to be returned
	 * @param clientAccountId: is the unique id for this user. So, the method will try to find a row that match both username and clientAccountId.
	 *  If null or empty string given to this param, the method will try to get the last row that match username.   
	 * @return a Map object that stored the detail of the given username. So, if there's no username available in the support tracker DB, then it will return an empty Map.
	 * 			if the connection failed (or the query has not been executed successfully, it will return null.
	 * @throws SQLException if the connection failed, query execution failed or closing connection failed.
	 */
	public static Map<String,String> getUserDetails(String username, String clientAccountId) throws SQLException{
		Logger logger = Logger.getRootLogger(); // initiate as a default root logger
		
		logger.debug("(SupportTrackerDB) about to get the details of user from Support Tracker DB " + username);
		
		// building query
		StringBuffer query = new StringBuffer();
		query.append("SELECT CA.clientAccountId as info, ");
		query.append(       "CA.contactPersonName as displayName, ");
		query.append(       "CA.contactPersonDepartment as department, ");
		query.append(       "CA.contactPersonPosition as description, ");
		query.append(       "CA.contactPersonPhone as telephoneNumber, ");
		query.append(       "CA.contactPersonFax as facsimileTelephoneNumber, ");
		query.append(       "CA.contactPersonEmail as mail, ");
		query.append(       "CA.contactPersonMobile as mobile, ");
		query.append(       "CA.clientId as clientId, ");
		query.append(       "CA.contactPersonPager as contactPersonPager, ");
		query.append(       "CA.active as active, ");
		query.append(       "CA.clientAccountId as clientAccountId, ");
		query.append(       "CA.loginName as sAMAccountName, ");
		query.append(       "CL.companyName as company, ");
		query.append(       "CL.companyAddress as streetAddress, ");
		query.append(       "CC.countryCode as c ");
		query.append(  "FROM ClientAccount CA ");
		query.append( "INNER JOIN Client CL ON CA.clientId = CL.clientId ");
		query.append(  "LEFT OUTER JOIN Client_Country CC ON CL.clientId = CC.clientId ");
		query.append(  "LEFT OUTER JOIN Country_Code AS CCode ON CC.countryCode = CCode.code ");
		query.append(" WHERE CA.loginName LIKE ? ");
		
		if(clientAccountId != null && !clientAccountId.trim().isEmpty()) query.append(" and CA.clientAccountId = ? ");

		
		Map<String, String> userDetails= new HashMap<>(); 
		
		try {
			// executing the query
			ResultSet rs = null;
			
			if(clientAccountId != null && !clientAccountId.trim().isEmpty()){
				rs = runGivenStatementWithParamsOnSupportTrackerDB(query.toString(), new String[]{username, clientAccountId});
			} else {
				rs = runGivenStatementWithParamsOnSupportTrackerDB(query.toString(), new String[]{username});
			}
			
			ResultSetMetaData meta = rs.getMetaData();
			meta.getColumnCount();
			while (rs != null && rs.next()) {
				logger.debug("(SupportTrackerDB) Found user details: " + username);
				// put user info (from the query results) into a Map object (usd)
				for (int i = 1; i <= meta.getColumnCount(); i++) {
					String value = rs.getString(i) == null ? "" : rs.getString(i).trim();
					userDetails.put(meta.getColumnName(i), value);
					logger.debug("(SupportTrackerDB) " + meta.getColumnName(i) + "|" + rs.getString(i));
				}
			}
		} catch (SQLException e) {
			logger.error("(SupportTrackerDB) " + ErrorConstants.FAIL_QUERYING_DB, e);
			throw new SQLException("(SupportTrackerDB) " + ErrorConstants.FAIL_QUERYING_DB);
		}
		
		logger.debug("(SupportTrackerDB) finished getting the details of user: " + username + " from Support Tracker DB.");
		
		return userDetails;
	}
	
	
	
	/**
	 * get the staff detail of the given username
	 * @param username (login name or sAMAccountName name for both support tracker and ldap server) that his/her detail needed to be returned
	 * @return a Map object that stored the detail of the given username. So, if there's no username available in the support tracker DB, then it will return an empty Map.
	 * 			if the connection failed (or the query has not been executed successfully, it will return null.
	 * @throws SQLException if the connection failed, query execution failed or closing connection failed.
	 */
	public static Map<String, String> getOrionHealthStaffDetails(String username) throws SQLException{
		if(username == null || username.trim().isEmpty()) return null;
		
		Logger logger = Logger.getRootLogger();
		logger.debug("(SupportTrackerDB) selecting the details of a staff: " + username);
		
		String query = "SELECT  " +
							" staffId, " +
							" positionCodeName as description, " +
							" familyName as sn, " +
							" givenName as givenName, " +
							" email as mail, " +
							" workPhone as telephoneNumber, " +
							" mobile, " +
							" page, " +
							" recordStatus, " +
							" userPrivilegeId, " +
							" loginName as sAMAccountName, " +
							" receiveSms " +
							" FROM Staff AS st " +
							" FULL OUTER JOIN LK_PositionCode lkpost " +
							" ON st.positionCodeId = lkpost.positionCodeId " +
							" where loginName = ? ";
		
		ResultSet rs = runGivenStatementWithParamsOnSupportTrackerDB(query, new String[]{username});
		ResultSetMetaData meta = rs.getMetaData();
		int numbColumns = meta.getColumnCount();
		
		Map<String, String> staffDetails = new HashMap<String, String>();
		while(rs!=null && rs.next()){
			for(int i=1; i<=numbColumns; i++){
				String value = rs.getString(i) == null ? "" : rs.getString(i).trim();
				staffDetails.put(meta.getColumnName(i), value);
			}
		}
		
		if(!staffDetails.isEmpty()){
			staffDetails.put("displayName", staffDetails.get("givenName") + " " + staffDetails.get("sn"));
			staffDetails.put("company", LdapTool.ORION_HEALTH_NAME);
			staffDetails.put("info", staffDetails.get("staffId"));
		}
		
		

		logger.debug("(SupportTrackerDB) finished selecting detail of staff: " + username);
		return staffDetails;
	}
	
	//ADDITIONAL FUNCTION - SPT-316
	/**
	 * Get the sorted list of organisations being supported
	 * @return sorted list of organisations being supported
	 * @throws SQLException if the connection failed, query execution failed or closing connection failed.
	 */
	public static List<String> getOrganisations() throws SQLException{
		Logger logger = Logger.getRootLogger(); // initiate as a default root logger
		
		logger.debug("(SupportTrackerDB) about to get all the organisations from Support Tracker DB ");
		
		// building query
		List<String> orgs = new ArrayList<String>();
		StringBuffer query = new StringBuffer();
		query.append("SELECT ORG.companyName as name ");
		query.append(  "FROM Client ORG ");
		
		ResultSet rs = runGivenStatementWithParamsOnSupportTrackerDB(query.toString(), new String[]{});
		while(rs!=null && rs.next()){
			orgs.add(rs.getString(1));
		}
			
		Collections.sort(orgs);
		
		logger.debug("(SupportTrackerDB) finished getting all the organisations from Support Tracker DB ");
		
		return orgs;
	}
	
	//ADDITIONAL FUNCTION - SPT-311
	/**
	 * Get sorted list of Orion Health staff emails
	 * @return a sorted list of Orion Health staff emails
	 * @throws SQLException if the connection failed, query execution failed or closing connection failed.
	 */
	public static List<String> getStaffEmails() throws SQLException{
		Logger logger = Logger.getRootLogger(); // initiate as a default root logger
		
		logger.debug("(SupportTrackerDB) about to get all the emails from Support Tracker DB ");
		
		// building query
		List<String> orgs = new ArrayList<String>();
		StringBuffer query = new StringBuffer();
		query.append("SELECT email ");
		query.append(  "FROM Staff ");
		
		ResultSet rs = runGivenStatementWithParamsOnSupportTrackerDB(query.toString(), new String[]{});
		while(rs!=null && rs.next()){
			orgs.add(rs.getString(1).toLowerCase());
		}
				
		Collections.sort(orgs);
		
		logger.debug("(SupportTrackerDB) finished getting all emails from Support Tracker DB ");
		
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
		
		logger.debug("(SupportTrackerDB) about to add client to Support Tracker DB: ");
		for(Map.Entry<String, String[]> es : maps.entrySet()){
			logger.debug(es.getKey() + " : " + es.getValue()[0]);
		}
		
		String username = maps.get("sAMAccountName")[0];
		if(isAnySupportTrackerClientAccountMatchUsername(username)
				|| isAnySupportTrackerStaffAccountMatchUsername(username)){
			throw new SQLException("There is already this username in either ClientAccount or Staff table.");
		}
		
		
		String companyName = maps.get("company")[0];
		// handle special case, which the companyName is Orion Health
		// the account being created is for the internal staff
		if(companyName.trim().equalsIgnoreCase(LdapTool.ORION_HEALTH_NAME)){
			return addStaffAccount(maps);
		}
		
		
		
		
		// creating query to select clientId that belong to the given companyName (stored in maps)
		// e.g. final query should look like this:      "SELECT clientId FROM Client WHERE companyName = 'Aamal Medical Co'"
		String qry = "SELECT clientId FROM Client WHERE rtrim(ltrim(companyName)) = ?";
		ResultSet rs = SupportTrackerJDBC.runGivenStatementWithParamsOnSupportTrackerDB(qry, new String[]{companyName});
		int clientId = -1;
		while(rs!=null && rs.next()){
			clientId = rs.getInt(1);
		}
				
		//if there is a companyName in the DB (is not -1), insert a client account with this clientId
		//the client account info contained in given maps
		int status = -1;
		if( clientId != -1 ){
			// building a query (in String)
			// e.g example of a query:   INSERT INTO ClientAccount (contactPersonName, contactPersonDepartment, contactPersonPosition, contactPersonPhone, contactPersonFax, contactPersonEmail, contactPersonMobile, loginName, clientId, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
			StringBuffer query = new StringBuffer();
			query.append("INSERT INTO ClientAccount (contactPersonName, contactPersonDepartment, ");
			query.append("contactPersonPosition, contactPersonPhone, contactPersonFax, ");
			query.append("contactPersonEmail, contactPersonMobile, loginName, clientId, active) ");
			query.append("VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			
			String[] params = new String[10];
			params[0] = maps.get("displayName")[0];
			params[1] = maps.get("department")[0] == null ? "" : maps.get("department")[0];
			params[2] = maps.get("description")[0] == null ? "" : maps.get("description")[0];
			params[3] = maps.get("telephoneNumber")[0] == null ? "" : maps.get("telephoneNumber")[0];
			params[4] = maps.get("facsimileTelephoneNumber")[0] == null ? "" : maps.get("facsimileTelephoneNumber")[0];
			params[5] =	maps.get("mail")[0];
			params[6] = maps.get("mobile")[0] == null ? "" : maps.get("mobile")[0];
			params[7] = maps.get("sAMAccountName")[0];
			params[8] = "" + clientId;
			params[9] = "Y";
			
			try{
				status = SupportTrackerJDBC.runUpdateOfGivenStatementWithStringParamsOnSupportTrackerDB(query.toString(), params);
//				status = SupportTrackerJDBC.runUpdateOfGivenStatementWithParamsOnSupportTrackerDB(query.toString(), params);
				logger.debug(String.format("(SupportTrackerDB) Added user with name: %s and clientId %d successfully",
									maps.get("displayName")[0], clientId));
			} catch (SQLException e){
				logger.error("(SupportTrackerDB) " + ErrorConstants.FAIL_CONNECTING_DB, e);
				throw new SQLException("(SupportTrackerDB) " + ErrorConstants.FAIL_CONNECTING_DB);
			}

		} else {
			// throw new exception because the caller of this method will use
			// this message (of this exception) as the result to inform to the
			// user.
			throw new SQLException("This company name " + companyName
					+ " deosn't exist in Support Tracker database.");
		}

		// this client has been added, try to get and return its clientAccountId
		int clientAccountId = -1;
		if (status > 0) {
			clientAccountId = SupportTrackerJDBC.getClientAccountId(maps.get("sAMAccountName")[0]);
		}

		logger.debug("(SupportTrackerDB) finished adding client to Support Tracker DB");

		return clientAccountId;
	}
	
	/**
	 * query the Support Tracker DB and getting the last id (if there are more than one) of the ClientAccount that match with the given username
	 * @param username is loginName (or sAMAccountName from Ldap)
	 * @return last id (if there are more than one) of the ClientAccount that match with the given username
	 * @throws SQLException
	 */
	public static int getClientAccountId(String username) throws SQLException{
		String query = "SELECT clientAccountId FROM ClientAccount WHERE loginName = ?";
		ResultSet rs = SupportTrackerJDBC.runGivenStatementWithParamsOnSupportTrackerDB(query, new String[]{username});
		int id =  -1;
		while(rs!=null && rs.next()){
			id = rs.getInt(1);
		}
		return id;
	}
	
	
	/**
	 * update the ClientAccount table with the given maps on any rows (accounts) that match to username and clientAccountId
	 * @param username
	 * @param clientAccountId
	 * @param maps must contains key/value for these keys: {"displayName", "department", "description", "telephoneNumber",
	 *  "facsimileTelephoneNumber", "mail", "mobile", "sAMAccountName"}
	 * @return true if at least one row has been updated, false if there's no any row has been updated.
	 */
	public static boolean updateClientAccount(String username, String clientAccountId, Map<String,String[]> maps){
		Logger logger = Logger.getRootLogger(); // initiate as a default root logger
		
		logger.debug("(SupportTrackerDB) about to update client to Support Tracker DB: " + username);
		
		String query = "UPDATE ClientAccount SET contactPersonName = ? , contactPersonDepartment = ? , "
						+ " contactPersonPosition = ? , contactPersonPhone = ? , contactPersonFax = ? , "
						+ " contactPersonEmail = ? , contactPersonMobile = ? WHERE loginName = ? ";		
		String[] params = new String[8];
		params[0] = maps.get("displayName")[0];
		params[1] = maps.get("department") == null ? "" : maps.get("department")[0];
		params[2] = maps.get("description") == null ? "" : maps.get("description")[0];
		params[3] = maps.get("telephoneNumber") == null ? "" : maps.get("telephoneNumber")[0];
		params[4] = maps.get("facsimileTelephoneNumber") == null ? "" : maps.get("facsimileTelephoneNumber")[0];
		params[5] =	maps.get("mail")==null ? "":maps.get("mail")[0];
		params[6] = maps.get("mobile") == null ? "" : maps.get("mobile")[0];
		params[7] = maps.get("sAMAccountName")[0];
		
		try{
			int status = runUpdateOfGivenStatementWithStringParamsOnSupportTrackerDB(query, params);
			logger.debug(String.format("(SupportTrackerDB) Updated user with name: %s successfully", username));
			return status > 0;
		} catch (SQLException e){
			logger.error("(SupportTrackerDB) "+ErrorConstants.FAIL_CONNECTING_DB, e);
			return false;
		}
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
		logger.debug("(SupportTrackerDB) about to add staff to Support Tracker DB: ");

		// we are not allowing to have the same username in the database 
		String username = maps.get("sAMAccountName")[0];
		if(isAnySupportTrackerClientAccountMatchUsername(username)
				|| isAnySupportTrackerStaffAccountMatchUsername(username)){
			throw new SQLException("There is already this username in either ClientAccount or Staff table.");
		}
		
		int status = 0;
		String query = "INSERT INTO Staff "
						+ "(positionCodeId, familyName, givenName, email, workPhone, "
						+ "mobile, page, recordStatus, userPrivilegeId, loginName, "
						+ "receiveSms) "
						+ "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

		String positionCodeId = getPositionCodeIdForGivenPositionName(maps.get("description") == null ? "null" : maps.get("description")[0]);
		
		// params for the query
		String[] params = new String[11]; 
		params[0] = positionCodeId;
		params[1] = maps.get("sn")[0];
		params[2] = maps.get("givenName")[0];
		params[3] = maps.get("mail")[0];
		params[4] = maps.get("telephoneNumber")[0];
		params[5] = maps.get("mobile")[0];
		params[6] = "";// page column
		params[7] = "Y"; // recordStatus column
		params[8] = "2"; //userPrivilegeId column
		params[9] = maps.get("sAMAccountName")[0];
		params[10] = "N";
		
		try{
			status = SupportTrackerJDBC.runUpdateOfGivenStatementWithStringParamsOnSupportTrackerDB(query, params);
//			status = SupportTrackerJDBC.runUpdateOfGivenStatementWithParamsOnSupportTrackerDB(query, params);
			logger.debug(String.format("(SupportTrackerDB) Added user with name: %s successfully", maps.get("displayName")[0]));
			
		} catch (SQLException e) {
			logger.error(ErrorConstants.FAIL_CONNECTING_DB, e);
					throw new SQLException(ErrorConstants.FAIL_CONNECTING_DB);
		}
			
		// this staff account has been added, try to get and return its
		// staffId
		int staffId = -1;
		if (status > 0) {
			staffId = SupportTrackerJDBC.getStaffId(maps.get("sAMAccountName")[0]);
		}
		
		logger.debug("(SupportTrackerDB) finished adding staff to Support Tracker DB: ");

		return staffId;
	}
	
	
	/**
	 * query the Support Tracker DB and getting the last id (if there are more than one) of the Staff account that match with the given username
	 * @param username is loginName (or sAMAccountName from Ldap)
	 * @return last id (if there are more than one) of the account that match given username
	 * @throws SQLException
	 */
	public static int getStaffId(String username) throws SQLException{
		String query = "SELECT staffId FROM Staff WHERE loginName = ?";
		ResultSet rs = SupportTrackerJDBC.runGivenStatementWithParamsOnSupportTrackerDB(query, new String[]{username});
		int id =  -1;
		while(rs!=null && rs.next()){
			id = rs.getInt(1);
		}
		return id;
	}
	
	
	/**
	 * Every Orion Staff Account (Staff table of ST DB) contains PositionCodeId which is a number.
	 * The LK_PositionCode table storing the PositionCodeName and the join PositionCodeId.
	 * This method will return the PositionCodeId that match to the PositionName of that LK_PositionCode table.
	 * If there's no match it will return code "28" as default.
	 * @param positionName that is used to match with the PositionCodeName column of LK_PositionCode that store the position id and name.
	 * @return the String object contains the code number that match the PositionCodeName. and if there's no match, it will return code "28"
	 */
	public static String getPositionCodeIdForGivenPositionName(String positionName){
		String positionCodeId = null;
		
		try{
			String query = "SELECT positionCodeId FROM LK_PositionCode WHERE positionCodeName = ?";
			ResultSet rs = runGivenStatementWithParamsOnSupportTrackerDB(query, new String[]{positionName.trim()});
			while(rs!=null && rs.next()){
				int id = rs.getInt(1);
				if(id > 0) positionCodeId = ""+id;
			}
		} catch (SQLException e){
			
		}
		
		if(positionCodeId==null || positionCodeId.trim().isEmpty()) positionCodeId = "28";
		return (positionCodeId == null || positionCodeId.trim().isEmpty()) ? "28" : positionCodeId;
	}
	
	
	/**
	 * Every Orion Staff Account (Staff table of ST DB) contains PositionCodeId which is a number.
	 * The LK_PositionCode table storing the PositionCodeName and the join PositionCodeId.
	 * This method will return a set of all the rows of PositionCodeName column in LK_PositionCodeName of ST DB.
	 * @return a set of all the rows of PositionCodeName column in LK_PositionCodeName of ST DB. if it cannot get anything
	 * from that table, it will return a default set of PositionCodeName which is {"Account Manager", "Accounts Personnel", "Administration", 
	 * "Chief Executive Officer", "Chief Operating Officer", "Clinical Consultant", 
	 * "Consultant", "Development Manager", "General Manager", "Group Financial Controller", 
	 * "Network Administrator", "Orion Health Staff", "Orion Health Support Staff", "Product Development Consultant", 
	 * "PSG Manager, Senior Technical Writer", "Service Delivery Manager", "Software Engineer", 
	 * "Support Administrators", "Support Manager", "Technical Writer", "Tester", "VP Product Management", "VP Sales"};
	 */
	public static Set<String> getAllPositionCodeNames(){
		String query = "SELECT positionCodeName FROM LK_PositionCode ORDER BY positionCodeName";
		try {
			ResultSet rs = runGivenStatementWithParamsOnSupportTrackerDB(query, new String[]{});
			TreeSet<String> posNames = new TreeSet<>();
			while(rs.next()){
				String value = rs.getString(1) == null ? "" : rs.getString(1).trim();
				posNames.add(value);
			}
			return posNames;
		} catch (SQLException e) {
			String[] allRoles = new String[]{"Account Manager", "Accounts Personnel", "Administration", 
					"Chief Executive Officer", "Chief Operating Officer", "Clinical Consultant", 
					"Consultant", "Development Manager", "General Manager", "Group Financial Controller", 
					"Network Administrator", "Orion Health Staff", "Orion Health Support Staff", "Product Development Consultant", 
					"PSG Manager, Senior Technical Writer", "Service Delivery Manager", "Software Engineer", 
					"Support Administrators", "Support Manager", "Technical Writer", "Tester", "VP Product Management", "VP Sales"};
			return new TreeSet<String>(Arrays.asList(allRoles));
		}	
	}
	
	
	/**
	 * update the Staff table with the given paramMaps on any rows (accounts) that match to username
	 * @param username
	 * @param paramMaps: must contains key/value for these keys:   {"sn", "givenName", "mail", "telephoneNumber", "mobile"}
	 * @return true if at least one row has been updated, false if there's no any row has been updated.
	 */
	public static boolean updateStaffAccount(String username, Map<String,String[]> paramMaps){
		Logger logger = Logger.getRootLogger(); // initiate as a default root logger
		logger.debug("(SupportTrackerDB) about to update staff to Support Tracker DB: " + username);

		int status = 0;
		String query = "UPDATE Staff SET familyName = ? , givenName= ? , "
				+ " email = ? , workPhone = ? , mobile = ? , positionCodeId = ? WHERE loginName = ?";

		String positionCodeId = getPositionCodeIdForGivenPositionName(paramMaps.get("description")==null ? "":paramMaps.get("description")[0]);
		// params for the query
		String[] params = new String[7]; 
		params[0] = paramMaps.get("sn")==null ? "":paramMaps.get("sn")[0];
		params[1] = paramMaps.get("givenName")==null ? "":paramMaps.get("givenName")[0];
		params[2] = paramMaps.get("mail")==null ? "":paramMaps.get("mail")[0];
		params[3] = paramMaps.get("telephoneNumber")==null ? "":paramMaps.get("telephoneNumber")[0];
		params[4] = paramMaps.get("mobile")==null ? "":paramMaps.get("mobile")[0];
		params[5] = positionCodeId;
		params[6] = username;
		
		try{
			status = SupportTrackerJDBC.runUpdateOfGivenStatementWithStringParamsOnSupportTrackerDB(query, params);
			logger.debug(String.format("(SupportTrackerDB) Update staff with name: %s successfully", username));
			
		} catch (SQLException e) {
			logger.error("(SupportTrackerDB) "+ErrorConstants.FAIL_CONNECTING_DB, e);
		}
		
		logger.debug("(SupportTrackerDB) finished adding staff to Support Tracker DB: ");

		return status > 0;
	}
	
	
	
	
	/**
	 * delete client (using clientAccountId) from Support Tracker Database
	 * @param clientAccountId that need to be deleted
	 * @return false if there's no record has been deleted or true if at least 1 record has been deleted
	 * @throws SQLException if there's any exception occur before, during and after connection and query execution
	 */
	public static boolean deleteClient(int clientAccountId) throws SQLException{
		Logger logger = Logger.getRootLogger(); // initiate as a default root logger
		
		logger.debug("(SupportTrackerDB) deleting client that has ID from support tracker DB " + clientAccountId);
		
		String query = "DELETE ClientAccount WHERE clientAccountId = ?";
		
		int st = runUpdateOfGivenStatementWithParamsOnSupportTrackerDB(query, new String[]{""+clientAccountId});
		
		if(st > 0){
			logger.debug(String.format("(SupportTrackerDB) Deleted clientAccountId: %d successfully", clientAccountId));
			return true;
		} else {
			logger.debug(String.format("(SupportTrackerDB) There's no row affected when attampted to delete clientAccountId: %d.", clientAccountId));
			return false;
		}
	}
	
	
	
	/**
	 * Toggle user status ("enable" and "disable") in the database
	 * @param username whose his/her status needed to be changed.
	 * @param enabled new status value, either "Y" for enable or "N" for disable.
	 * @return true if there are some users (status) have been changed, false otherwise
	 * @throws SQLException if the connection to DB failed or SQL query failed to be executed
	 */
	public static boolean toggleUserStatus(String username, String clientAccountId, boolean enabled) throws SQLException{
		Logger logger = Logger.getRootLogger(); // initiate as a default root logger
		
		logger.debug("(SupportTrackerDB) toggling user in support tracker DB " + username + " to: " + enabled);
		
		if(enabled){
			return enableClientAccount(username, clientAccountId) > 0;
		}else{
			return disableClientAccount(username) > 0;
		}
		
	}
	
	
	/**
	 * update the "active" column to "Y" for the row in ClientAccount table that match the given useranme and clientAccountId
	 * @param username
	 * @param clientAccountId
	 * @return the number of rows that have been updated
	 * @throws SQLException
	 */
	public static int enableClientAccount(String username, String clientAccountId) throws SQLException{
		// building SQL query
		String query = "UPDATE ClientAccount SET active = 'Y' WHERE loginName = ? and clientAccountId = ? ";
				
		int result = runUpdateOfGivenStatementWithParamsOnSupportTrackerDB(query, new String[]{username, clientAccountId});
		return result;
	}
	
	/**
	 * update the "active" column to "N" for the rows in ClientAccount table that match username
	 * @param username
	 * @return the number of rows that have been updated
	 * @throws SQLException
	 */
	public static int disableClientAccount(String username) throws SQLException {
		// building SQL query
		String query = "UPDATE ClientAccount SET active = 'N' WHERE loginName = ? ";

		int result = runUpdateOfGivenStatementWithParamsOnSupportTrackerDB(query, new String[]{username});
		return result;
	}
	
	/**
	 * update the "recordStatus" to "N" for the rows in Staff table that match username
	 * @param username
	 * @return
	 * @throws SQLException
	 */
	public static int disableStaffAccount(String username) throws SQLException {
		// building SQL query
		String query = "UPDATE Staff SET recordStatus='N' WHERE loginName = ? ";

		int result = runUpdateOfGivenStatementWithParamsOnSupportTrackerDB(query, new String[]{username});
		return result;
	}

	/**
	 * update the "recordStatus" to "Y" for the rows in Staff table that match username
	 * @param username
	 * @param staffId
	 * @return
	 * @throws SQLException
	 */
	public static int enableStaffAccount(String username) throws SQLException{
		String query = "UPDATE Staff SET recordStatus='Y' WHERE loginName = ?";

		int result = runUpdateOfGivenStatementWithParamsOnSupportTrackerDB(query, new String[]{username});

		return result;
	}
	
	
	
	
	/**
	 * @param username
	 * @param clientAccountId
	 * @return true if there is an account that match both username and clieant and its "active" column is not "Y".
	 * 			false if there is an account that match both username and clieant and its "active" column is "Y".
	 * @throws SQLException if there's no record that match both username and clientAccountId
	 */
	public static boolean isClientAccountDisabled(String username, String clientAccountId) throws SQLException{
		if(username==null || username.trim().isEmpty()) throw new SQLException("No username given.");
		
		Map<String, String> userDetails = getUserDetails(username, clientAccountId);
		if(userDetails==null || userDetails.isEmpty()){
			throw new SQLException("No record of the given user: " + username);
		}
		return !userDetails.get("active").equals("Y");
	}
	/**
	 * @param username
	 * @param clientAccountId
	 * @return true if there is an account that match both username and clieant and its "active" column is "Y".
	 * 			false if there is an account that match both username and clieant and its "active" column is not "Y".
	 * @throws SQLException if there's no record that match both username and clientAccountId
	 */
	public static boolean isClientAccountEnabled(String username, String clientAccountId) throws SQLException {
		return !isClientAccountDisabled(username, clientAccountId);
	}
	
	/**
	 * @param username
	 * @return true if there is at least a row that match username and its "recordStatus" is "Y"
	 * @throws SQLException
	 */
	public static boolean isStaffAccountEnabled(String username) throws SQLException{
		if(username==null || username.trim().isEmpty()) throw new SQLException("No username given.");
		
		String query = "SELECT * FROM Staff where loginName = ? and recordStatus = 'Y'";
		ResultSet rs = runGivenStatementWithParamsOnSupportTrackerDB(query, new String[]{username});
		return rs.next();
	}
	
	/**
	 * @param username
	 * @return return true if there is no any row that match username and its "recordsStatus" is "Y"
	 * @throws SQLException
	 */
	public static boolean isStaffAccountDisabled(String username) throws SQLException{
		return !isStaffAccountEnabled(username);
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
		
		logger.debug("(SupportTrackerDB) getting all available username for: " + firstname + ", " + surname );
		
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
		String query = "SELECT loginName FROM ClientAccount WHERE loginName IN (";
		for(int i=0; i<names.size(); i++){
			query += " ? ";
			if(i != (names.size() -1)) query += ",";
		}
		query += ")";
		
		
			try {
				// query those possible names from the database
				// remove any names that have already been in the database
				ResultSet rs = SupportTrackerJDBC.runGivenStatementWithParamsOnSupportTrackerDB(query, names.toArray(new String[names.size()]));
				while(rs!=null && rs.next()){
					String str = rs.getString(1);
					names.remove(str);
					logger.debug("(SupportTrackerDB) Found: "+str);
				}
			} catch (SQLException e) {
				logger.error("(SupportTrackerDB) "+ErrorConstants.FAIL_QUERYING_DB, e);
				throw new SQLException("(SupportTrackerDB) "+ErrorConstants.FAIL_QUERYING_DB);
			}
			//MODIFIED CODE - NEEDS TO BE INSIDE IF - SPT-448
			//Close connection (IF CONNECTION IS NOT NULL)
//			try {
//				con.close();
//			} catch (SQLException e) {
//				logger.error(ErrorConstants.FAIL_CLOSING_DB_CONNECT, e);
//			}
			//END MODIFIED CODE

		String[] strNames = new String[names.size()];
		strNames = names.toArray(strNames);
		
		logger.debug("(SupportTrackerDB) finished getting all available username for: " + firstname + ", " + surname );
		
		// return all possible names that have not been stored in the database
		return strNames;
	}
	
	
	/**
	 * connect to a database specified in ldap.properties configure file
	 * and return an Object that represent that connection
	 * @return an Object represent the connection to the database server
	 * @throws SQLException if the connection failed.
	 */
	public static Connection getConnection() throws SQLException{
		if(con == null || con.isClosed()){
			Logger logger = Logger.getRootLogger(); // initiate as a default root logger
			
			logger.debug("(SupportTrackerDB) About to connect to Support Tracker Database");
			
			jdbcUrl = LdapProperty.getProperty(DBConstants.ST_JDBC_URL);
			jdbcUser = LdapProperty.getProperty(DBConstants.ST_JDBC_USER);
			jdbcPassword = LdapProperty.getProperty(DBConstants.ST_JDBC_PASSWORD);
			try {
				Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
				con = DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword);
				
				if(con != null) logger.debug("(SupportTrackerDB) Successfully connected to Support Tracker Database");
				else logger.error("(SupportTrackerDB) Fail in connecting to Support Tracker Database. Fail to define the failture reason.");
				
				return con;
				
			} catch (ClassNotFoundException e) {
				logger.error("(SupportTrackerDB) "+ErrorConstants.FAIL_CONNECT_DB_CLASSNOTFOUND, e);
				throw new SQLException("(SupportTrackerDB) "+ErrorConstants.FAIL_CONNECT_DB_CLASSNOTFOUND);
				
			} catch (ExceptionInInitializerError e){
				logger.error("(SupportTrackerDB) "+ErrorConstants.FAIL_INITIALIZATION_CONNECT_DB, e);
				throw new SQLException("(SupportTrackerDB) "+ErrorConstants.FAIL_INITIALIZATION_CONNECT_DB);
				
			} catch (LinkageError e){
				logger.error("(SupportTrackerDB) "+ErrorConstants.FAIL_LINKAGE_DB, e);
				throw new SQLException("(SupportTrackerDB) "+ErrorConstants.FAIL_LINKAGE_DB);
				
			} catch (SQLException e) {
				logger.error("(SupportTrackerDB) "+ErrorConstants.FAIL_CONNECTING_DB, e);
				throw new SQLException("(SupportTrackerDB) "+ErrorConstants.FAIL_CONNECTING_DB);
				
			}
		}
		
		return con;
	}
	
	public static void closeConnection() throws SQLException{
		if(con!=null){
			con.close();
			con = null;
		}
	}
	
	
	
	/**
	 * check if there is at least one client account in Support Tracker match the given username
	 * 
	 * @param username need to be checked
	 * @return true if there is a client account in Support Tracker match the given username
	 * @throws SQLException
	 */
	public static boolean isAnySupportTrackerClientAccountMatchUsername(String username) throws SQLException{
		String query = "SELECT * FROM ClientAccount WHERE loginName = ?";
		ResultSet rs = SupportTrackerJDBC.runGivenStatementWithParamsOnSupportTrackerDB(query, new String[]{username});
		return rs.next();
	}
	
	/**
	 * check if there is at least one active client account in Support Tracker match the given username
	 * 
	 * @param username need to be checked
	 * @return true if there is a client account in Support Tracker match the given username
	 * @throws SQLException
	 */
	public static boolean isAnyActiveSupportTrackerClientAccountMatchUsername(String username) throws SQLException{
		String query = "SELECT * FROM ClientAccount WHERE loginName = ? and active = 'Y'";
		ResultSet rs = SupportTrackerJDBC.runGivenStatementWithParamsOnSupportTrackerDB(query, new String[]{username});
		return rs.next();
	}
	
	
	/**
	 * check if there is at least one staff account in Support Tracker match the given username
	 * 
	 * @param username need to be checked
	 * @return true if there is a staff account in Support Tracker match the given username
	 * @throws SQLException
	 */
	public static boolean isAnySupportTrackerStaffAccountMatchUsername(String username) throws SQLException{
		String query = "SELECT * FROM Staff where loginName = ?";
		ResultSet rs = SupportTrackerJDBC.runGivenStatementWithParamsOnSupportTrackerDB(query, new String[]{username});
		return rs.next();
	}
	
	/**
	 * check if there is at least one active staff account in Support Tracker match the given username
	 * 
	 * @param username need to be checked
	 * @return true if there is a staff account in Support Tracker match the given username
	 * @throws SQLException
	 */
	public static boolean isAnyActiveSupportTrackerStaffAccountMatchUsername(String username) throws SQLException{
		String query = "SELECT * FROM Staff where loginName = ?  and recordStatus = 'Y'";
		ResultSet rs = SupportTrackerJDBC.runGivenStatementWithParamsOnSupportTrackerDB(query, new String[]{username});
		return rs.next();
	}
}