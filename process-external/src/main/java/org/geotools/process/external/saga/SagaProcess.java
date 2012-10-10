package org.geotools.process.external.saga;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.DataSourceException;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.Parameter;
import org.geotools.factory.Hints;
import org.geotools.feature.FeatureCollection;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.ProcessException;
import org.geotools.process.external.ExternalProcess;
import org.geotools.process.external.Parameters;
import org.geotools.process.external.Utils;
import org.geotools.util.SimpleInternationalString;
import org.opengis.util.ProgressListener;

public class SagaProcess extends ExternalProcess {

	public static final String SAGA_OUTPUT_EXTENT = "extent";

	private String cmdname;
	private String modulelib;
	private String[] extentParamNames;
	private HashMap<String, String[]> fixedTableCols;
	HashMap<String, String> outputFilenames = new HashMap<String, String>();

	public SagaProcess(String desc) {
		outputFilenames = new HashMap<String, String>();
		inputs = new HashMap<String, Parameter<?>>();
		outputs = new HashMap<String, Parameter<?>>();
		fixedTableCols = new HashMap<String, String[]>();
		String[] lines = desc.split("\n");
		description = cmdname = lines[0].trim();
		
		if (description.contains("|")){
			String[] tokens = description.split("\\|");
			cmdname = tokens[1];
			description = tokens[0];
		}		
		name = description.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
		modulelib = lines[1].trim();
		for (int i = 2; i < lines.length; i++) {
			String line = lines[i].trim();
			if (line.startsWith("ParameterSelection")) {//we treat this differently, as it is number-based, not string-based
				HashMap map = new HashMap();
				String[] tokens = line.split("\\|");
				ArrayList list = new ArrayList();
				String[] options = tokens[3].split(";");
				for (int j = 0; j < options.length; j++) {
					list.add(new Integer(j));
				}
				map.put(Parameter.OPTIONS, list);
				Parameter param = new Parameter(tokens[1], Integer.class,
						new SimpleInternationalString(tokens[1]),
						new SimpleInternationalString(tokens[2]), true, 0, 1,
						new Integer(0), map);
				inputs.put(param.key.toLowerCase(), param);
			} else if (line.startsWith("ParameterFixedTable")){//also this,since column names have to be stored
				String[] tokens = line.split("\\|");
				HashMap map = new HashMap();
				map.put(Parameter.ELEMENT, Double.class);
				Parameter param = new Parameter(tokens[1], Double[][].class,
						new SimpleInternationalString(tokens[1]),
						new SimpleInternationalString(tokens[2]), true, 1, 1,
						null, map);
				String[] cols = tokens[4].split(";");
				fixedTableCols.put(param.key, cols);
				inputs.put(param.key.toLowerCase(), param);
			} else if (line.startsWith("Parameter")) {
				Parameter param = Parameters.getInputFromString(line);
				inputs.put(param.key.toLowerCase(), param);
			} else if (line.startsWith("Extent")) {
				Parameter param = new Parameter(this.SAGA_OUTPUT_EXTENT,
						ReferencedEnvelope.class,
						new SimpleInternationalString("Output extent"),
						new SimpleInternationalString("Output extent"), true,
						1, 1, null, null);
				inputs.put(this.SAGA_OUTPUT_EXTENT, param);
				extentParamNames = line.substring(6).split(" ");
			} else if (line.equals("")) {
				break;
			} else {
				Parameter output = Parameters.getOutputFromString(line);
				outputs.put(output.key.toLowerCase(), output);
			}
		}


	}

	public Map<String, Object> _execute(Map<String, Object> params_org,
			ProgressListener progress) throws ProcessException {

		Map<String, Object> params = new HashMap<String, Object>();

		Set<String> set = params_org.keySet();
		Iterator<String> iter = set.iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			params.put(key.toLowerCase(), params_org.get(key));
		}		
		
		ArrayList<String> commands = new ArrayList<String>();
		exportedLayers = new HashMap<Object, String[]>();
		
		//1. Export layers
		
