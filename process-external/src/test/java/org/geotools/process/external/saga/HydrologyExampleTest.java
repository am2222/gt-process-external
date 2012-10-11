package org.geotools.process.external.saga;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.process.external.examples.HydrologyExampleProcess;
import org.junit.Test;

import com.vividsolutions.jts.geom.Coordinate;

public class HydrologyExampleTest {


	private GridCoverage2D getDEM() {

		URL url = this.getClass().getResource("/dem25.tif");
		File file = new File(url.getFile()); 		

		AbstractGridFormat format = GridFormatFinder.findFormat( file );
		AbstractGridCoverage2DReader reader = format.getReader( file );				
		GridCoverage2D coverage;
		try {
			coverage = (GridCoverage2D) reader.read(null);
		} catch (IllegalArgumentException e) {
			return null;
		} catch (IOException e) {
			return null;
		}
		return coverage;
		
	}

	@Test
	public void testViewshed() {

		GridCoverage2D gc = getDEM();
		HydrologyExampleProcess proc = new HydrologyExampleProcess();
		FeatureCollection result = proc.execute(gc, Double.valueOf(1000000),new Coordinate(1, 1), null);		
		assertEquals(result.size(), 62);		

	}

}
