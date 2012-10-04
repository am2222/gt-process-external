package org.geotools.process.external;

public class AppSpecificProcessGroup extends ProcessGroup {

	// A process group that manages intermediate files created specifically for
	// an external
	// app, such as those in a format only targeted at that app.

	@Override
	public void finish() {
		for (ExternalProcess process : processes) {
			process.deleteExportedLayers();
		}
	}

}
