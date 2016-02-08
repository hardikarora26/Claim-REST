# Claim-REST
Rest Web Service to perform CRUD operations for Claim

Steps To Compile the Source and Run the Application:

In order to compile the project at your system you will need following application

. Eclipse Java EE IDE
. Maven
. MySQL 5.7
. Apache Tomcat 7(Preferred)
.Java 7

. In order to setup the database. Please find the Claim_Service.sql file available in the root folder of the project.
  You may execute the query inside the file through Mysql Command Line tool by ruuning command "source path_to_sql_file"
  
. Once all the tools are setup and the project has been imported in eclipse. You can right click on project and select Run As> Maven Clean in order to get all the dependency required to complie the project

. You may have to configure the Context.xml file located in Claim-Rest>WebContent>META-INF folder. Change the username, password as per your MySQL server username and password.

. After that you can start the application from the Eclipse IDE or you can import the project in war file and copy it to apacheServer/webapps folder and execute startup.sh/startup.bat file from apacheServer/bin folder.

Functionality:
1. Once webservice is deployed on the server, you can access the webservice from the URL http://localhost:8080/Claim-REST/
2. The home page is a form from where you can access all the functionalities of the webservice(Create/Update/Read/Delete)
2. You can click Browse to select an xml file containing the claim. It will return the result condition on the Screen.

*NOTE: The sample MitchellClaim.xml does not match with provided schema MitchellClaim.xsd and uploading the file in the webservice will throw error as it does not match with the xsd
. You may Cut and Paste the <cla:Vin> element from the MitchellClaim.xml and put it below element <cla:ExteriorColor> to make the MitchellClaim.xml file match with MitchellClaim.xsd

Or you may edit the MitchellClaim.xsd to make the <xs:element name="Vin"/> to be the top and required element for <VehicleDetails> replacing element <ModelYear>
(The MitchellClaim.xsd file is located at Claim-REST/src/main/resources/ directory)
 
