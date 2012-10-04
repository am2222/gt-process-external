package org.geotools.process.external;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.external.saga.SagaProcessFactory;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;

public class ProcessGroupTest {

	private static final GridCoverageFactory covFactory = CoverageFactoryFinder
			.getGridCoverageFactory(null);

	@Test
	public void testProcessGroupWithSagaProcesses() {
		SagaProcessFactory fact = new SagaProcessFactory();

		GridCoverage2D gc = createFlat();

		ProcessGroup pg = new ProcessGroup();

		NameImpl name = new NameImpl("saga", "convergenceindex");
		ExternalProcess proc = fact.create(name);
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("elevation", gc);
		map.put("method", new Integer(0));
		map.put("neighbours", new Integer(0));
		pg.addProcess(proc);

		NameImpl name2 = new NameImpl("saga", "terrainruggednessindextri");
		ExternalProcess proc2 = fact.create(name2);
		HashMap<String, Object> map2 = new HashMap<String, Object>();
		map2.put("dem", gc);
		pg.addProcess(proc2);

		Map<String, Object> result = proc.execute(map, null);
		assertTrue(result.size() > 0);
		Map<String, Object> result2 = proc2.execute(map2, null);
		assertTrue(result2.size() > 0);

		pg.finish();

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