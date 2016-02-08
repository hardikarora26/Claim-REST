package com.mitchell.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import com.mitchell.claim.rest.ClaimService;


public class Test {

	public static void main(String[] args) throws FileNotFoundException {
		//Test t = new Test();
		InputStream is = new FileInputStream(new File("C:\\Users\\hardika.CORPDESSCI\\Desktop\\Coding Challenge\\create-claim_orig.xml"));
		InputStream xsd = new FileInputStream(new File("C:\\Users\\hardika.CORPDESSCI\\Desktop\\Coding Challenge\\MitchellClaim.xsd"));
		Test.validateAgainstXSD(is);

	}
	
	public static boolean validateAgainstXSD(InputStream xml)
	{
		try
		{   
			//StreamSource xsdStream = new StreamSource( xsd);
			StreamSource xsd = new StreamSource(  ClaimService.class.getClassLoader().getResource().getResourceAsStream("/MitchellClaim.xsd"));
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
