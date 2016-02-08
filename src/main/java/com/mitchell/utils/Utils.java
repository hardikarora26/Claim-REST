package com.mitchell.utils;

import java.io.InputStream;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.XMLConstants;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

import com.mitchell.claim.rest.ClaimService;
import com.sun.org.apache.xerces.internal.impl.dtd.models.CMStateSet;

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
	
	
}
