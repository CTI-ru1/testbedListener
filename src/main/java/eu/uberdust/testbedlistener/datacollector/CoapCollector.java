package eu.uberdust.testbedlistener.datacollector;

import com.rapplogic.xbee.api.wpan.RxResponse16;
import eu.mksense.MessageListener;
import eu.mksense.XBeeRadio;
import eu.uberdust.testbedlistener.datacollector.parsers.CoapMessageParser;
import eu.uberdust.testbedlistener.util.PropertyReader;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Opens a connection to the XBee and receives messages from all nodes to collect data.
 */
public class CoapCollector implements MessageListener {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(CoapCollector.class);

    /**
     * WebSocket address prefix.
     */
    private static final String WS_URL_PREFIX = "ws://";

    /**
     * WebSocket address suffix.
     */
    private static final String WS_URL_SUFFIX = "insertreading.ws";

    /**
     * executors for handling incoming messages.
     */
    private final transient ExecutorService executorService;

    /**
     * Default Constructor.
     */
    public CoapCollector() {
        PropertyConfigurator.configure(Thread.currentThread().getContextClassLoader().getResource("log4j.properties"));

        final StringBuilder wsUrlBuilder = new StringBuilder(WS_URL_PREFIX);
        wsUrlBuilder.append(PropertyReader.getInstance().getProperties().getProperty("uberdust.server"));
        wsUrlBuilder.append(":");
        wsUrlBuilder.append(PropertyReader.getInstance().getProperties().getProperty("uberdust.port"));
        wsUrlBuilder.append(PropertyReader.getInstance().getProperties().getProperty("uberdust.basepath"));
        wsUrlBuilder.append(WS_URL_SUFFIX);

        final String ws_url = wsUrlBuilder.toString();

        executorService = Executors.newCachedThreadPool();
        XBeeRadio.getInstance().addMessageListener(112, this);
    }

    @Override
    public void receive(final RxResponse16 rxResponse16) {
        executorService.submit(new CoapMessageParser(rxResponse16.getRemoteAddress(), rxResponse16.getData()));
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
        new CoapCollector();
    }
}

