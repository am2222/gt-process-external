package org.geotools.process.external.examples;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.external.saga.SagaProcessFactory;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.gs.GSProcess;
import org.opengis.util.ProgressListener;

/**
 * A process that performs supervised classification and vectorises the resulting layer
 * This could be used as a rendering transform to perform on-the-fly classification and vectorization
 * 
 * @author Volaya
 * 
 */

@DescribeProcess(title = "Viewshed", description = "Calculates the viewshed of a given point over a given terrain.")
public class SupervisedClassificationExampleProcess implements GSProcess {

	@DescribeResult(name = "viewshed", description = "A layer with a value of 1 in those cells belonging to the viewshed, 0 otherwise")
	public GridCoverage2D execute(
			@DescribeParameter(name = "image", description = "Image to classify") GridCoverage2D image,
			@DescribeParameter(name = "training", description = "Training areas") FeatureCollection fc,
			@DescribeParameter(name = "training", description = "Training areas") String field,
			ProgressListener progress) throws ProcessException {

		SagaProcessFactory fact = new SagaProcessFactory();
		Process proc = fact.create(new NameImpl("saga",
				"supervisedclassification"));
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("grids", new GridCoverage2D[] { image });
		map.put("roi", fc);
		map.put("roi_id", field);
		return (GridCoverage2D) proc.execute(map, progress).get("classes");

	}

}
