package org.geotools.process.external;

import java.util.ArrayList;
import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.Parameter;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.SimpleInternationalString;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class Parameters {

	public static Parameter getInputFromString(String line) {

		Parameter param = null;

		String[] tokens = line.split("\\|");
		if (tokens[0].equals("ParameterBoolean")) {
			param = new Parameter(tokens[1].replace("-", ""), Number.class,
					new SimpleInternationalString(tokens[1]),
					new SimpleInternationalString(tokens[2]), false, 0, 1,
					Boolean.valueOf(tokens[3]), null);
		} else if (tokens[0].equals("ParameterNumber")) {
			HashMap map = new HashMap();
			try {
				double min = Double.valueOf(tokens[3]);
				map.put(Parameter.MIN, min);
			} catch (Exception e) {
			}
			try {
				double max = Double.valueOf(tokens[4]);
				map.put(Parameter.MAX, max);
			} catch (Exception e) {
			}
			param = new Parameter(tokens[1], Number.class,
					new SimpleInternationalString(tokens[1]),
					new SimpleInternationalString(tokens[2]), false, 0, 1,
					Double.valueOf(tokens[5]), map);

		} else if (tokens[0].equals("ParameterRaster")) {
			int min = 1;
			if (Boolean.valueOf(tokens[3]).booleanValue()) {
				min = 0;
			}
			param = new Parameter(tokens[1], GridCoverage2D.class,
					new SimpleInternationalString(tokens[1]),
					new SimpleInternationalString(tokens[2]), min > 0, min, 1,
					null, null);
		} else if (tokens[0].equals("ParameterString")) {
			String def = null;
			if (tokens.length > 3) {
				def = tokens[3];
			}
			param = new Parameter(tokens[1], Number.class,
					new SimpleInternationalString(tokens[1]),
					new SimpleInternationalString(tokens[2]), true, 1, 1, def,
					null);
		} else if (tokens[0].equals("ParameterVector")) {
			int min = 1;
			if (Boolean.valueOf(tokens[3]).booleanValue()) {
				min = 0;
			}
			param = new Parameter(tokens[1], FeatureCollection.class,
					new SimpleInternationalString(tokens[1]),
					new SimpleInternationalString(tokens[2]), min > 0, min, 1,
					null, null);
		} else if (tokens[0].equals("ParameterTableField")) {
			param = new Parameter(tokens[1], String.class,
					new SimpleInternationalString(tokens[1]),
					new SimpleInternationalString(tokens[2]));
		} else if (tokens[0].equals("ParameterSelection")) {
			HashMap map = new HashMap();
			ArrayList list = new ArrayList();
			String[] options = tokens[3].split(";");
			for (int i = 0; i < options.length; i++) {
				list.add(options[i]);
			}
			map.put(Parameter.OPTIONS, list);
			param = new Parameter(tokens[1], Number.class,
					new SimpleInternationalString(tokens[1]),
					new SimpleInternationalString(tokens[2]), true, 1, 1,
					options[0], map);

		} else if (tokens[0].equals("ParameterExtent")) {
			param = new Parameter(tokens[1], ReferencedEnvelope.class,
					new SimpleInternationalString(tokens[1]),
					new SimpleInternationalString(tokens[2]));
		} else if (tokens[0].equals("ParameterCrs")) {
			param = new Parameter(tokens[1], CoordinateReferenceSystem.class,
					new SimpleInternationalString(tokens[1]),
					new SimpleInternationalString(tokens[2]));
		} else if (tokens[0].equals("ParameterFile")) {
			param = new Parameter(tokens[1], String.class,
					new SimpleInternationalString(tokens[1]),
					new SimpleInternationalString(tokens[2]));
		} else if (tokens[0].equals("ParameterMultipleInput")) {
			// TODO: *********
			param = new Parameter(tokens[1], String.class,
					new SimpleInternationalString(tokens[1]),
					new SimpleInternationalString(tokens[2]));
		} else {
			param = new Parameter(tokens[1], String.class,
					new SimpleInternationalString(tokens[1]),
					new SimpleInternationalString(tokens[2]));
		}

		return param;

	}

	public static Parameter getOutputFromString(String line) {

		Parameter param = null;
		String[] tokens = line.split("\\|");
		if (tokens[0].equals("OutputRaster")) {
			param = new Parameter(tokens[1], GridCoverage2D.class,
					new SimpleInternationalString(tokens[1]),
					new SimpleInternationalString(tokens[2]), true, 1, 1, null,
					null);
		} else if (tokens[0].equals("OutputVector")) {
			param = new Parameter(tokens[1], FeatureCollection.class,
					new SimpleInternationalString(tokens[1]),
					new SimpleInternationalString(tokens[2]), true, 0, 1, null,
					null);
		} else if (tokens[0].equals("OutputFile")) {
			param = new Parameter(tokens[1], String.class,
					new SimpleInternationalString(tokens[1]),
					new SimpleInternationalString(tokens[2]));
		} else {
			param = new Parameter(tokens[1], Object.class,
					new SimpleInternationalString(tokens[1]),
					new SimpleInternationalString(tokens[2]));
		}

		return param;

	}

}
