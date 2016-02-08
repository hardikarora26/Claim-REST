create database if not exists claim;
use claim;

drop table if exists ClaimInfo;
create table ClaimInfo (
 claimNumber     VARCHAR(250) NOT NULL,
 claimantFirstName  VARCHAR(50),
 claimantLastName   VARCHAR(50),
 status  VARCHAR(6),
 lossDate DATETIME,
 causeOfLoss VARCHAR(50),
 ReportedDate DATETIME,
 lossDescription TEXT, 
 adjusterID BIGINT,
  PRIMARY KEY (claimNumber)
  );
  
drop table if exists VehicleInfo;  
create table VehicleInfo (
 claimNumber     VARCHAR(250) NOT NULL,
 modelYear  INT,
 modelDescription   TEXT,
 engineDescription  VARCHAR(250),
 exteriorColor VARCHAR(250),
 vin VARCHAR(20),
 licPlate VARCHAR(250),
 licPlateState VARCHAR(250),
 licPlateExpDate DATE,
 damageDescription TEXT,
 FOREIGN KEY (claimNumber) REFERENCES ClaimInfo(claimNumber) ON DELETE CASCADE
  );
