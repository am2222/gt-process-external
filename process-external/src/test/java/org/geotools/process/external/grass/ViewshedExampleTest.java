package org.geotools.process.external.grass;

import static org.junit.Assert.assertTrue;

import java.awt.image.Raster;

import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.ViewType;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.external.examples.ViewshedExampleProcess;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;

import com.vividsolutions.jts.geom.Coordinate;

public class ViewshedExampleTest {

	private static final GridCoverageFactory covFactory = CoverageFactoryFinder
			.getGridCoverageFactory(null);

	private GridCoverage2D createCone() {
		int SIZE = 100;
		int CENTER = SIZE / 2;
		ReferencedEnvelope env;
		double MAX_ELEVATION = Math.sqrt(CENTER * CENTER + CENTER * CENTER);
		env = new ReferencedEnvelope(0, SIZE, 0, SIZE,
				DefaultGeographicCRS.WGS84);
		float[][] data = new float[SIZE][SIZE];
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < SIZE; x++) {
				int dx = Math.abs(CENTER - x);
				int dy = Math.abs(CENTER - y);
				double dist = Math.sqrt(dx * dx + dy * dy);
				double elevation = MAX_ELEVATION - dist;
				data[y][x] = (float) elevation;
			}
		}

		return covFactory.create("coverage", data, env);
	}

	@Test
	public void testViewshed() {

		GridCoverage2D gc = createCone();
		ViewshedExampleProcess proc = new ViewshedExampleProcess();
		GridCoverage2D result = proc.execute(gc, Double.valueOf(1.75), Double.valueOf(1000),
				new Coordinate(1, 1), null);
		Raster data = result.view(ViewType.NATIVE).getRenderedImage().getData();
		assertTrue(data.getSampleDouble(10, 90, 0) > 0);
		assertTrue(Double.isNaN(data.getSampleDouble(3, 3, 0)));

	}

}
