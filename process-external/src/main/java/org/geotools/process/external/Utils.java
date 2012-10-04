package org.geotools.process.external;

import java.io.File;


public class Utils {

	protected static int nTempFilenames = 0;

	public static boolean isWindows() {
		final String os = System.getProperty("os.name").toLowerCase();
		return (os.indexOf("win") >= 0);
	}

	public static String getTempFilename(String prefix, String ext) {
		String filename = prefix + Long.toString(System.currentTimeMillis())
				+ "_" + Integer.toString(nTempFilenames) + "." + ext;
		nTempFilenames++;
		return getRootExchangeFolder() + File.separator + filename;
	}

	private static String getRootExchangeFolder() {
		File baseDir = new File(System.getProperty("java.io.tmpdir"),
				"gt_exchange");
		if (!baseDir.exists()) {
			baseDir.mkdir();
		}
		return baseDir.getPath();
	}

	// Code adapted from Guava library
	public static String createTempFolder(String prefix) {
		final int TEMP_DIR_ATTEMPTS = 10000;
		String baseDir = getRootExchangeFolder();
		String baseName = prefix + Long.toString(System.currentTimeMillis())
				+ "_";

		for (int counter = 0; counter < TEMP_DIR_ATTEMPTS; counter++) {
			File tempDir = new File(baseDir, baseName + counter);
			if (tempDir.mkdir()) {
				return tempDir.getAbsolutePath();
			}
		}
		throw new IllegalStateException("Failed to create directory within "
				+ TEMP_DIR_ATTEMPTS + " attempts (tried " + baseName + "0 to "
				+ baseName + (TEMP_DIR_ATTEMPTS - 1) + ')');
	}

}
