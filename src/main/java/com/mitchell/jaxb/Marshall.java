package com.mitchell.jaxb;

import java.io.File;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.util.JAXBSource;
import javax.xml.namespace.QName;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;


public class Marshall {
	
	
	public String jaxbObjToHTML(MitchellClaimType mct) throws TransformerException{
		
		String strResult= null;
	try {
//		 String property = "java.io.tmpdir";
//		    String tempDir = System.getProperty(property);
//		    System.out.println("OS current temporary directory is " + tempDir);
//		    File file = new File(tempDir,"marshall.xml");
//		    StringWriter writer1 = new StringWriter();
		  //  StreamResult result = null;
//	JAXBContext jc = JAXBContext.newInstance(MitchellClaimType.class);
//	Marshaller marshaller = jc.createMarshaller();
//	marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
//	marshaller.marshal(new JAXBElement<MitchellClaimType>(new QName("http://www.mitchell.com/examples/claim","MitchellClaim"), MitchellClaimType.class, mct), writer1);
  
	
	 TransformerFactory tf = TransformerFactory.newInstance();
	 StreamSource xslt = new StreamSource( getClass().getClassLoader().getResourceAsStream("/stylesheet.xsl"));
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
    //   System.out.println("From marshall::::: "+strResult);
       return strResult;
	      } catch (JAXBException e) {
		e.printStackTrace();
	      }
       return "Error Parsing To HTML";
	}
}
