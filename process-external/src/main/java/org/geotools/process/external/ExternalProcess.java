package org.geotools.process.external;

import java.io.File;
import java.io.FileFilter;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.ViewType;
import org.geotools.coverage.grid.io.AbstractGridCoverageWriter;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Parameter;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.util.ProgressListener;

public abstract class ExternalProcess implements Process {

	protected String tempLayersFolder;

	protected int nExportedLayers = 0;
	protected HashMap<String, Parameter<?>> inputs;
	protected HashMap<String, Parameter<?>> outputs;

	// This maps GeoTools objects to filenames of layer were they are saved.
	protected HashMap<Object, String> intermediateLayers = new HashMap<Object, String>();

	// This maps GeoTools objects to layers imported into a format
	// understandable by the external app (if needed).
	// These files are created from the ones referred in the exportedLayers map,
	// by using import processes from the app.
	// A single geotools object can be imported to several files, depending on the
	// external application
	protected HashMap<Object, String[]> exportedLayers = new HashMap<Object, String[]>();
	protected String name;
	protected String description;

	protected boolean isCleared = false;
	protected boolean isAppSpecificCleared = false;

	// A general ProcessGroup. Should be responsible of optimizing usage of
	// layers referred in the intermediateLayers map
	protected GeneralProcessGroup processGroup = null;

	// A process group specific for this process. Should be responsible of
	// optimizing usage of layers referred in the exportedLayers map
	protected AppSpecificProcessGroup appProcessGroup = null;

	// Export a grid coverage to a temporary file in TIF format
	protected String saveRasterLayer(GridCoverage2D gc) {
		// TODO:do not save if it is already file-based
		
		// we do not save if it has been saved before in this ProcessGroup (in
		// case the process belongs to one)
		if (processGroup!= null){
			String filename = processGroup.getLayerFilename(gc);
			if (filename != null) {
				return filename;
			}
		}
		try {
			String filename = getTempLayerFilename("raster", "tif");
			AbstractGridCoverageWriter writer = new GeoTiffWriter(new File(
					filename));
			writer.write(gc.view(ViewType.NATIVE), null);
			writer.dispose();
			intermediateLayers.put(gc, filename);
			if (processGroup != null) {
				processGroup.addLayerFilename(gc, filename);
			}
			return filename;
		} catch (Exception e) {
			throw new ProcessException("Error exporting grid coverage:\n"
					+ e.getMessage());
		}


	}

	// Exports a FeatureCollection to a temporary shapefile
	protected String saveVectorLayer(FeatureCollection fc) {
		// TODO:do not save if it is already file-based

		// we do not save if it has been saved before in this ProcessGroup (in
		// case the process belong to one)
		if (processGroup != null) {
			String filename = processGroup.getLayerFilename(fc);
			if (filename != null) {
				return filename;
			}
		}
		try {
			String filename = getTempLayerFilename("vector", "shp");

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
			intermediateLayers.put(fc, filename);
			if (processGroup != null) {
				processGroup.addLayerFilename(fc, filename);
			}
			return filename;
		} catch (Exception e) {
			throw new ProcessException("Error exporting feature collection:\n"
					+ e.getMessage());
		}

	}

	protected String getTempLayerFilename(String prefix, String ext) {
		String filename = prefix + Long.toString(System.currentTimeMillis())
				+ "_" + Integer.toString(nExportedLayers) + "." + ext;
		nExportedLayers++;
		return tempLayersFolder + File.separator + filename;
	}

	protected boolean deleteDir(File dir) {
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

	public String getDescription() {
		return description;
	}

	public Map<String, Parameter<?>> getParameterInfo() {
		return inputs;
	}

	public Map<String, Parameter<?>> getResultInfo() {
		return outputs;
	}

	public String getName() {
		return name;
	}

	public void setGeneralProcessGroup(GeneralProcessGroup pg) {
		processGroup = pg;
	}
	
	public void setAppSpecificProcessGroup(AppSpecificProcessGroup pg) {
		appProcessGroup = pg;
	}
	
	public Map<String, Object> execute(Map<String, Object> params,
			ProgressListener progress) throws ProcessException {

		tempLayersFolder = Utils.createTempFolder("gttemplayers");

		Map<String, Object> result = _execute(params, progress);

		if (processGroup == null){
			deleteIntermediateLayers();
		}
		if (appProcessGroup == null) {
			deleteExportedLayers();
		}
		return result;
	
	}
	
	public void deleteIntermediateLayers() {
		if (isCleared) {
		    return;
		}
		final Collection<String> layers = intermediateLayers.values();
		File[] filesToDelete = new File(tempLayersFolder).listFiles(new FileFilter() {
		    public boolean accept(File f) {
			String filename = f.getName();
			for (final String layer : layers) {
			    String baseFilename = new File(layer).getName();
			    baseFilename = baseFilename.substring(0, baseFilename.lastIndexOf("."));
			    if (filename.contains(baseFilename)) {
				return true;
			    }
			}
			return false;
		    }
		});
		for (File file : filesToDelete) {
		    file.delete();
		}
		isCleared = true;
	}

	public abstract void deleteExportedLayers();

	protected abstract Map<String, Object> _execute(Map<String, Object> params,
			ProgressListener progress) throws ProcessException;
	
	
}
