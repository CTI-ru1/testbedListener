package eu.uberdust.testbedlistener.test;


import eu.uberdust.testbedlistener.controller.util.RoutingTable;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.log4j.Logger;

/**
 * Unit test for simple App.
 */
public class RoutingTablesTest
        extends TestCase {
    private static final Logger LOGGER = Logger.getLogger(RoutingTablesTest.class);

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public RoutingTablesTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(RoutingTablesTest.class);
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp() {


        RoutingTable.buildRoutingTables("test-routing.xml");

        LOGGER.info(RoutingTable.getRoute("node1", null));

        LOGGER.info(RoutingTable.getRoute("node2", "urn:node:capability:acapability"));

        LOGGER.info(RoutingTable.getRoute("node4", "urn:node:capability:capability1"));
        LOGGER.info(RoutingTable.getRoute("node4", "urn:node:capability:othercapability1"));

        LOGGER.info(RoutingTable.getRoute("node5", "urn:node:capability:bcapability"));

        LOGGER.info(RoutingTable.getRoute("node7", null));
        assertTrue(true);

    }
}

