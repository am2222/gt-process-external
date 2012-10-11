package org.geotools.process.external.saga;

import static org.junit.Assert.assertTrue;

import java.awt.geom.Point2D;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.KVP;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequenceFactory;

public class SagaInterpolationTest {

    @Test
    public void testSimpleSurface() {

        ReferencedEnvelope bounds = new ReferencedEnvelope(0, 30, 0, 30, DefaultGeographicCRS.WGS84);
        Coordinate[] data = new Coordinate[] { 
                new Coordinate(10, 10, 100),
                new Coordinate(10, 20, 20), 
                new Coordinate(20, 10, 0), 
                new Coordinate(20, 20, 80) };
        SimpleFeatureCollection fc = createPoints(data, bounds);

        ProgressListener monitor = null;
        
        
        SagaProcessFactory fact = new SagaProcessFactory();
        SagaProcess proc = fact.create(new NameImpl("saga","inversedistanceweighted"));
        GridCoverage2D gc = (GridCoverage2D)proc.execute(
        		new KVP("shapes", fc, "field", "value", "extent", bounds, "user_size", Double.valueOf(1)), 
        		monitor).get("user_grid");
        
        double ERROR_TOL = 10;
        
        for (Coordinate p : data) {
            float covval = coverageValue(gc, p.x, p.y);
            double error = Math.abs(p.z - covval);
            assertTrue(error < ERROR_TOL);
        }

    }

    private float coverageValue(GridCoverage2D cov, double x, double y)
    {
        float[] covVal = new float[1];
        Point2D worldPos = new Point2D.Double(x, y);
        cov.evaluate(worldPos, covVal);
        return covVal[0];
    }
    
    private SimpleFeatureCollection createPoints(Coordinate[] pts, ReferencedEnvelope bounds)
    {

        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName("obsType");
        tb.setCRS(bounds.getCoordinateReferenceSystem());
        tb.add("shape", MultiPoint.class);
        tb.add("value", Double.class);

        SimpleFeatureType type = tb.buildFeatureType();
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(type);
        SimpleFeatureCollection fc = FeatureCollections.newCollection();

        GeometryFactory factory = new GeometryFactory(new PackedCoordinateSequenceFactory());

        for (Coordinate p : pts) {
            Geometry point = factory.createPoint(p);
            fb.add(point);
            fb.add(p.z);
            fc.add(fb.buildFeature(null));
        }

        return fc;
    }


}
