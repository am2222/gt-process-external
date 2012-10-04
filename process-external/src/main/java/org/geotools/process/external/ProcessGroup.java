package org.geotools.process.external;

import java.util.ArrayList;
import java.util.HashMap;

public class ProcessGroup {
	
	//A process group defines a group of operations to be run on a dataset, so as to
	//optimize how layers in that dataset are written to disk to be exchanged  between
	//geotools and an external app. Basically it blocks the deletion mechanism of 
	//an ExternalProcess, so layers are not deleted while the process group is not finished
	// and different processes in it can reuse the sale layer files. Then the ProcessGroup
	// will take care of doing the cleaning.

	protected ArrayList<ExternalProcess> processes;
	private HashMap<Object, String> layerFilenames = new HashMap<Object, String>();

	public ProcessGroup() {
		processes = new ArrayList<ExternalProcess>();
	}

	public void addProcess(ExternalProcess proc) {
		processes.add(proc);
		proc.setProcessGroup(this);
	}

	public void finish() {
		for (ExternalProcess process : processes) {
			process.deleteIntermediateLayers();
		}
	}

	public String getLayerFilename(Object obj) {
		return layerFilenames.get(obj);
	}

	public void addLayerFilename(Object obj, String filename) {
		layerFilenames.put(obj, filename);

	}

}
