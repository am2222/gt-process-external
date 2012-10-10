package org.geotools.process.external;

import java.util.HashMap;

public class AppSpecificProcessGroup extends ProcessGroup {

	// A process group that manages intermediate files created specifically for
	// an external app, such as those in a format only targeted at that app.

	//this includes all layers exported by the processes in the group, joining
	//all the exportedLayers maps in them
	private HashMap<Object, String[]> layerFilenames = new HashMap<Object, String[]>();
	
	public void finish() {
	    for (ExternalProcess process : processes) {
	    	process.deleteExportedLayers();
	    }
	}
	
	public String[] getLayerFilenames(Object obj) {
		return layerFilenames.get(obj);
	}

	public void addLayerFilename(Object obj, String[] filenames) {
		layerFilenames.put(obj, filenames);

	}
	
	public void addProcess(ExternalProcess proc) {
		processes.add(proc);
		proc.setAppSpecificProcessGroup(this);
	}


}
