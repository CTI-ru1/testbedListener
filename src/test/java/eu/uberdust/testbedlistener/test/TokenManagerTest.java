package eu.uberdust.testbedlistener.test;

import ch.ethz.inf.vs.californium.coap.Message;
import eu.uberdust.testbedlistener.util.TokenManager;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: amaxilatis
 * Date: 11/27/12
 * Time: 1:04 PM
 */
public class TokenManagerTest extends TestCase {
    private static final Logger LOGGER = Logger.getLogger(TokenManagerTest.class);

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public TokenManagerTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(TokenManagerTest.class);
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp() {
        int count = 200000;
        generateTokens(count);
        LOGGER.info("Tested " + count);

    }

    private void generateTokens(int count) {
        Map<String, Integer> tokens = new HashMap<String, Integer>();
        for (int i = 0; i < count; i++) {
            byte[] token = TokenManager.getInstance().acquireToken();
            Message mess = new Message();
            mess.setToken(token);
            if (tokens.containsKey(mess.getTokenString())) {
                LOGGER.error("TOKEN DUPLICATE " + i);
                assertFalse(true);
            }
            tokens.put(mess.getTokenString(), 1);
            if (i % 1000000 == 0) {
                LOGGER.info("checked " + i);
            }
        }
    }


}
