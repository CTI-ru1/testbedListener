package eu.uberdust;

import eu.uberdust.controller.TestbedController;
import eu.uberdust.datacollector.DataCollector;
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
     * Starts the application.
     *
     * @param args not used.
     */
    public static void main(final String[] args) {

        final String server = PropertyReader.getInstance().getProperties().getProperty("uberdust.server");
        final String port = PropertyReader.getInstance().getProperties().getProperty("uberdust.port");
        final String testbedId = PropertyReader.getInstance().getProperties().getProperty("wisedb.testbedid");


        NetworkManager.getInstance().start(server + ":" + port, Integer.parseInt(testbedId));

        //Awaits for commands from Uberdust.
        if (PropertyReader.getInstance().getProperties().get("use.controller").equals("1")) {
            LOGGER.info("starting TestbedController");
            NetworkManager.getInstance().addObserver(TestbedController.getInstance());
        }

        //Flashes the testbed every some minutes to ensure that the testbed collector application is used.
        if (PropertyReader.getInstance().getProperties().get("use.nodeflasher").equals("1")) {
            LOGGER.info("starting NodeFlasherController");
            new NodeFlasherController();
        }

        //Listens to new Messages from the TestbedRuntime
        if (PropertyReader.getInstance().getProperties().get("use.datacollector").equals("1")) {
            LOGGER.info("starting DataCollector");
            final Thread dataCollector = new Thread(new DataCollector());
            dataCollector.run();
        }
        LOGGER.info("up and running");
    }
}
