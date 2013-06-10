package eu.uberdust.testbedlistener.datacollector.collector;

import com.rapplogic.xbee.api.wpan.RxResponse16;
import eu.mksense.MessageListener;
import eu.mksense.XBeeRadio;
import eu.uberdust.communication.UberdustClient;
import eu.uberdust.testbedlistener.coap.CoapServer;
import eu.uberdust.testbedlistener.datacollector.parsers.CoapMessageParser;
import eu.uberdust.testbedlistener.datacollector.parsers.XbeeMessageParser;
import eu.uberdust.testbedlistener.util.Converter;
import eu.uberdust.testbedlistener.util.HereIamMessage;
import eu.uberdust.testbedlistener.util.PropertyReader;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.json.JSONException;
import org.simpleframework.xml.convert.Convert;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Opens a connection to the XBee and receives messages from all nodes to collect data.
 */
public class XbeeCollector extends AbstractCollector implements MessageListener, Runnable {

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

        testbedPrefix = null;
        capabilityPrefix = null;
        try {
            testbedPrefix = UberdustClient.getInstance().getUrnPrefix(testbedId);
            capabilityPrefix = UberdustClient.getInstance().getUrnCapabilityPrefix(testbedId);
        } catch (JSONException e) {
            LOGGER.error(e, e);
        }

        LOGGER.info(testbedPrefix);
        testbedId = Integer.parseInt(PropertyReader.getInstance().getProperties().getProperty("wisedb.testbedid"));
        LOGGER.info(testbedId);

        executorService = Executors.newCachedThreadPool();
    }

    @Override
    public void receive(final RxResponse16 rxResponse16) {

        String macAddress = "0x"
                + Integer.toHexString(rxResponse16.getRemoteAddress().getMsb());
        if (Integer.toHexString(rxResponse16.getRemoteAddress().getLsb()).length() == 1) {
            macAddress += "0";
        }
        macAddress += Integer.toHexString(rxResponse16.getRemoteAddress().getLsb());

        int[] data = rxResponse16.getData();
        byte byteData[] = new byte[data.length + 2];
        byteData[0] = (byte) (rxResponse16.getRemoteAddress().getMsb());
        byteData[1] = (byte) (rxResponse16.getRemoteAddress().getLsb());
        for (int i = 0; i < data.length; i++) {
            byteData[i + 2] = (byte) data[i];
        }
        HereIamMessage mess = new HereIamMessage(byteData);
        if (mess.isValid()) {
            byte finalData[] = new byte[byteData.length + 2];
            finalData[0] = 0x69;
            finalData[1] = 0x69;
            System.arraycopy(byteData, 0, finalData, 2, byteData.length);
            executorService.submit(new CoapMessageParser(macAddress, finalData,testbedPrefix,capabilityPrefix));
        } else {
            byte finalData[] = new byte[data.length + 1];
            finalData[0] = 0x69;
            finalData[1] = 0x69;
            System.arraycopy(byteData, 3, finalData, 2, data.length - 1);
            executorService.submit(new CoapMessageParser(macAddress, finalData,testbedPrefix,capabilityPrefix));
        }
    }

    @Override
    public void run() {
        XBeeRadio.getInstance().addMessageListener(112, this);
    }
}

