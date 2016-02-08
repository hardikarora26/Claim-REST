package com.mitchell.utils;

import java.io.InputStream;
import java.io.StringWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.util.JAXBSource;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import com.mitchell.claim.rest.ClaimService;
import com.mitchell.jaxb.CauseOfLossCode;
import com.mitchell.jaxb.LossInfoType;
import com.mitchell.jaxb.MitchellClaimType;
import com.mitchell.jaxb.StatusCode;


public class Utils {

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
	public static String resultSetToJaxb(ResultSet rs) throws SQLException, DatatypeConfigurationException, TransformerException{
		String output ="";
		while(rs.next()){

			MitchellClaimType mct = new MitchellClaimType();
			LossInfoType lit = new LossInfoType();

			mct.setClaimNumber(rs.getString("claimNumber"));
			mct.setClaimantFirstName(rs.getString("claimantFirstName"));
			mct.setClaimantLastName(rs.getString("claimantLastName"));
			mct.setStatus(StatusCode.fromValue(rs.getString("status")));
			mct.setLossDate(Utils.toXMLCalender(rs.getDate("lossDate")));
			lit.setCauseOfLoss(CauseOfLossCode.fromValue(rs.getString("causeOfLoss")));
			lit.setReportedDate(Utils.toXMLCalender(rs.getDate("reportedDate")));
			lit.setLossDescription(rs.getString("lossDescription"));
			mct.setAssignedAdjusterID(rs.getLong("adjusterID"));
			mct.setLossInfo(lit);
			output += (jaxbObjToHTML(mct));
		}
		return output;
	}

	public static String jaxbObjToHTML(MitchellClaimType mct) throws TransformerException{

		String strResult= null;
		try {

			TransformerFactory tf = TransformerFactory.newInstance();
			StreamSource xslt = new StreamSource( Utils.class.getClassLoader().getResourceAsStream("stylesheet.xsl"));
			Transformer transformer = tf.newTransformer(xslt);

			// Source
			JAXBContext jaxc = JAXBContext.newInstance(MitchellClaimType.class);
			JAXBSource source = new JAXBSource(jaxc, new JAXBElement<MitchellClaimType>(new QName("http://www.mitchell.com/examples/claim","MitchellClaim"), MitchellClaimType.class, mct));

			// Result
			StringWriter writer = new StringWriter();
			StreamResult result = new StreamResult(writer);

			// Transform
			transformer.transform(source, result);  
			strResult = writer.toString();
			return strResult;
		} catch (JAXBException e) {
			e.printStackTrace();
		}
		return "Error Parsing To HTML";
	}


}
