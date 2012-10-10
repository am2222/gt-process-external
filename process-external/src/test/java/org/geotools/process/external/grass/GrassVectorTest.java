package org.geotools.process.external.grass;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.process.Process;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequenceFactory;

public class GrassVectorTest {

	@Test
	public void testConvexHullProcess() {

		GrassProcessFactory fact = new GrassProcessFactory();
		NameImpl name = new NameImpl("grass", "v.hull");
		Process proc = fact.create(name);
		FeatureCollection fc = getPoints();
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("input", fc);
		map.put("a", new Boolean(true));
		Map<String, Object> result = proc.execute(map, null);
		FeatureCollection fc2 = (FeatureCollection) result.get("output");
		SimpleFeature hull = (SimpleFeature) fc2.features().next();
		MultiPolygon polyg = (MultiPolygon) hull.getDefaultGeometry();
		assertEquals(polyg.getArea(), 1, 1e-6);

	}

	private SimpleFeatureCollection getPoints() {

		SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
		tb.setName("data");
		tb.setCRS(DefaultGeographicCRS.WGS84);
		tb.add("shape", Point.class);

		SimpleFeatureType type = tb.buildFeatureType();
		SimpleFeatureBuilder fb = new SimpleFeatureBuilder(type);
		SimpleFeatureCollection fc = FeatureCollections.newCollection();

		GeometryFactory factory = new GeometryFactory(
				new PackedCoordinateSequenceFactory());

		Coordinate[] pts = new Coordinate[] { new Coordinate(0, 0),
				new Coordinate(0, 1), new Coordinate(1, 1),
				new Coordinate(1, 0) };
		for (Coordinate p : pts) {
			Geometry point = factory.createPoint(p);
			fb.add(point);
			fc.add(fb.buildFeature(null));
		}

		return fc;
	}

}
