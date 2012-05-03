package eu.uberdust.datacollector;

import com.rapplogic.xbee.api.wpan.RxResponse16;
import eu.mksense.MessageListener;
import eu.mksense.XBeeRadio;
import eu.uberdust.datacollector.parsers.XbeeMessageParser;
import eu.uberdust.testbedlistener.util.PropertyReader;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Opens a connection to the XBee and receives messages from all nodes to collect data.
 */
public class XbeeCollector implements MessageListener {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(XbeeCollector.class);

    /**
     * WebSocket address prefix.
     */
    private static final String WS_URL_PREFIX = "ws://";

    /**
     * WebSocket address suffix.
     */
    private static final String WS_URL_SUFFIX = "insertreading.ws";
    /**
     * WebSocket address url.
     */
    private static String ws_url = "";

    /**
     * executors for handling incoming messages.
     */
    private final transient ExecutorService executorService;

    private String testbedPrefix;
    private int testbedId;
    private String capabilityPrefix;


    /**
     * Default Constructor.
     */
    public XbeeCollector() {
        PropertyConfigurator.configure(Thread.currentThread().getContextClassLoader().getResource("log4j.properties"));

        final StringBuilder wsUrlBuilder = new StringBuilder(WS_URL_PREFIX);
        wsUrlBuilder.append(PropertyReader.getInstance().getProperties().getProperty("uberdust.server"));
        wsUrlBuilder.append(":");
        wsUrlBuilder.append(PropertyReader.getInstance().getProperties().getProperty("uberdust.port"));
        wsUrlBuilder.append(PropertyReader.getInstance().getProperties().getProperty("uberdust.basepath"));
        wsUrlBuilder.append(WS_URL_SUFFIX);
        ws_url = wsUrlBuilder.toString();

        testbedPrefix = PropertyReader.getInstance().getProperties().getProperty("testbed.prefix");
        capabilityPrefix = PropertyReader.getInstance().getProperties().getProperty("testbed.capability.prefix");
        LOGGER.info(testbedPrefix);
        testbedId = Integer.parseInt(PropertyReader.getInstance().getProperties().getProperty("wisedb.testbedid"));
        LOGGER.info(testbedId);

        executorService = Executors.newCachedThreadPool();
        XBeeRadio.getInstance().addMessageListener(112, this);
    }

    @Override
    public void receive(final RxResponse16 rxResponse16) {
        executorService.submit(new XbeeMessageParser(rxResponse16.getRemoteAddress(), rxResponse16.getData()
                , testbedPrefix, testbedId, capabilityPrefix));
    }

    public static void main(String[] args) {
        final String xbeePort = PropertyReader.getInstance().getProperties().getProperty("xbee.port");
        final Integer rate =
                Integer.valueOf(PropertyReader.getInstance().getProperties().getProperty("xbee.baudrate"));
        try {
            XBeeRadio.getInstance().open(xbeePort, rate);
        } catch (final Exception e) {
            LOGGER.error(e);
            return;
        }
        new XbeeCollector();
    }
}

