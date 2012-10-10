package org.geotools.process.external.grass;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
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

public class GrassProcess extends ExternalProcess {

	public static final String GRASS_REGION_EXTENT_PARAMETER = "regionextent";
	public static final String GRASS_REGION_CELLSIZE_PARAMETER = "regioncellsize";
	public static final Object GRASS_LATLON = "latlon";

	private String grassCommand;
	private String gisdbase;

	public GrassProcess(String desc) {

		inputs = new HashMap<String, Parameter<?>>();
		outputs = new HashMap<String, Parameter<?>>();
		String[] lines = desc.split("\n");
		grassCommand = lines[0].trim();
		desc = lines[1].trim();
		name = desc.substring(0, desc.indexOf(" "));
		description = desc.substring(desc.indexOf(" ") + 2);
		for (int i = 3; i < lines.length; i++) {
			String line = lines[i].trim();
			if (line.startsWith("Parameter")) {
				Parameter param = Parameters.getInputFromString(line);
				inputs.put(param.key, param);
			} else if (line.equals("")) {
				break;
			} else {
				Parameter output = Parameters.getOutputFromString(line);
				outputs.put(output.key, output);
			}
		}

		Parameter param = new Parameter(GRASS_REGION_EXTENT_PARAMETER,
				ReferencedEnvelope.class, new SimpleInternationalString(
						"GRASS region extent"), new SimpleInternationalString(
						"GRASS region extent"), false, 0, 1, null, null);
		inputs.put(param.key, param);
		HashMap map = new HashMap();
		map.put(Parameter.MIN, 0);
		param = new Parameter(GRASS_REGION_CELLSIZE_PARAMETER, Number.class,
				new SimpleInternationalString("GRASS region cellsize"),
				new SimpleInternationalString("GRASS region cellsize"), false,
				0, 1, 1.0, map);
		inputs.put(param.key, param);

	}

