package org.geotools.process.external;

import org.geotools.process.external.grass.GrassProcess;

public class GrassProcessGroup extends AppSpecificProcessGroup {

	String gisdbase = null;
	
	@Override
	public void addProcess(ExternalProcess proc) {
		if (proc instanceof GrassProcess){
			super.addProcess(proc);
		}		
	}
	
	public String getGisdbase() {
		return gisdbase;
	}
	
	public void setGisdbase(String gisdbase){
		this.gisdbase = gisdbase;
	}

}
