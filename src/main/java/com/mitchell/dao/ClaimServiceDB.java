package com.mitchell.dao;

import java.sql.Connection;
import javax.naming.*;
import javax.sql.*;

/**
 * This class returns the Oracle database connect object from
 * a CentOS Oracle Express Virtual Machine
 * 
 * The method and variable in this class are static to save resources
 * You only need one instance of this class running.
 * 
 * This was explained in episode 3 of the Java Rest Tutorial Series on YouTube
 * 
 * We can some significant changes to this episode 5.
 * 
 * @author 308tube
 *
 */
public class ClaimServiceDB  {

	private static DataSource claimServiceDS = null; //hold the database object
	private static Context context = null; //used to lookup the database connection in weblogic
	
	/**
	 * 
	 * @return Database object
	 * @throws Exception
	 */
	public static DataSource claimServiceDS() throws Exception {
		
		/**
		 * check to see if the database object is already defined...
		 * if it is, then return the connection, no need to look it up again.
		 */
		if (claimServiceDS != null) {
			return claimServiceDS;
		}
		
		try {
			
			/**
			 * This will run only one time to get the database object.
			 * context is used to lookup the database object
			 * claimServiceDS will hold the database object
			 */
			if (context == null) {
				context = new InitialContext();
			}
			
			Context envContext  = (Context)context.lookup("java:/comp/env");
			
			claimServiceDS = (DataSource)envContext.lookup("jdbc/claim_service");
			
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		return claimServiceDS;
		
	}
	
	/**
	 * This method will return the connection to the Oracle 308tube schema
	 * Note that the scope is protected which means only java class in the
	 * dao package can use this method.
	 * 
	 * @return Connection to 308tube Oracle database.
	 */
	protected static Connection oraclePcPartsConnection() {
		Connection conn = null;
		try {
			conn = claimServiceDS().getConnection();
			return conn;
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		return conn; 
		}
}
