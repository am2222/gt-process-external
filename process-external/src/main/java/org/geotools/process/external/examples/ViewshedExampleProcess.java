package org.geotools.process.external.examples;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.feature.NameImpl;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.external.grass.GrassProcessFactory;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.gs.GSProcess;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * A process that calculates the set of cells in a terrain that are viewed from
 * a given point, the so-called vievshed of the point, based on the
 * corresponding GRASS algorithm (r.los).
 * 
 * 
 * @author Volaya
 * 
 */

@DescribeProcess(title = "Viewshed", description = "Calculates the viewshed of a given point over a given terrain.")
public class ViewshedExampleProcess implements GSProcess {

	@DescribeResult(name = "viewshed", description = "A layer with a value of 1 in those cells belonging to the viewshed, 0 otherwise")
	public GridCoverage2D execute(
			@DescribeParameter(name = "dem", description = "Elevation") GridCoverage2D dem,
			@DescribeParameter(name = "observerHeight", description = "Observer's height") Double observerHeight,
			@DescribeParameter(name = "maxDist", description = "Maximum observation distance") Double maxDist,
			@DescribeParameter(name = "point", description = "The observation point") Coordinate pt,
			ProgressListener progress) throws ProcessException {
		
		GrassProcessFactory fact = new GrassProcessFactory();
		Process proc = fact.create(new NameImpl("grass", "r.los"));
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("input", dem);
		map.put("obs_elev", observerHeight);
		map.put("max_dist", maxDist);
		map.put("coordinate",
				Double.toString(pt.x) + "," + Double.toString(pt.y));
		return (GridCoverage2D) proc.execute(map, progress).get("output");

	}

}
