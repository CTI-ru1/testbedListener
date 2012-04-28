package eu.uberdust;

import eu.mksense.XBeeRadio;
import eu.uberdust.controller.TestbedController;
import eu.uberdust.controller.XbeeController;
import eu.uberdust.datacollector.DataCollector;
import eu.uberdust.datacollector.XbeeCollector;
import eu.uberdust.network.NetworkManager;
import eu.uberdust.nodeflasher.NodeFlasherController;
import eu.uberdust.testbedlistener.util.PropertyReader;

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

        LOGGER.info("Backend Type: " + backendType);
        NetworkManager.getInstance().start(server + ":" + port, Integer.parseInt(testbedId));


        if (backendType.equals(XBEE)) {
            final String xbeePort = PropertyReader.getInstance().getProperties().getProperty("xbee.port");
            final Integer rate =
                    Integer.valueOf(PropertyReader.getInstance().getProperties().getProperty("xbee.baudrate"));
            try {
                XBeeRadio.getInstance().open(xbeePort, rate);
            } catch (final Exception e) {
                LOGGER.error(e);
                return;
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
