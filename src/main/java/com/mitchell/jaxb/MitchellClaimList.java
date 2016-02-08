package com.mitchell.jaxb;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXB;

public class MitchellClaimList {

	public static List<MitchellClaimType> mitchellClaim;

	public MitchellClaimList(){
		if (mitchellClaim == null) {
			mitchellClaim = new ArrayList<MitchellClaimType>();
		}	
	}

	public List<MitchellClaimType> getMitchellClaimList() {	        
		return mitchellClaim;
	}

	public MitchellClaimType getLastMitchellClaimType(){
		return  mitchellClaim.get(mitchellClaim.size() -1);
	}

	public void deleteLastMitchellClaimType(){
		mitchellClaim.remove(mitchellClaim.size() -1);
	}

	public void setMitchellClaimList(InputStream inputStream){
		MitchellClaimType mct = JAXB.unmarshal(inputStream, MitchellClaimType.class);
		mitchellClaim.add(mct);
	}
}
