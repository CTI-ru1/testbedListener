package eu.uberdust.datacollector.test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Unit test for simple App.
 */
public class ParsingTest
        extends TestCase {
    private static final Logger LOGGER = Logger.getLogger(ParsingTest.class);

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public ParsingTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(ParsingTest.class);
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp() {

        BasicConfigurator.configure();
        LOGGER.setLevel(Level.DEBUG);

        assertTrue(true);
        try {
            final String payloadString = "0x7f|0x69|0x70|0x1|0x1|0x1|0x1|0x3|0x2|0x4|0x4|0x6|";
            LOGGER.debug(payloadString);
            final String parsedString = payloadString.replaceAll("0x", "").replace('|', ',');
            LOGGER.debug(parsedString);
        } catch (Exception e) {
            assertTrue(false);
        }

    }
}

