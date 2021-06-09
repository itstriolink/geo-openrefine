package com.google.refine.geojson;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;

import com.google.refine.geojson.util.Constants;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

public class RegExTest {
    @Test
    public void testLatitudeValueTrue() {
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue("47.3862091".matches(Constants.RegEx.latitudeValue));
        softAssert.assertTrue("50".matches(Constants.RegEx.latitudeValue));
        softAssert.assertTrue("50.927".matches(Constants.RegEx.latitudeValue));
        softAssert.assertAll();
    }

    @Test
    public void testLongitudeValueTrue() { assertTrue("8.2403654".matches(Constants.RegEx.longitudeValue)); }

    @Test
    public void testLatitudeValueFalse() { assertFalse("foo".matches(Constants.RegEx.latitudeValue)); }

    @Test
    public void testLongitudeValueFalse() { assertFalse("bar".matches(Constants.RegEx.longitudeValue)); }
}