		set = inputs.keySet();
		iter = set.iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			Parameter param = inputs.get(key);
			Object value = null;
			if (params.containsKey(key)) {
				value = params.get(key);
			}
			if (value == null) {
				if (param.getMinOccurs() == 0) {
					continue;
				} else {
					throw new ProcessException("Parameter " + param.getName()
							+ " is not optional");
				}
			}
			if (param.getType().equals(GridCoverage2D.class)
					|| (param.getType().isArray() && param.getType()
							.getComponentType().equals(GridCoverage2D.class))) {
				if (param.getType().isArray()) {
					Object[] arr = (Object[]) value;
					for (int i = 0; i < arr.length; i++) {
						String exportCommand = exportRasterLayer((GridCoverage2D) arr[i]); 
						if (exportCommand != null){
							commands.add(exportCommand);
						}
					}
				} else {
					String exportCommand = exportRasterLayer((GridCoverage2D) value); 
					if (exportCommand != null){
						commands.add(exportCommand);
					}					
				}
			} else if (param.getType().equals(FeatureCollection.class)
					|| (param.getType().isArray() && param.getType()
							.getComponentType().equals(FeatureCollection.class))) {
				if (param.getType().isArray()) {
					Object[] arr = (Object[]) value;
					for (int i = 0; i < arr.length; i++) {
						exportVectorLayer((FeatureCollection) arr[i]);
					}
				} else {
					exportVectorLayer((FeatureCollection) value);
				}
			}

		}

		// 2. set parameter values and create command call

		String command = new String();
		
		if (Utils.isWindows()){
            command = modulelib + " \"" + cmdname + "\"";
		}
		else{
			command = "lib" + modulelib + " \"" + cmdname + "\"";
		}
		
		set = inputs.keySet();
		iter = set.iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			Parameter param = inputs.get(key);
			Object value = null;
			if (params.containsKey(key)) {
				value = params.get(key);
			}
			if (value == null) {
				value = param.getDefaultValue();
				if (value == null) {
					continue;
				}
			}			

			if (param.getType().equals(GridCoverage2D.class)
					|| param.getType().equals(FeatureCollection.class)
					|| (param.getType().isArray() && param.getType()
							.getComponentType().equals(GridCoverage2D.class))
					|| (param.getType().isArray() && param.getType()
							.getComponentType().equals(FeatureCollection.class))) {
				if (param.getType().isArray()) {
					Object[] arr = (Object[]) value;
					String s = "";
					for (int i = 0; i < arr.length; i++) {
						if (i != 0) {
							s += ",";
						}
						String[] layers = exportedLayers.get(arr[i]);
						for (int j = 0; j < layers.length; j++) {
							if (j != 0) {
								s += ",";
							}
							s += layers[j];
						}
					}
					command += " -" + param.getName() + " \"" + s + "\"";
				} else {
					command += " -" + param.getName() + " \""
							+ exportedLayers.get(value)[0] + "\"";
				}
			}else if (param.getType().equals(Double[][].class)) {
                String tempTableFile = Utils.getTempFilename("sagatable", "txt");
                try {
        			new File(tempTableFile).createNewFile();
        			final FileWriter fstream = new FileWriter(tempTableFile);
        			final BufferedWriter fout = new BufferedWriter(fstream);	        			
	                String[] cols = fixedTableCols.get(param.getName());
	                for (int j = 0; j < cols.length; j++) {
						if (j!=0){
							fout.write("\t");
						}						
	                	fout.write(cols[j]);
					}	                
	                Double[][] m = (Double[][])value;
	                for (int i = 0; i < m.length; i++) {
	                	fout.write("\n");
	                	for (int j = 0; j < m.length; j++) {
	                		if (j!=0){
								fout.write("\t");
							}						
		                	fout.write(Double.toString(m[i][j]));
						}	
	                }
	                fout.close();
	                command += " -" + param.getName() + " \"" + tempTableFile + "\"";
                }
                catch(IOException e){
                	throw new ProcessException("Error creating SAGA table file:\n"+e.getMessage());
                }
			} else if (param.getType().equals(ReferencedEnvelope.class)) {
				ReferencedEnvelope envelope = (ReferencedEnvelope) value;
				command += " -" + extentParamNames[0] + " "
						+ envelope.getMinX();
				command += " -" + extentParamNames[1] + " "
						+ envelope.getMaxX();
				command += " -" + extentParamNames[2] + " "
						+ envelope.getMinY();
				command += " -" + extentParamNames[3] + " "
						+ envelope.getMaxY();
			} else if (param.getType().equals(Boolean.class)) {
				if (new Boolean(value.toString()).booleanValue()) {
					command += " -" + param.getName();
				}
			}  else if (param.getType().equals(String.class)) {			
				command += " -" + param.getName() + " \"" + value.toString()
						+ "\"";
			}
			else {			
				command += " -" + param.getName() + " " + value.toString();
			}

		}

		set = outputs.keySet();
		iter = set.iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			Parameter param = outputs.get(key);			
				String filename = null;
				if (param.getType().equals(GridCoverage2D.class)) {
				filename = getTempLayerFilename(key, "tif");
				command += " -" + param.getName() + " \"" + filename
						+ ".sgrd\"";
				} else {
						// {
				filename = getTempLayerFilename(key, "shp");
				command += " -" + param.getName() + " \"" + filename + "\"";
				}
				outputFilenames.put(key, filename);							
		}
		
		commands.add(command);

		// 3:Export resulting raster layers to a format that geotools can read

		set = outputs.keySet();
		iter = set.iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			Parameter param = outputs.get(key);
			if (param.getType().equals(GridCoverage2D.class)) {
				String filename = outputFilenames.get(key);					                    
        		if (Utils.isWindows()){
                    commands.add("io_gdal 1 -GRIDS \"" + filename + ".sgrd"
                                    + "\" -FORMAT 1 -TYPE 0 -FILE \""
                                    + filename + "\"");
                }
                else{
                	commands.add("io_gdal 1 -GRIDS \"" + filename + ".sgrd"
                            + "\" -FORMAT 1 -TYPE 0 -FILE \""
                            + filename + "\"");
                }
			} 
		}

		SagaUtils.executeSaga(commands.toArray(new String[0]), progress);

		// 4. Open resulting layers and return results map

		HashMap<String, Object> results = new HashMap<String, Object>();
		set = outputs.keySet();
		iter = set.iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			Parameter param = outputs.get(key);
			String filename = outputFilenames.get(key);
			if (param.getType().equals(GridCoverage2D.class)) {
				GeoTiffReader reader;
				try {
					reader = new GeoTiffReader(new File(filename), new Hints(
							Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER,
							Boolean.TRUE));
					if (reader != null) {
						GridCoverage2D gc = (GridCoverage2D) reader.read(null);
						results.put(key, gc);
						if (appProcessGroup != null){
							appProcessGroup.addLayerFilename(gc, new String[]{filename + ".sgrd"});
						}
					}
				} catch (DataSourceException e) {
					throw new ProcessException("Error reading result layers:\n"
							+ e.getMessage());
				} catch (IOException e) {
					throw new ProcessException("Error reading result layers:\n"
							+ e.getMessage());
				}
			} else if (param.getType().equals(FeatureCollection.class)) {
				try {
					File file = new File(filename);
					Map map = new HashMap();
					map.put("url", file.toURL());
					DataStore dataStore = DataStoreFinder.getDataStore(map);
					String typeName = dataStore.getTypeNames()[0];
					FeatureSource source = dataStore.getFeatureSource(typeName);
					FeatureCollection fc = source.getFeatures();
					results.put(key, fc);
					if (appProcessGroup != null){
						appProcessGroup.addLayerFilename(fc, new String[]{filename});
					}
				} catch (IOException e) {
					throw new ProcessException("Error reading result layers:\n"
							+ e.getMessage());
				}
			} else {
				results.put(key, filename);
			}
		}

		return results;

	}

	private void exportVectorLayer(FeatureCollection fc) {
		String intermediateFilename = saveVectorLayer(fc);
		exportedLayers.put(fc, new String[] { intermediateFilename });
	}

	private String exportRasterLayer(GridCoverage2D gc) {
		// we do not save if it has been saved before in this SagaProcessGroup (in
		// case the process belong to one)
		if (appProcessGroup != null) {
			String[] filenames = appProcessGroup.getLayerFilenames(gc);
			if (filenames != null) {
				exportedLayers.put(gc, filenames);
				return null;
			}
		}
		String intermediateFilename = saveRasterLayer(gc);
		String destFilename = getTempLayerFilename("raster", "sgrd");
		if (gc.getNumSampleDimensions() > 1) {
			String[] filenames = new String[gc.getNumSampleDimensions()];
			String basename = destFilename.substring(0,
					destFilename.lastIndexOf("."))
					+ "_000";
			for (int i = 0; i < filenames.length; i++) {
				filenames[i] = basename + Integer.toString(i) + ".sgrd";
			}
			exportedLayers.put(gc, filenames);
			if (appProcessGroup != null){
				appProcessGroup.addLayerFilename(gc, filenames);
			}
		} else {
			exportedLayers.put(gc, new String[] { destFilename });
			if (appProcessGroup != null){
				appProcessGroup.addLayerFilename(gc, new String[] { destFilename });
			}
		}
		if (Utils.isWindows()) {
			return "io_gdal 0 -GRIDS \"" + destFilename + "\" -FILES \""
					+ intermediateFilename + "\"";
		} else {
			return "libio_gdal 0 -GRIDS \"" + destFilename + "\" -FILES \""
					+ intermediateFilename + "\"";
		}

	}


	@Override
	public void deleteExportedLayers() {

		if (isAppSpecificCleared) {
			return;
		}
		final Collection<String[]> inputLayers = exportedLayers.values();
		final Collection<String> outputLayers = outputFilenames.values();
		File[] filesToDelete = new File(tempLayersFolder).listFiles(new FileFilter() {
			public boolean accept(File f) {
				String filename = f.getName();
				//check if it is an input intermediate files	
				for (final String[] layer : inputLayers) {
					for (int i = 0; i < layer.length; i++) {
						String baseFilename = new File(layer[i]).getName();
						baseFilename = baseFilename.substring(0,
								baseFilename.lastIndexOf("."));
						if (filename.contains(baseFilename)) {
							return true;
						}
					}
				}
				//or an output one
				for (final String layer : outputLayers) {				
					String baseFilename = new File(layer).getName();
					if (baseFilename.endsWith("tif")){					
						if (filename.contains(baseFilename)) {
							return true;
						}
					}					
				}
				return false;
			}
		});
		for (File file : filesToDelete) {
			file.delete();
		}

		isAppSpecificCleared = true;

	}

}
