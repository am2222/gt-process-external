package org.geotools.process.external.grass;

import static org.junit.Assert.assertEquals;

import java.awt.image.Raster;
import java.util.HashMap;
import java.util.Map;

import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.ViewType;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.Process;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;

public class GrassRasterTest {

	private static final GridCoverageFactory covFactory = CoverageFactoryFinder
			.getGridCoverageFactory(null);

	@Test
	public void testSlopeProcess() {

		GrassProcessFactory fact = new GrassProcessFactory();
		NameImpl name = new NameImpl("grass", "r.slope.aspect");
		Process proc = fact.create(name);
		GridCoverage2D gc = createFlat();
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("input", gc);
		Map<String, Object> result = proc.execute(map, null);
		GridCoverage2D slope = (GridCoverage2D) result.get("slope");
		Raster data = slope.view(ViewType.NATIVE).getRenderedImage().getData();
		assertEquals(data.getSampleDouble(1, 1, 0), 0d, 1e-6);

	}

	private static GridCoverage2D createFlat() {
		int SIZE = 100;
		ReferencedEnvelope env;
		env = new ReferencedEnvelope(0, SIZE, 0, SIZE,
				DefaultGeographicCRS.WGS84);
		float[][] data = new float[SIZE][SIZE];
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < SIZE; x++) {
				data[x][y] = 5;
			}
		}

		return covFactory.create("coverage", data, env);
	}
}
