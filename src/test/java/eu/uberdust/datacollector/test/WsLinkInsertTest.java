package eu.uberdust.datacollector.test;

import eu.uberdust.communication.protobuf.Message;
import eu.uberdust.communication.websocket.readings.WSReadingsClient;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: amaxilatis
 * Date: 12/7/11
 * Time: 1:24 PM
 */
public class WsLinkInsertTest extends TestCase {

    /**
     * LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(WsLinkInsertTest.class);
    /**
     * WebSocket address.
     */
    private static final String WS_URL = "ws://uberdust.cti.gr:80/insertreading.ws";

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public WsLinkInsertTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(WsLinkInsertTest.class);
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp() {
        BasicConfigurator.configure();
        LOGGER.setLevel(Level.ALL);

        Message.LinkReadings.Reading reading = Message.LinkReadings.Reading.newBuilder()
                .setSource("urn:wisebed:ctitestbed:0x1ccd")
                .setTarget("urn:wisebed:ctitestbed:0x842f")
                .setCapability("command")
                .setTimestamp(System.currentTimeMillis())
                .setDoubleReading(141).build();

        Message.LinkReadings readings = Message.LinkReadings.newBuilder().addReading(reading).build();
        /**
         * WebSocket Call
         */


        // insert node reading using WebSockets
        LOGGER.info("Calling WebSocket at (" + WS_URL + ") connecting");
        try {

            WSReadingsClient.getInstance().setServerUrl(WS_URL);
            LOGGER.info("Calling sendNodeReading(nodeReading1)");
            WSReadingsClient.getInstance().sendLinkReading(readings);
//            LOGGER.info("Rest Equivalent " + linkReading.toRestString());
            LOGGER.info("Test Finished");

            assertTrue(true);
        } catch (Exception e) {
            assertFalse(true);
            e.printStackTrace();
        }

    }
}

