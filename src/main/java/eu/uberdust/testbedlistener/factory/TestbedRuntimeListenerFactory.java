package eu.uberdust.testbedlistener.factory;

import eu.uberdust.network.NetworkManager;
import eu.uberdust.testbedlistener.controller.TestbedController;
import eu.uberdust.testbedlistener.datacollector.TestbedRuntimeCollector;
import eu.uberdust.testbedlistener.util.PropertyReader;

/**
 * The testbed Listener Connects to Uberdust and Testbed Runtime to forward command and readings Both Ways.
 */
public class TestbedRuntimeListenerFactory extends AbstractListenerFactory {

    /**
     * Logger.
     */
    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(TestbedRuntimeListenerFactory.class);

    /**
     * ENABLED backend identifier.
     */
    private static final String ENABLED = "1";

    /**
     * Starts the application.
     */
    @Override
    public void run() {

        final String server = PropertyReader.getInstance().getProperties().getProperty("uberdust.server");
        final String port = PropertyReader.getInstance().getProperties().getProperty("uberdust.port");
        final String testbedId = PropertyReader.getInstance().getProperties().getProperty("wisedb.testbedid");
        final String testbedBasePath = PropertyReader.getInstance().getProperties().getProperty("uberdust.basepath");


        if (PropertyReader.getInstance().getProperties().get("use.controller").equals("1") ||
                PropertyReader.getInstance().getProperties().get("use.datacollector").equals("1")) {
            LOGGER.info("Connecting Network Manager");
            NetworkManager.getInstance().start(server + ":" + port + testbedBasePath, Integer.parseInt(testbedId));
        }

        //Awaits for commands from Uberdust.
        if (ENABLED.equals(PropertyReader.getInstance().getProperties().get("use.controller"))) {
            NetworkManager.getInstance().addObserver(TestbedController.getInstance());
        }


        //Listens to new Messages from the TestbedRuntime
        if (ENABLED.equals(PropertyReader.getInstance().getProperties().get("use.datacollector"))) {
            LOGGER.info("Starting TestbedRuntimeCollector...");
            final Thread dataCollector = new Thread(new TestbedRuntimeCollector());
            dataCollector.start();
        }


        LOGGER.info("up and running");
    }

}
