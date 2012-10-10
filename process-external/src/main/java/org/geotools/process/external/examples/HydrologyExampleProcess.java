//package org.geotools.process.external.examples;
//
//import org.geotools.coverage.grid.GridCoverage2D;
//import org.geotools.feature.FeatureCollection;
//import org.geotools.process.ProcessException;
//import org.geotools.process.factory.DescribeParameter;
//import org.geotools.process.factory.DescribeProcess;
//import org.geotools.process.factory.DescribeResult;
//import org.geotools.process.gs.GSProcess;
//import org.opengis.util.ProgressListener;
//
///**
// * This process is an example of a complex process created using several processes
// * from an external application.
// * 
// * It takes the coordinates of a point, a DEM and a threshold for channel definition.
// * Based on that, it extracts the channel network, delineates the subbasins
// * @author Volaya
// *
// */
//@DescribeProcess(title = "Erosion risk analysis", description = "Delineates subbasins and calculates erosion risk for each of them.")
//public class HydrologyExampleProcess implements GSProcess {
//
//	@DescribeResult(name = "watersheds", description = "Watersheds")
//	public GridCoverage2D execute(
//			@DescribeParameter(name = "image", description = "Image to classify") GridCoverage2D image,
//			@DescribeParameter(name = "training", description = "Training areas") FeatureCollection fc,
//			@DescribeParameter(name = "training", description = "Training areas") String field,
//			ProgressListener progress) throws ProcessException {
//		
//		
//		
//	}
//	
//}