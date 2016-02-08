package com.mitchell.claim.rest;

import java.io.*;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
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
import javax.xml.XMLConstants;
import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBContext;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.SAXParseException;

import com.mitchell.dao.ClaimServiceDB;
import com.mitchell.jaxb.CauseOfLossCode;
import com.mitchell.jaxb.LossInfoType;
import com.mitchell.jaxb.Marshall;
import com.mitchell.jaxb.MitchellClaimList;
import com.mitchell.jaxb.MitchellClaimType;
import com.mitchell.jaxb.StatusCode;
import com.mitchell.jaxb.VehicleInfoType;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;


@Path("/claim")
public class ClaimService {
	/**
	 * Upload a File
	 * @throws SQLException 
	 * @throws FileNotFoundException 
	 * @throws Exception 
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

		//Validate input XML File
		//   	if(!validateAgainstXSD(fileInputStream)){
		//   	  return Response.status(415).entity("Invalid XML File").build();
		//   	  }
		mcl = new MitchellClaimList();
		mcl.setMitchellClaimList(fileInputStream);

		mct = mcl.getLastMitchellClaimType();

		if(mct.getClaimNumber()== null ){
			mcl.deleteLastMitchellClaimType();
			return Response.status(400).entity("Error: Claim without Claim Number is invalid").build();
		}

		if(mct.getVehicles() != null && mct.getVehicles().getVehicleDetails() == null ){
			mcl.deleteLastMitchellClaimType();
			return Response.status(400).entity("Error: No Vehicle Info In Claim").build();
		}

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


	@Path("/update")
	@POST
	public String updateCliam() throws Exception{

		PreparedStatement updateStmt = null;
		Connection conn = null;
		String output = null;
		MitchellClaimList mcl = new MitchellClaimList();
		MitchellClaimType mct = mcl.getLastMitchellClaimType();
		System.out.println(mct.getAssignedAdjusterID());
		System.out.println(mct.getClaimantLastName());
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
			updateStmt.setTimestamp(4, toTimeStamp(mct.getLossDate()));		
			updateStmt.executeUpdate();
			updateStmt.close();
			return "Update Succesful";

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return output;
	} 

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
			dateRange.setTimestamp(1,toTimeStamp(start_date));
			dateRange.setTimestamp(2,toTimeStamp(end_date));

			ResultSet rs = dateRange.executeQuery();
			if (!rs.isBeforeFirst() ) {    
				return Response.status(404).entity("No matching claim found").build();
			}
			output = resultSetToHtml(rs);
			dateRange.close();         
			return Response.status(200).entity(output).build();
		} catch (SQLException e) {
			e.printStackTrace();
			return Response.status(404).build();
		}
	} 

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
			claimToDelete += resultSetToHtml(rs);
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


	@Path("/read/{parameter}")
	@GET
	@Produces(MediaType.TEXT_HTML)
	public String readClaimInfo(@PathParam("parameter") String parameter,
			@DefaultValue("") @QueryParam("value") String value) throws SQLException {
		PreparedStatement readStatment = null;
		Connection conn = null;
		String output = null;
		if(!(parameter.equalsIgnoreCase("claimNumber") || parameter.equalsIgnoreCase("claimantFirstName") || parameter.equalsIgnoreCase("claimantLastName")
				|| parameter.equalsIgnoreCase("status"))){
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
			output = resultSetToHtml(rs);
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


	private boolean duplicateRecordExists(MitchellClaimType mct) throws Exception{

		Boolean claimExists = false;
		PreparedStatement checkDuplicateRecord = null;
		Connection conn = null;
		System.out.println(mct.getClaimNumber());
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
			insertClaimInfo.setTimestamp(5, toTimeStamp(mct.getLossDate()));
			if(mct.getLossInfo().getCauseOfLoss() == null){
				insertClaimInfo.setString(6, null);
			}
			else{
				insertClaimInfo.setString(6, toLowercase(mct.getLossInfo().getCauseOfLoss().toString()));
			}
			insertClaimInfo.setTimestamp(7, toTimeStamp(mct.getLossInfo().getReportedDate()));
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

	public boolean insertVehicleInfo(MitchellClaimType mct) throws SQLException{

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
				insertVehicleInfo.setTimestamp(9, toTimeStamp(vit.getLicPlateExpDate()));
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

	public static String resultSetToHtml(ResultSet rs) throws SQLException, DatatypeConfigurationException, TransformerException{
		String output ="";
		while(rs.next()){

			MitchellClaimType mct = new MitchellClaimType();
			LossInfoType lit = new LossInfoType();

			mct.setClaimNumber(rs.getString("claimNumber"));
			mct.setClaimantFirstName(rs.getString("claimantFirstName"));
			mct.setClaimantLastName(rs.getString("claimantLastName"));
			mct.setStatus(StatusCode.fromValue(rs.getString("status")));
			mct.setLossDate(toXMLCalender(rs.getDate("lossDate")));
			lit.setCauseOfLoss(CauseOfLossCode.fromValue(rs.getString("causeOfLoss")));
			lit.setReportedDate(toXMLCalender(rs.getDate("reportedDate")));
			lit.setLossDescription(rs.getString("lossDescription"));
			mct.setAssignedAdjusterID(rs.getLong("adjusterID"));
			mct.setLossInfo(lit);
			output += (new Marshall().jaxbObjToHTML(mct));
		}
		return output;
	}

	public static Timestamp toTimeStamp(String s) throws ParseException{
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = sdf.parse(s);	
		Timestamp timestamp = new java.sql.Timestamp(date.getTime());
		return timestamp;
	}

	public static Timestamp toTimeStamp(XMLGregorianCalendar xgc){
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String convertedTimeStamp = sdf.format(xgc.toGregorianCalendar().getTime()); 
		return java.sql.Timestamp.valueOf(convertedTimeStamp);
	}

	public static XMLGregorianCalendar toXMLCalender(Date date) throws DatatypeConfigurationException{
		GregorianCalendar gc = new GregorianCalendar();
		gc.setTime(date);
		XMLGregorianCalendar xgc = DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);
		return xgc;
	}
	public static String toLowercase(String str)
	{
		String firstLetter = str.substring(0,1).toUpperCase();
		String restLetters = str.substring(1).toLowerCase();
		return firstLetter + restLetters;
	}

	public static boolean validateAgainstXSD(InputStream xml)
	{
		try
		{   
			StreamSource xsd = new StreamSource( ClaimService.class.getClassLoader().getResourceAsStream("/MitchellClaim.xsd"));
			SchemaFactory factory =  SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			Schema schema = factory.newSchema(xsd);
			Validator validator = schema.newValidator();
			validator.validate(new StreamSource(xml));
			return true;
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			return false;
		}
	}
}