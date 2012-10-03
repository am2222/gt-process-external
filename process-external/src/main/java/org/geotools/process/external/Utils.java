package org.geotools.process.external;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.ViewType;
import org.geotools.coverage.grid.io.AbstractGridCoverageWriter;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.process.ProcessException;
import org.opengis.feature.simple.SimpleFeatureType;

public class Utils {

	private static final String tempLayersFolder = createTempFolder("gttemplayers");
	private static int nExportedLayers = 0;
	private static int nTempFilenames = 0;

	public static boolean isWindows() {
		return true;
	}

	// Export a grid coverage to a temporary file in TIF format
	public static String exportRasterLayer(GridCoverage2D gc) {
		try {
			String filename = getTempOutputFilename("raster", "tif");
			AbstractGridCoverageWriter writer = new GeoTiffWriter(new File(
					filename));
			writer.write(gc.view(ViewType.NATIVE), null);
			writer.dispose();
			return filename;
		} catch (Exception e) {
			throw new ProcessException("Error exporting grid coverage:\n"
					+ e.getMessage());
		}

	}

	// Exports a FeatureCollection to a temporary shapefile
	public static String exportVectorLayer(FeatureCollection fc) {
		try {
			String filename = getTempOutputFilename("vector", "shp");

			ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

			Map<String, Serializable> params = new HashMap<String, Serializable>();
			params.put("url", new File(filename).toURI().toURL());
			params.put("create spatial index", Boolean.TRUE);

			ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory
					.createNewDataStore(params);
			newDataStore.createSchema((SimpleFeatureType) fc.getSchema());
			Transaction transaction = new DefaultTransaction("create");

			String typeName = newDataStore.getTypeNames()[0];
			SimpleFeatureSource featureSource = newDataStore
					.getFeatureSource(typeName);

			if (featureSource instanceof SimpleFeatureStore) {
				SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
				featureStore.setTransaction(transaction);
				try {
					featureStore.addFeatures(fc);
					transaction.commit();
				} catch (Exception e) {
					transaction.rollback();
					throw e;
				} finally {
					transaction.close();
				}
			} else {
				// should not reach here
			}
			return filename;
		} catch (Exception e) {
			throw new ProcessException("Error exporting feature collection:\n"
					+ e.getMessage());
		}

	}

	public static String getTempOutputFilename(String prefix, String ext) {
		String filename = prefix + Long.toString(System.currentTimeMillis())
				+ "_" + Integer.toString(nExportedLayers) + "." + ext;
		nExportedLayers++;
		return tempLayersFolder + File.separator + filename;
	}

	public static String getTempFilename(String prefix, String ext) {
		String filename = prefix + Long.toString(System.currentTimeMillis())
				+ "_" + Integer.toString(nTempFilenames) + "." + ext;
		nTempFilenames++;
		return getRootExchangeFolder() + File.separator + filename;
	}

	private static final String getRootExchangeFolder() {
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

	public static boolean deleteDir(File dir) {
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}
		return dir.delete();
	}

}
