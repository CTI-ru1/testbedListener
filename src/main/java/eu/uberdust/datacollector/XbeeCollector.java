package eu.uberdust.datacollector;

import com.rapplogic.xbee.api.wpan.RxResponse16;
import eu.mksense.MessageListener;
import eu.mksense.XBeeRadio;
import eu.uberdust.datacollector.parsers.XbeeMessageParser;
import eu.uberdust.testbedlistener.util.PropertyReader;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Opens a connection to the XBee and receives messages from all nodes to collect data.
 */
public class XbeeCollector implements MessageListener {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(DataCollector.class);

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

    private final HashMap<String, String> capabilities = new HashMap<String, String>();

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
        LOGGER.info(testbedPrefix);
        testbedId = Integer.parseInt(PropertyReader.getInstance().getProperties().getProperty("wisedb.testbedid"));
        LOGGER.info(testbedId);

        final String[] sensorCapabilities =
                PropertyReader.getInstance().getProperties().getProperty("sensor.capabilities").split(";");
        final String[] sensorCapabilitiesIndexes =
                PropertyReader.getInstance().getProperties().getProperty("sensor.capabilities_index").split(";");

        for (int i = 0; i < sensorCapabilities.length; i++) {
                        capabilities.put(sensorCapabilitiesIndexes[i], sensorCapabilities[i]);
        }
        LOGGER.info(capabilities.toString());


        executorService = Executors.newCachedThreadPool();
        XBeeRadio.getInstance().addMessageListener(112, this);
    }

    @Override
    public void receive(final RxResponse16 rxResponse16) {
        executorService.submit(new XbeeMessageParser(rxResponse16.getRemoteAddress(), rxResponse16.getData()
                , testbedPrefix, testbedId, capabilities));
    }
}

