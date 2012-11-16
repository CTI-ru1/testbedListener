package eu.uberdust.testbedlistener.test;


import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.log4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Random;

/**
 * Unit test for simple App.
 */
public class CoapRequestTest
        extends TestCase {
    private static final Logger LOGGER = Logger.getLogger(CoapRequestTest.class);

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public CoapRequestTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(CoapRequestTest.class);
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp() {
        Random mid = new Random();
        URI uri = null;
        try {
            uri = new URI(new StringBuilder().append("/").append("pir").toString());
        } catch (URISyntaxException e) {
            LOGGER.error(e.getLocalizedMessage(), e);
        }
        final Request request = new Request(CodeRegistry.METHOD_GET, false);
        request.setMID(mid.nextInt() % 65535);
        request.setURI(uri);
//            List<Option> uriPath = Option.split(OptionNumberRegistry.URI_PATH, uri.getPath(), "/");
//            request.setOptions(OptionNumberRegistry.URI_PATH, uriPath);
//            request.setOption(new Option(0, OptionNumberRegistry.OBSERVE));
//            request.setToken(TokenManager.getInstance().acquireToken());

        sendRequest(request.toByteArray(), "9a8");

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

