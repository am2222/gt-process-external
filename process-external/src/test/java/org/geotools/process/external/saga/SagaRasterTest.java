package org.geotools.process.external.saga;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.Process;
import org.geotools.process.ProcessFactory;
import org.geotools.process.Processors;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.opengis.feature.type.Name;

public class SagaRasterTest {

	private static final GridCoverageFactory covFactory = CoverageFactoryFinder
			.getGridCoverageFactory(null);
	private static final int COUNT = 276;
	SagaProcessFactory fact = new SagaProcessFactory();

	public void setUp() {

	}

	@Test
	public void testAlgorithmsCount() {
		assertEquals(fact.processes.size(), COUNT);
	}

	@Test
	public void testConvergenceIndex() {
		
		NameImpl name = new NameImpl("saga", "convergenceindex");
		Process proc = fact.create(name);
		GridCoverage2D gc = createFlat();
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("elevation", gc);
		map.put("method", new Integer(0));
		map.put("neighbours", new Integer(0));
		Map<String, Object> result = proc.execute(map, null);
		assertTrue(result.size() > 0);
	}
	
	@Test
	public void testHook(){
		Set<ProcessFactory> factories = Processors.getProcessFactories();
		for (ProcessFactory factory : factories){
			Set<Name> procs = factory.getNames();
			for (Name proc : procs){
				System.out.println(proc.toString());	
			}			
		}
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