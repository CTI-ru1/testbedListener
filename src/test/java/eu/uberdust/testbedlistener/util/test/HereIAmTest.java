package eu.uberdust.testbedlistener.util.test;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.log4j.Logger;

/**
 * Unit test for simple App.
 */
public class HereIAmTest
        extends TestCase {
    private static final Logger LOGGER = Logger.getLogger(HereIAmTest.class);

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public HereIAmTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(HereIAmTest.class);
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp() {

//        byte[] payload = new byte[3];
//        payload[1] = (byte) 0x99;
//        payload[2] = 0x79;
//        byte macMSB = payload[1];
//        byte macLSB = payload[2];
//        final String macStr = Converter.byteToString(macMSB) + Converter.byteToString(macLSB);
//        LOGGER.info(macStr);
//
//        Request request = CoapHelper.getWellKnown(macStr);
//        request.prettyPrint();
//        request = CoapHelper.getWellKnown(macStr, 0);
//        request.prettyPrint();
//        request = CoapHelper.getWellKnown(macStr, 1);
//        request.prettyPrint();
//        request = CoapHelper.getWellKnown(macStr, 2);
//        request.prettyPrint();


//            try {
//                int[] zpayloap = new int[1 + request.toByteArray().length];
//                zpayloap[0] = 51;
//                System.arraycopy(Converter.getInstance().ByteToInt(request.toByteArray()), 0, zpayloap, 1, Converter.getInstance().ByteToInt(request.toByteArray()).length);
//                LOGGER.info(Converter.getInstance().payloadToString(zpayloap));
//                XBeeRadio.getInstance().send(remoteAddress, 112, zpayloap);
//            } catch (Exception e) {     //NOPMD
//                LOGGER.error(e.getMessage(), e);
//    }

    }


}

