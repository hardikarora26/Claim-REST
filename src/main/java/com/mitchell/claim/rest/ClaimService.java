package com.mitchell.claim.rest;

import java.io.*;
import java.sql.*;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.mitchell.dao.ClaimServiceDB;
import com.mitchell.jaxb.MitchellClaimList;
import com.mitchell.jaxb.MitchellClaimType;
import com.mitchell.jaxb.VehicleInfoType;
import com.mitchell.utils.Utils;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;


@Path("/claim")
public class ClaimService {



	/**
	 * This method is called when the user uploads the XML file and clicks Submit
	 * The method will submit the record if the claimNumber is not already saved in database 
	 * Otherwise the User will be prompted to update the claim
	 */
	@POST
	@Path("/create")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response createClaim(
			@FormDataParam("file") InputStream fileInputStream,
			@FormDataParam("file") FormDataContentDisposition contentDispositionHeader) throws FileNotFoundException, SQLException, Exception {
		String output = null;
		MitchellClaimType mct = null;
		MitchellClaimList mcl = null;

		//Store InputStream in string for multiple use
		InputStream in = fileInputStream;
		StringBuilder sb=new StringBuilder();
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String read;

		while((read=br.readLine()) != null) {		   
			sb.append(read);   
		}
		br.close();
		read = sb.toString();
		InputStream is = new ByteArrayInputStream(read.getBytes("UTF-8"));

		//Validate input XML File againt given MitchellClaim.xsl
		if(!Utils.validateAgainstXSD(is)){
			return Response.status(415).entity("Invalid XML File").build();
		}
		
		InputStream is2 = new ByteArrayInputStream(read.getBytes("UTF-8"));
		
		mcl = new MitchellClaimList();
		mcl.setMitchellClaimList(is2);
		mct = mcl.getLastMitchellClaimType();
		
		
	// Following two methods are not valid anymore as we now validating the xml against xsl	
	/*	
		if(mct.getClaimNumber()== null ){
			mcl.deleteLastMitchellClaimType();
			return Response.status(400).entity("Error: Claim without Claim Number is invalid").build();
		}

		if(mct.getVehicles() != null && mct.getVehicles().getVehicleDetails() == null ){
			mcl.deleteLastMitchellClaimType();
			return Response.status(400).entity("Error: No Vehicle Info In Claim").build();
		}
  */
		
		if(!duplicateRecordExists(mct)){
			if( insertClaimInfo(mct) && insertVehicleInfo(mct)){
				output = "Claim Created";	
			}
		}
		else{ 
			output = "Duplicate";
		}
		return Response.status(200).entity(output).build();
	}
	

