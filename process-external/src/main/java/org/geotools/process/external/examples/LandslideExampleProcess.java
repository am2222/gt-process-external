//package org.geotools.process.external.examples;
//
//import org.geotools.coverage.grid.GridCoverage2D;
//import org.geotools.feature.NameImpl;
//import org.geotools.process.ProcessException;
//import org.geotools.process.external.saga.SagaProcess;
//import org.geotools.process.external.saga.SagaProcessFactory;
//import org.geotools.process.factory.DescribeParameter;
//import org.geotools.process.factory.DescribeProcess;
//import org.geotools.process.factory.DescribeResult;
//import org.geotools.process.gs.GSProcess;
//import org.geotools.util.KVP;
//import org.opengis.util.ProgressListener;
//
//@DescribeProcess(title = "shalstab", description = "Shallow Slope Stability Model.")
//public class LandslideExampleProcess implements GSProcess {
//
//	@DescribeResult(name = "stability", description = "A stability layer")
//	public GridCoverage2D execute(
//			@DescribeParameter(name = "dem", description = "Elevation") GridCoverage2D dem,
//			@DescribeParameter(name = "c", description = "Soil Cohesion(Pa)",  defaultValue = "2500") Double c,
//			@DescribeParameter(name = "Ps", description = "Saturated Soil Density(kg/m3)", defaultValue = "1600") Double Ps,
//			@DescribeParameter(name = "z", description = "Soil Depth(m)", defaultValue = "1") Double z,
//			@DescribeParameter(name = "K", description = "Hydraulic  conductivity", defaultValue = "65") Double K,
//			@DescribeParameter(name = "psi", description = "Internal Friction Angle", defaultValue = "33") Double psi,
//			ProgressListener progress) throws ProcessException {
//
//		SagaProcessFactory fact = new SagaProcessFactory();
//		SagaProcess sinkremoval = fact.create(new NameImpl("saga",
//				"sinkremoval"));
//		GridCoverage2D preprocessedDEM = (GridCoverage2D) sinkremoval.execute(
//				new KVP("dem", dem), progress).get("dempreproc");
//		SagaProcess slopeproc = fact.create(new NameImpl("saga",
//				"slopeaspectcurvature"));
//		GridCoverage2D slope = (GridCoverage2D) slopeproc.execute(
//				new KVP("elevation", dem), progress).get("slope");
//		SagaProcess careaproc = fact.create(new NameImpl("saga",
//				"catchmentareaparallel"));
//		GridCoverage2D carea = (GridCoverage2D) careaproc.execute(
//				new KVP("elevation", dem, "method", Integer.valueOf(4)),
//				progress).get("carea");
//		
//		
//	}
//
//	/**
//	 * @param args
//	 */
//	public static void main(String[] args) {
//		// TODO Auto-generated method stub
//
//	}
//
// }
