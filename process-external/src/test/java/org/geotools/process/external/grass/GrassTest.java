package org.geotools.process.external.grass;

import java.util.HashMap;
import java.util.Map;

import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.Process;
import org.geotools.referencing.crs.DefaultGeographicCRS;

public class GrassTest {

	private static final GridCoverageFactory covFactory = CoverageFactoryFinder
			.getGridCoverageFactory(null);

	public static void main(String[] args) {

		GrassProcessFactory fact = new GrassProcessFactory();
		NameImpl name = new NameImpl("grass", "r.fillnulls");
		Process proc = fact.create(name);
		GridCoverage2D gc = createFlat();
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("input", gc);
		Map<String, Object> result = proc.execute(map, null);

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
