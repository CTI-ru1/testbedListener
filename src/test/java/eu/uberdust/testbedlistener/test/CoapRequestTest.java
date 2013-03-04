package eu.uberdust.testbedlistener.test;


import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import eu.uberdust.testbedlistener.CoapHelper;
import eu.uberdust.testbedlistener.coap.CoapServer;
import eu.uberdust.testbedlistener.util.PropertyReader;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.log4j.Logger;

import java.util.Arrays;

/**
 * Unit test for simple App.
 */
public class CoapRequestTest
        extends TestCase {
    private static final Logger LOGGER = Logger.getLogger(CoapRequestTest.class);
    private static final String MAC = "123";

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public CoapRequestTest(String testName) {
        super(testName);
        PropertyReader.getInstance().setFile("listener.properties");
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(CoapRequestTest.class);
    }

    /**
     * Test Two requests have differenet MIDs.
     */
    public void testNotEqualMIDs() {
        Request request1 = CoapHelper.getWellKnown(MAC);
        Request request2 = CoapHelper.getWellKnown(MAC);
        assertNotSame(request1.getMID(), request2.getMID());
    }

    /**
     * Test the Request contains well-known/core.
     */
    public void testContainsWellKnownCore() {
        Request request1 = CoapHelper.getWellKnown(MAC);
        assertEquals("/.well-known/core", request1.getUriPath());
    }

    /**
     * Test the Request contains well-known/core.
     */
    public void testPrintWellKnownCore() {
        Request request = new Request(CodeRegistry.METHOD_GET, false);
        request.setURI("/.well-known/core");
        request.setMID(1234);
        request.prettyPrint();
        System.out.println(Arrays.toString(request.toByteArray()));
    }


    /**
     * Test the Request contains the MAC.
     */
    public void testContainsURIhost() {

        Request request1 = CoapHelper.getWellKnown(MAC);
        assertTrue(request1.hasOption(OptionNumberRegistry.URI_HOST));
        assertEquals(MAC, request1.getFirstOption(OptionNumberRegistry.URI_HOST).getStringValue());
    }
}

