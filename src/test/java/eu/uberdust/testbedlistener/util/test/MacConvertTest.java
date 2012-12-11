package eu.uberdust.testbedlistener.util.test;


import eu.uberdust.testbedlistener.util.Converter;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.log4j.Logger;

/**
 * Unit test for simple App.
 */
public class MacConvertTest
        extends TestCase {
    private static final Logger LOGGER = Logger.getLogger(MacConvertTest.class);

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public MacConvertTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(MacConvertTest.class);
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp() {

        String destination = "181";
        byte[] destinationBytes = new byte[2];

        final byte[] macBytes = Converter.getInstance().addressToByte(destination);

        LOGGER.info(Byte.toString(macBytes[0]));
        LOGGER.info(Byte.toString(macBytes[1]));
    }
}

