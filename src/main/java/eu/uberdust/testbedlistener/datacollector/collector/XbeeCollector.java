package eu.uberdust.testbedlistener.datacollector.collector;

import com.rapplogic.xbee.api.wpan.RxResponse16;
import eu.mksense.MessageListener;
import eu.mksense.XBeeRadio;
import eu.uberdust.testbedlistener.datacollector.parsers.XbeeMessageParser;
import eu.uberdust.testbedlistener.util.PropertyReader;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Opens a connection to the XBee and receives messages from all nodes to collect data.
 */
public class XbeeCollector extends AbstractCollector implements MessageListener {

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
//
//    public static void main(String[] args) {
//        PropertyReader.getInstance().setFile("listener.properties");
//        final String xbeePort = PropertyReader.getInstance().getProperties().getProperty("xbee.port");
//        final Integer rate =
//                Integer.valueOf(PropertyReader.getInstance().getProperties().getProperty("xbee.baudrate"));
//        try {
//            XBeeRadio.getInstance().open(xbeePort, rate);
//        } catch (final Exception e) {
//            LOGGER.error(e);
//            return;
//        }
//
//        XBeeAddress16 address
//                = new XBeeAddress16();
//        address.setLsb(0xb0);
//        address.setMsb(0x02);
//
//        int[] payload = new int[3];
//        payload[0] = 1;
//        payload[1] = 1;
//        payload[2] = 1;
//        int[] payload1 = new int[3];
//        payload[0] = 1;
//        payload[1] = 2;
//        payload[2] = 1;
//        while (true) {
//            try {
//                XBeeRadio.getInstance().send(address, 112, payload);
//                LOGGER.info("sending1");
//            } catch (Exception e) {
//                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//            }
//            try {
//                Thread.sleep(500);
//            } catch (InterruptedException e) {
//                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//            }
//            try {
//                XBeeRadio.getInstance().send(address, 112, payload1);
//                LOGGER.info("sending2");
//            } catch (Exception e) {
//                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//            }
//            try {
//                Thread.sleep(60000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//            }
//        }
////        new XbeeCollector();
//
//    }
}

