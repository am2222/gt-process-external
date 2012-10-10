package org.geotools.process.external.examples;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.process.ProcessException;
import org.geotools.process.external.SagaProcessGroup;
import org.geotools.process.external.saga.SagaProcess;
import org.geotools.process.external.saga.SagaProcessFactory;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.gs.GSProcess;
import org.geotools.util.KVP;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * This process is an example of a complex process created using several processes
 * from an external application.
 * 
 * It takes the coordinates of a point, a DEM and a threshold for channel definition.
 * Based on that, it extracts the channel network, delineates the subbasins and calculates
 * for each subbasin additional parameters indicating the risk of erosion.
 * 
 * @author Volaya
 *
 */
@DescribeProcess(title = "Erosion risk analysis", description = "Delineates subbasins and calculates erosion risk for each of them.")
public class HydrologyExampleProcess implements GSProcess {

	@DescribeResult(name = "watersheds", description = "Watersheds")
	public FeatureCollection execute(
			@DescribeParameter(name = "dem", description = "DEM") GridCoverage2D dem,
			@DescribeParameter(name = "threshold", description = "Threshold for channel definition") Double threshold,
			@DescribeParameter(name = "point", description = "Outlet point") Coordinate pt,
			ProgressListener progress) throws ProcessException {
		
		SagaProcessFactory fact = new SagaProcessFactory();
		SagaProcessGroup pg = new SagaProcessGroup();
		
		SagaProcess careaproc = fact.create(new NameImpl("saga","catchmentareaparallel"));
		pg.addProcess(careaproc);
		GridCoverage2D carea = (GridCoverage2D) careaproc.execute(
				new KVP("elevation", dem, "method", Integer.valueOf(4)),progress).get("carea");
		
//		SagaProcess upslopeareaproc = fact.create(new NameImpl("saga","upslopearea"));
//		pg.addProcess(upslopeareaproc);
//		GridCoverage2D mask = (GridCoverage2D) upslopeareaproc.execute(
//				new KVP("elevation", dem, "target_pt_x", Double.valueOf(pt.x), "target_pt_y", Double.valueOf(pt.y)),
//				progress).get("area");
		
		SagaProcess channelproc = fact.create(new NameImpl("saga","channelnetwork"));
		pg.addProcess(channelproc);
		GridCoverage2D channels = (GridCoverage2D) channelproc.execute(
				new KVP("elevation", dem, "init_method", Integer.valueOf(2), 
						"init_value", Double.valueOf(threshold),
						"init_grid", carea),
				progress).get("chnlntwrk");
		
		SagaProcess basinsproc = fact.create(new NameImpl("saga","watershedbasins"));
		pg.addProcess(basinsproc);
		GridCoverage2D basins = (GridCoverage2D) basinsproc.execute(
				new KVP("elevation", dem, "init_method", Integer.valueOf(2), 
						"channels", channels),
				progress).get("basins");
			
		SagaProcess vectorisinproc = fact.create(new NameImpl("saga","vectorisinggridclasses"));
		pg.addProcess(vectorisinproc);
		FeatureCollection vectorBasins = (FeatureCollection) vectorisinproc.execute(
				new KVP("grid", basins, "area", carea, 
						"class_all", Integer.valueOf(1)),
				progress).get("polygons");
		
		SagaProcess slopeproc = fact.create(new NameImpl("saga","slopeaspectcurvature"));
		pg.addProcess(slopeproc);
		GridCoverage2D slope = (GridCoverage2D) slopeproc.execute(
				new KVP("elevation", dem),
				progress).get("slope");
		
		SagaProcess lsfactorproc = fact.create(new NameImpl("saga","lsfactor"));
		pg.addProcess(lsfactorproc);
		GridCoverage2D lsfactor = (GridCoverage2D) lsfactorproc.execute(
				new KVP("slope", slope, "area", carea, 
						"conv", Integer.valueOf(1)),
				progress).get("ls");
		
		SagaProcess statsproc = fact.create(new NameImpl("saga","gridstatisticsforpolygons"));
		pg.addProcess(statsproc);
		FeatureCollection extendedVectorBasins = (FeatureCollection) statsproc.execute(
				new KVP("grids", new GridCoverage2D[]{lsfactor},  
						"polygons", vectorBasins),
				progress).get("result");
		
		pg.finish();
		
		return extendedVectorBasins;
		
		
	}
	
}