	public Map<String, Object> _execute(Map<String, Object> params,
			ProgressListener progress) throws ProcessException {

		HashMap<String, String> outputFilenames = new HashMap<String, String>();

		if (Utils.isWindows()) {
			String path = GrassUtils.grassPath();
			if (path.equals("")) {
				throw new ProcessException(
						"GRASS folder is not configured.\nPlease configure it before running GRASS algorithms.");
			}
		}

		ArrayList<String> commands = new ArrayList<String>();
		exportedLayers = new HashMap<Object, String[]>();

		boolean latlon = false;
		if (params.containsKey(GRASS_LATLON)) {
			latlon = Boolean.valueOf(params.get(GRASS_LATLON).toString())
					.booleanValue();
		}

		try {
			gisdbase = GrassUtils.createMapset(latlon);
		} catch (IOException e) {
			throw new ProcessException("Error creating GRASS mapset:\n"
					+ e.getMessage());
		}

		// 1: Export layers to grass mapset.

		Set<String> set = inputs.keySet();
		Iterator<String> iter = set.iterator();
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
					|| (param.getType().isArray() && param.getClass()
							.getComponentType().equals(GridCoverage2D.class))) {
				if (param.getType().isArray()) {
					Object[] arr = (Object[]) value;
					for (int i = 0; i < arr.length; i++) {
						commands.add(exportRasterLayer((GridCoverage2D) arr[i]));
					}
				} else {
					commands.add(exportRasterLayer((GridCoverage2D) value));
				}
			} else if (param.getType().equals(FeatureCollection.class)
					|| (param.getType().isArray() && param.getClass()
							.getComponentType().equals(FeatureCollection.class))) {
				if (param.getType().isArray()) {
					Object[] arr = (Object[]) value;
					for (int i = 0; i < arr.length; i++) {
						commands.add(exportVectorLayer((FeatureCollection) arr[i]));
					}
				} else {
					commands.add(exportVectorLayer((FeatureCollection) value));
				}
			}

		}

		// 2. set GRASS region
		ReferencedEnvelope region = null;
		Double cellsize = null;
		if (params.containsKey(GRASS_REGION_EXTENT_PARAMETER)) {
			region = (ReferencedEnvelope) params
					.get(GRASS_REGION_EXTENT_PARAMETER);
		}
		if (params.containsKey(GRASS_REGION_CELLSIZE_PARAMETER)) {
			cellsize = ((Double) params.get(GRASS_REGION_CELLSIZE_PARAMETER));
		}

		// TODO consider multiple input
		String command = "g.region";
		if (region == null || cellsize == null) {
			boolean found = false;
			set = inputs.keySet();
			iter = set.iterator();
			while (iter.hasNext()) {
				String key = iter.next();
				Parameter param = inputs.get(key);
				Object value = null;
				if (params.containsKey(key)) {
					value = params.get(key);
				}
				if (value != null) {
					if (param.getType().equals(GridCoverage2D.class)) {
						command += " rast=" + exportedLayers.get(value)[0];
						found = true;
						break;
					}
					if (param.getType().equals(FeatureCollection.class)) {
						command += " vect=" + exportedLayers.get(value)[0];
						found = true;
						break;
					}
				}
			}
			if (!found) {
				throw new ProcessException(
						"GRASS region has to be explicitly defined");
			}
		} else {
			command += " n=" + Double.toString(region.getMaxY());
			command += " s=" + Double.toString(region.getMinY());
			command += " e=" + Double.toString(region.getMaxX());
			command += " w=" + Double.toString(region.getMinX());
			command += " res=" + Double.toString(cellsize);
		}
		commands.add(command);

		// 3. set parameter values and create command call

		command = new String(grassCommand);
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
				continue;
			}
			if (param.getName().equals(GRASS_REGION_CELLSIZE_PARAMETER)
					|| param.getName().equals(GRASS_REGION_EXTENT_PARAMETER)) {
				continue;
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
						s += exportedLayers.get(arr[i])[0];
					}
					command += " " + param.getName() + "=" + s;
				} else {
					command += " " + param.getName() + "="
							+ exportedLayers.get(value)[0];
				}
			} else if (param.getType().equals(Boolean.class)) {
				if (new Boolean(value.toString()).booleanValue()) {
					command += " -" + param.getName();
				}
			} else {
				command += " " + param.getName() + "=" + value.toString();
			}

		}

		set = outputs.keySet();
		iter = set.iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			Parameter param = outputs.get(key);
			if (param.getType().equals(String.class)) {
				String filename = getTempLayerFilename(key, "txt");
				outputFilenames.put(key, filename);
				command += ' ' + param.getName() + "=" + filename + "\"";
			} else {
				String filename = null;
				if (param.getType().equals(GridCoverage2D.class)) {
					filename = getTempLayerFilename(key, "tif");
				} else {
					filename = getTempLayerFilename(key, "shp");
				}
				outputFilenames.put(key, filename);
				command += ' ' + param.getName() + '=' + param.getName();
			}
		}

		command += " --overwrite";
		commands.add(command);

		// 4:Export resulting layers to a format that geotools can read

		set = outputs.keySet();
		iter = set.iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			Parameter param = outputs.get(key);
			if (param.getType().equals(GridCoverage2D.class)) {
				String filename = outputFilenames.get(key);
				commands.add("g.region rast=" + param.getName());
				command = "r.out.gdal -c createopt=\"TFW=YES,COMPRESS=LZW\"";
				command += " input=";
				command += param.getName();
				command += " output=\"" + filename + "\"";
				commands.add(command);
			} else if (param.getType().equals(FeatureCollection.class)) {
				String filename = outputFilenames.get(key);
				command = "v.out.ogr -ce input=" + param.getName();
				command += " dsn=\"" + new File(filename).getParent() + "\"";
				command += " format=ESRI_Shapefile";
				String name = new File(filename).getName();
				name = name.substring(0, name.length() - 4);
				command += " olayer=" + name;
				command += " type=auto";
				commands.add(command);
			}
		}

		// 5. Run GRASS

		GrassUtils.executeGrass(commands, gisdbase, progress);

		// 6. Open resulting layers and return results map

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

	private String exportVectorLayer(FeatureCollection fc) {
		String intermediateFilename = saveVectorLayer(fc);
		String destFilename = getTempFilename();
		exportedLayers.put(fc, new String[] { destFilename });
		String command = "v.in.ogr";
		command += " min_area=-1";
		command += " dsn=\"" + new File(intermediateFilename).getParent()
				+ "\"";
		String name = new File(intermediateFilename).getName();
		name = name.substring(0, name.length() - 4);
		command += " layer=" + name;
		command += " output=" + destFilename;
		command += " --overwrite -o";
		return command;
	}

	private String exportRasterLayer(GridCoverage2D gc) {
		String intermediateFilename = saveRasterLayer(gc);
		String destFilename = getTempFilename();
		exportedLayers.put(gc, new String[] { destFilename });
		String command = "r.in.gdal";
		command += " input=\"" + intermediateFilename + "\"";
		command += " band=1";
		command += " out=" + destFilename;
		command += " --overwrite -o";
		return command;
	}

	private String getTempFilename() {
		long milisecs = new Date().getTime();
		String filename = "tmp" + Long.toString(milisecs)
				+ Integer.toString(nExportedLayers);
		nExportedLayers += 1;
		return filename;
	}

	@Override
	public void deleteExportedLayers() {
		if (isAppSpecificCleared) {
			return;
		}
		// we just delete the mapset
		deleteDir(new File(gisdbase));

	}

}