	/**
	 * The method is called whenever the request to read a claim is made 
	 * 
	 * @param parameter
	 * @param value
	 * @return
	 * @throws SQLException
	 */
	@Path("/read/{parameter}")
	@GET
	@Produces(MediaType.TEXT_HTML)
	public String readClaimInfo(@PathParam("parameter") String parameter,
			@DefaultValue("") @QueryParam("value") String value) throws SQLException {
		PreparedStatement readStatment = null;
		Connection conn = null;
		String output = null;
		if(!(parameter.equalsIgnoreCase("claimNumber") || parameter.equalsIgnoreCase("claimantFirstName") || parameter.equalsIgnoreCase("claimantLastName")
				|| parameter.equalsIgnoreCase("status") || parameter.equalsIgnoreCase("causeofloss") || parameter.equalsIgnoreCase("adjusterId"))){
			return "<br>Invalid Parameter </br> <br> Please input either ClamNumber, ClaimantFirstName, ClaimantLastName, Status, CauseOfLoss, AdjusterId";
		}
		try {
			Class.forName("com.mysql.jdbc.Driver");
			conn = ClaimServiceDB.claimServiceDS().getConnection();
			readStatment = conn.prepareStatement("select * from ClaimInfo where "+parameter+" = ?");
			readStatment.setString(1, value);
			ResultSet rs = readStatment.executeQuery();
			if (!rs.isBeforeFirst() ) {    
				return "No matching claim found";
			}
			output = Utils.resultSetToJaxb(rs);
			readStatment.close();            
			return output;
		}
		catch (Exception e) {
			e.printStackTrace();
			return "Probelm Occured While Reading Data";
		}
		finally{
			if (conn != null) conn.close();
		}        
	}

	
	/**
	 * This method is called when the User clicks "Yes" to update the claim
	 * @return
	 * @throws Exception
	 */
	@Path("/update")
	@POST
	public String updateCliam() throws Exception{

		PreparedStatement updateStmt = null;
		Connection conn = null;
		String output = null;
		MitchellClaimList mcl = new MitchellClaimList();
		MitchellClaimType mct = mcl.getLastMitchellClaimType();
		try {
			Class.forName("com.mysql.jdbc.Driver");
			conn = ClaimServiceDB.claimServiceDS().getConnection();
			updateStmt = conn.prepareStatement
					("UPDATE ClaimInfo SET claimantFirstName = COALESCE(?,claimantFirstName), "
							+ "claimantLastName = COALESCE(?,claimantLastName), status = COALESCE(?,status), "
							+ "lossDate = COALESCE(?,lossDate), adjusterID = COALESCE("+mct.getAssignedAdjusterID()+",adjusterID) WHERE ClaimNumber =?");
			updateStmt.setString(5,mct.getClaimNumber());
			updateStmt.setString(1,mct.getClaimantFirstName());
			updateStmt.setString(2,mct.getClaimantLastName());
			if(mct.getStatus() == null){
				updateStmt.setString(3, null);
			}
			else{
				updateStmt.setString(3, mct.getStatus().toString());
			}
			updateStmt.setTimestamp(4, Utils.toTimeStamp(mct.getLossDate()));		
			updateStmt.executeUpdate();
			updateStmt.close();
			return "Update Succesful";

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return output;
	} 

	
	/**
	 * In order to delete a claim, values for two parameters is required and this method then delete the particular claim if it founds any
	 * @param claimNumber
	 * @param claimantFirstName
	 * @return
	 * @throws Exception
	 */

	@Path("/delete")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public String deleteClaim(@FormParam("claimNumber") String claimNumber,
			@FormParam("claimantFirstName") String claimantFirstName) throws Exception{
		Connection conn = null;
		PreparedStatement readClaim = null;
		PreparedStatement deleteClaim = null;
		String claimToDelete = "<h1>Following Claim is Deleted</h1>";

		try {
			Class.forName("com.mysql.jdbc.Driver");
			conn = ClaimServiceDB.claimServiceDS().getConnection();
			readClaim = conn.prepareStatement("select * from ClaimInfo where ClaimNumber = ? and ClaimantFirstName =?");
			readClaim.setString(1, claimNumber);
			readClaim.setString(2, claimantFirstName);
			ResultSet rs = readClaim.executeQuery();
			if (!rs.isBeforeFirst() ) {    
				return "No matching claim found";
			}
			claimToDelete += Utils.resultSetToJaxb(rs);
			readClaim.close();
			deleteClaim= conn.prepareStatement("Delete From ClaimInfo where ClaimNumber = ? and ClaimantFirstName = ?");
			deleteClaim.setString(1, claimNumber);
			deleteClaim.setString(2, claimantFirstName);
			deleteClaim.executeUpdate();
			deleteClaim.close();
			return claimToDelete;
		}
		catch (Exception e) {
			e.printStackTrace();
			return "Invalid Parameter";	

		}
		finally{
			if(conn != null)conn.close();
		}	
	}
	
	
	/**
	 * The method returns all the claims whose LossDate fall under the user inputed Date range
	 * @param start_date
	 * @param end_date
	 * @return
	 * @throws Exception
	 */
	@Path("/dateRange")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response findClaimWithinDateRange(@FormParam("start_date") String start_date,
			@FormParam("end_date") String end_date) throws Exception{

		PreparedStatement dateRange = null;
		Connection conn = null;
		String output = null;
		try {
			Class.forName("com.mysql.jdbc.Driver");
			conn = ClaimServiceDB.claimServiceDS().getConnection();
			dateRange = conn.prepareStatement
					("SELECT * FROM claimInfo WHERE lossDate BETWEEN ? AND ?");
			try{
			dateRange.setTimestamp(1,Utils.toTimeStamp(start_date));
			dateRange.setTimestamp(2,Utils.toTimeStamp(end_date));
			}
			catch(Exception InvalidDateFormat){
				InvalidDateFormat.printStackTrace();
				return Response.status(400).entity("Incorrect Date Format: valid Format is yyyy-mm-dd HH:mm:ss").build();
			}

			ResultSet rs = dateRange.executeQuery();
			if (!rs.isBeforeFirst() ) {    
				return Response.status(404).entity("No matching claim found").build();
			}
			output = Utils.resultSetToJaxb(rs);
			dateRange.close();         
			return Response.status(200).entity(output).build();
		} catch (SQLException e) {
			e.printStackTrace();
			return Response.status(404).build();
		}
	} 
	

	/**
	 * If the user click "No" to update the claim, then this method will be called to delete the MitchellClaimType object that was just created
	 * 
	 * @throws Exception
	 */
	@Path("/deleteMCTObject")
	@POST
	public void deleteMCTObject() {

		MitchellClaimList mcl = new MitchellClaimList();
		mcl.deleteLastMitchellClaimType();
	} 
	

	/**
	 * This method will be call from createClaim() method and checks that the submitted claim exists in the database or not
	 * @param mct
	 * @return
	 * @throws Exception
	 */
	private boolean duplicateRecordExists(MitchellClaimType mct) throws Exception{

		Boolean claimExists = false;
		PreparedStatement checkDuplicateRecord = null;
		Connection conn = null;
		try {
			Class.forName("com.mysql.jdbc.Driver");
			conn = ClaimServiceDB.claimServiceDS().getConnection();
			checkDuplicateRecord = conn.prepareStatement("select * from ClaimInfo where claimnumber = ?");
			checkDuplicateRecord.setString(1, mct.getClaimNumber());
			ResultSet r1=checkDuplicateRecord.executeQuery();
			if(r1.next()) {
				claimExists = true;
			}
			checkDuplicateRecord.close();
		}
		catch(SQLException e){
			e.printStackTrace();
		}
		finally{
			if (conn != null) conn.close();
		}
		return claimExists;
		
		
	}
	/**
	 * The method will be called from createClaim() to update the ClaimInfo Table in database
	 * @param mct
	 * @return
	 * @throws SQLException
	 */
	private boolean insertClaimInfo(MitchellClaimType mct) throws SQLException{

		PreparedStatement insertClaimInfo = null;
		Connection conn = null;	
		String queryClaimInfo = "INSERT INTO ClaimInfo"		
				+ "(claimNumber, claimantFirstName, claimantLastName, status, lossDate, causeOfLoss, reportedDate, lossDescription, adjusterID) VALUES"
				+ "(?,?,?,?,?,?,?,?,?)";	 
		Boolean returnValue = false;
		try {
			Class.forName("com.mysql.jdbc.Driver");
			conn = ClaimServiceDB.claimServiceDS().getConnection();

			insertClaimInfo= conn.prepareStatement(queryClaimInfo);
			insertClaimInfo.setString(1, mct.getClaimNumber());	
			insertClaimInfo.setString(2, mct.getClaimantFirstName());
			insertClaimInfo.setString(3, mct.getClaimantLastName());
			if(mct.getStatus() == null){
				insertClaimInfo.setString(4, null);
			}
			else{
				insertClaimInfo.setString(4, mct.getStatus().toString());
			}
			insertClaimInfo.setTimestamp(5, Utils.toTimeStamp(mct.getLossDate()));
			if(mct.getLossInfo().getCauseOfLoss() == null){
				insertClaimInfo.setString(6, null);
			}
			else{
				insertClaimInfo.setString(6, Utils.toLowercase(mct.getLossInfo().getCauseOfLoss().toString()));
			}
			insertClaimInfo.setTimestamp(7, Utils.toTimeStamp(mct.getLossInfo().getReportedDate()));
			insertClaimInfo.setString(8, mct.getLossInfo().getLossDescription());	
			if(mct.getAssignedAdjusterID() == null){
				insertClaimInfo.setString(9, null);
			}
			else{
				insertClaimInfo.setLong(9, mct.getAssignedAdjusterID());
			}
			insertClaimInfo.executeUpdate();
			insertClaimInfo.close();
			returnValue = true;
		}
		catch (Exception e) {
			e.printStackTrace();

		}
		finally{
			if (conn != null) conn.close();
		}
		return returnValue;
		
		
	}
	/**
	 * The method will be called from createClaim() to update the VehicleInfo Table in database
	 * @param mct
	 * @return
	 * @throws SQLException
	 */
	private boolean insertVehicleInfo(MitchellClaimType mct) throws SQLException{

		PreparedStatement insertVehicleInfo = null;
		Connection conn = null;
		String queryVehicleInfo = "INSERT INTO VehicleInfo"
				+ "(claimNumber, modelYear, modelDescription, engineDescription, exteriorColor, vin, licPlate, licPlateState, licPlateExpDate, damageDescription) VALUES"
				+ "(?,?,?,?,?,?,?,?,?,?)";
		Boolean returnValue = false;

		List<VehicleInfoType> vehicleListType = mct.getVehicles().getVehicleDetails();
		for( VehicleInfoType vit : vehicleListType){

			try {
				Class.forName("com.mysql.jdbc.Driver");
				conn = ClaimServiceDB.claimServiceDS().getConnection();

				insertVehicleInfo= conn.prepareStatement(queryVehicleInfo);

				insertVehicleInfo.setString(1, mct.getClaimNumber());
				insertVehicleInfo.setInt(2, vit.getModelYear());
				insertVehicleInfo.setString(3, vit.getModelDescription());
				insertVehicleInfo.setString(4, vit.getEngineDescription());
				insertVehicleInfo.setString(5, vit.getExteriorColor());
				insertVehicleInfo.setString(6, vit.getVin());
				insertVehicleInfo.setString(7, vit.getLicPlate());
				insertVehicleInfo.setString(8, vit.getLicPlateState());
				insertVehicleInfo.setTimestamp(9, Utils.toTimeStamp(vit.getLicPlateExpDate()));
				insertVehicleInfo.setString(10, vit.getDamageDescription());


				insertVehicleInfo.executeUpdate();
				insertVehicleInfo.close();
				returnValue = true;
			} 
			catch (SQLException e) {
				e.printStackTrace();
			}	
			catch (Exception e) {
				e.printStackTrace();
			}
			finally{
				if (conn != null) 
					conn.close();
			}
		}
		return returnValue;
	}
}