package eu.uberdust.testbedlistener.test;


import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.Request;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.log4j.Logger;

/**
 * Unit test for simple App.
 */
public class CoapACKTest
        extends TestCase {
    private static final Logger LOGGER = Logger.getLogger(CoapACKTest.class);

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public CoapACKTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(CoapACKTest.class);
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp() {

        final Request request = new Request(CodeRegistry.METHOD_GET, false);
        Message ack = request.newReply(true);
        LOGGER.info(ack.isAcknowledgement());
        LOGGER.info(ack.getType());
    }


    public void sendRequest(final byte[] data, final String nodeUrn) {
        final int[] bytes = new int[data.length + 1];
        bytes[0] = 51;
        for (int i = 0; i < data.length; i++) {
            final short read = (short) ((short) data[i] & 0xff);
            bytes[i + 1] = read;
        }

        final StringBuilder messageBinary = new StringBuilder("Requesting[Bytes]:");
        for (int i = 0; i < data.length + 1; i++) {
            messageBinary.append(bytes[i]).append("|");
        }
        LOGGER.info(messageBinary.toString());

    }
}

