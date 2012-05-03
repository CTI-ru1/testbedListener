package eu.uberdust;

import eu.mksense.XBeeRadio;
import eu.uberdust.controller.TestbedController;
import eu.uberdust.controller.XbeeController;
import eu.uberdust.datacollector.DataCollector;
import eu.uberdust.datacollector.XbeeCollector;
import eu.uberdust.network.NetworkManager;
import eu.uberdust.nodeflasher.NodeFlasherController;
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
     * XBee backend identifier.
     */
    private static final String XBEE = "xbee";

    /**
     * Testbed Runtime backend identifier.
     */
    private static final String TESTBED_RUNTIME = "tr";

    /**
     * Starts the application.
     *
     * @param args not used.
     */
    public static void main(final String[] args) {
        final String backendType = PropertyReader.getInstance().getProperties().getProperty("backend.type");
        final String server = PropertyReader.getInstance().getProperties().getProperty("uberdust.server");
        final String port = PropertyReader.getInstance().getProperties().getProperty("uberdust.port");
        final String testbedId = PropertyReader.getInstance().getProperties().getProperty("wisedb.testbedid");
        final String testbedBasePath = PropertyReader.getInstance().getProperties().getProperty("uberdust.basepath");

        LOGGER.info("Backend Type: " + backendType);

        NetworkManager.getInstance().start(server + ":" + port + testbedBasePath, Integer.parseInt(testbedId));

        if (backendType.equals(XBEE)) {

            final int xbeeMsb = Integer.valueOf(PropertyReader.getInstance().getProperties().getProperty("xbee.msb"), 16);
            final int xbeeLsb = Integer.valueOf(PropertyReader.getInstance().getProperties().getProperty("xbee.lsb"), 16);
            final Integer rate = Integer.valueOf(PropertyReader.getInstance().getProperties().getProperty("xbee.baudrate"));

            File devices = new File("/dev/");
            File[] files = devices.listFiles();
            boolean connected = false;
            for (File file : files) {
                if (file.getName().contains("ttyUSB") || file.getName().contains("ttyACM")) {
                    final String xbeePort = file.getAbsolutePath();


                    try {
                        LOGGER.info("trying " + xbeePort);
                        XBeeRadio.getInstance().open(xbeePort, rate);
                        LOGGER.info(XBeeRadio.getInstance().getMyXbeeAddress());
                        if ((XBeeRadio.getInstance().getMyXbeeAddress().getMsb() == xbeeMsb)
                                && (XBeeRadio.getInstance().getMyXbeeAddress().getLsb() == xbeeLsb)) {
                            LOGGER.info("connected");
                            connected = true;
                            break;
                        }

                    } catch (final Exception e) {
                        LOGGER.error(e);

                    }
                }
            }
            if (!connected) {
                LOGGER.error("Could not connect to xbee device!");
            }
        }


        //Awaits for commands from Uberdust.
        if (PropertyReader.getInstance().getProperties().get("use.controller").equals("1")) {
            if (backendType.equals(TESTBED_RUNTIME)) {

                LOGGER.info("starting TestbedController");
                NetworkManager.getInstance().addObserver(TestbedController.getInstance());

            } else if (backendType.equals(XBEE)) {
                NetworkManager.getInstance().addObserver(XbeeController.getInstance());
            }
        }

        if (backendType.equals(TESTBED_RUNTIME)) {
            //Flashes the testbed every some minutes to ensure that the testbed collector application is used.
            if (PropertyReader.getInstance().getProperties().get("use.nodeflasher").equals("1")) {
                LOGGER.info("starting NodeFlasherController");
                new NodeFlasherController();
            }
        }

        //Listens to new Messages from the TestbedRuntime
        if (PropertyReader.getInstance().getProperties().get("use.datacollector").equals("1")) {
            if (backendType.equals(TESTBED_RUNTIME)) {
                LOGGER.info("starting DataCollector");
                final Thread dataCollector = new Thread(new DataCollector());
                dataCollector.start();
            } else if (backendType.equals(XBEE)) {
                LOGGER.info("Starting XbeeDataCollector");
                new XbeeCollector();
            }
        }
        LOGGER.info("up and running");
    }
}
