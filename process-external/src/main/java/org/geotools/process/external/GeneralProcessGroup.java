package org.geotools.process.external;

import java.util.HashMap;

public class GeneralProcessGroup extends ProcessGroup{

	//A ProcessGroup to handle exporting from geotools, which is done
	//by all external factories. This should apply to all factories.
	private HashMap<Object, String> layerFilenames = new HashMap<Object, String>();
	
	public String getLayerFilename(Object obj) {
		return layerFilenames.get(obj);
	}

	public void addLayerFilename(Object obj, String filename) {
		layerFilenames.put(obj, filename);

	}
	
	public void addProcess(ExternalProcess proc) {
		processes.add(proc);
		proc.setGeneralProcessGroup(this);
	}

	public void finish() {
		for (ExternalProcess process : processes) {
			process.deleteIntermediateLayers();
		}
	}

}
