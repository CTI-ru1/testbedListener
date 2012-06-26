package eu.uberdust;

import com.rapplogic.xbee.api.XBeeAddress16;
import eu.mksense.XBeeRadio;
import eu.uberdust.network.NetworkManager;
import eu.uberdust.testbedlistener.coap.CoapServer;
import eu.uberdust.testbedlistener.controller.TestbedController;
import eu.uberdust.testbedlistener.controller.XbeeController;
import eu.uberdust.testbedlistener.datacollector.CoapCollector;
import eu.uberdust.testbedlistener.datacollector.DataCollector;
import eu.uberdust.testbedlistener.datacollector.XbeeCollector;
import eu.uberdust.testbedlistener.nodeflasher.NodeFlasherController;
import eu.uberdust.testbedlistener.util.PropertyReader;

import java.io.File;

/**
 * The testbed Listener Connects to Uberdust and Testbed Runtime to forward command and readings Both Ways.
 */
public class TestbedListener {

    /**
     * Logger.
     */
    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(TestbedListener.class);

    /**
     * ENABLED backend identifier.
     */
    private static final String ENABLED = "1";

    /**
     * XBee backend identifier.
     */
    private static final String XBEE = "backend.xbee";

    /**
     * Testbed Runtime backend identifier.
     */
    private static final String TESTBED_RUNTIME = "backend.tr";

    /**
     * Coap backend identifier.
     */
    private static final String COAP = "backend.coap";

    /**
     * Starts the application.
     *
     * @param args not used.
     */
    public static void main(final String[] args) {
        final String server = PropertyReader.getInstance().getProperties().getProperty("uberdust.server");
        final String port = PropertyReader.getInstance().getProperties().getProperty("uberdust.port");
        final String testbedId = PropertyReader.getInstance().getProperties().getProperty("wisedb.testbedid");
        final String testbedBasePath = PropertyReader.getInstance().getProperties().getProperty("uberdust.basepath");
        final String usbPort = PropertyReader.getInstance().getProperties().getProperty("xbee.port");


        if (ENABLED.equals(PropertyReader.getInstance().getProperties().getProperty(COAP))) {
            CoapServer.getInstance();
        }

        NetworkManager.getInstance().start(server + ":" + port + testbedBasePath, Integer.parseInt(testbedId));

        if (ENABLED.equals(PropertyReader.getInstance().getProperties().getProperty(XBEE))
                || ENABLED.equals(PropertyReader.getInstance().getProperties().getProperty(COAP))) {

            final int xbeeMsb = Integer.valueOf(PropertyReader.getInstance().getProperties().getProperty("xbee.msb"), 16);
            final int xbeeLsb = Integer.valueOf(PropertyReader.getInstance().getProperties().getProperty("xbee.lsb"), 16);
            final Integer rate = Integer.valueOf(PropertyReader.getInstance().getProperties().getProperty("xbee.baudrate"));

            if (usbPort.contains("/")) {
                try {
                    XBeeRadio.getInstance().open(usbPort, rate);
                } catch (final Exception e) {
                    LOGGER.fatal(e);
                }
            } else {
                File devices = new File("/dev/");
                File[] files = devices.listFiles();
                boolean connected = false;
                for (File file : files) {
                    if (file.getName().contains("ttyUSB") || file.getName().contains("ttyACM")) {
                        final String xbeePort = file.getAbsolutePath();

                        try {
                            LOGGER.info("trying " + xbeePort);
                            XBeeAddress16 address = XBeeRadio.getInstance().checkXbeeAddress(xbeePort, rate);

                            //wait to unlock the xbee
                            Thread.sleep(1000);
                            LOGGER.info(address);
                            if ((address.getMsb() == xbeeMsb)
                                    && (address.getLsb() == xbeeLsb)) {
                                LOGGER.info("connected");
                                XBeeRadio.getInstance().open(xbeePort, rate);
                                connected = true;
                                break;
                            }


                        } catch (final Exception e) {
                            LOGGER.error(e);
                            e.printStackTrace();
                        }
                    }
                }
                if (!connected) {
                    LOGGER.error("Could not connect to xbee device!");
                }
            }
        }

        LOGGER.info("Listening on channel :" + XBeeRadio.getInstance().getChannel());

        //Awaits for commands from Uberdust.
        if (ENABLED.equals(PropertyReader.getInstance().getProperties().get("use.controller"))) {
            if (ENABLED.equals(PropertyReader.getInstance().getProperties().getProperty(TESTBED_RUNTIME))) {

                LOGGER.info("starting TestbedController");
                NetworkManager.getInstance().addObserver(TestbedController.getInstance());
            }
            if (ENABLED.equals(PropertyReader.getInstance().getProperties().getProperty(XBEE))) {
                NetworkManager.getInstance().addObserver(XbeeController.getInstance());
            }
            if (ENABLED.equals(PropertyReader.getInstance().getProperties().getProperty(COAP))) {
//                    NetworkManager.getInstance().addObserver(CoapController.getInstance());
            }
        }


        //Flashes the testbed every some minutes to ensure that the testbed collector application is used.
        if (ENABLED.equals(PropertyReader.getInstance().getProperties().get("use.nodeflasher"))) {
            if (ENABLED.equals(PropertyReader.getInstance().getProperties().getProperty(TESTBED_RUNTIME))) {
                LOGGER.info("starting NodeFlasherController");
                new NodeFlasherController();
            }
        }

        //Listens to new Messages from the TestbedRuntime
        if (ENABLED.equals(PropertyReader.getInstance().getProperties().get("use.datacollector"))) {
            if (ENABLED.equals(PropertyReader.getInstance().getProperties().getProperty(TESTBED_RUNTIME))) {
                LOGGER.info("starting DataCollector");
                final Thread dataCollector = new Thread(new DataCollector());
                dataCollector.start();
            }
            if (ENABLED.equals(PropertyReader.getInstance().getProperties().getProperty(XBEE))) {
                LOGGER.info("Starting XbeeDataCollector");
                new XbeeCollector();
            }
            if (ENABLED.equals(PropertyReader.getInstance().getProperties().getProperty(COAP))) {
                new CoapCollector();
            }
        }
        LOGGER.info("up and running");
    }
}
