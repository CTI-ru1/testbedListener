package eu.uberdust.testbedlistener;

import eu.uberdust.testbedlistener.factory.CoapListenerFactory;
import eu.uberdust.testbedlistener.factory.CommandLineListenerFactory;
import eu.uberdust.testbedlistener.factory.TestbedRuntimeListenerFactory;
import eu.uberdust.testbedlistener.factory.XbeeListenerFactory;
import eu.uberdust.testbedlistener.util.PropertyReader;

/**
 * Main class for the Testbedlistener application.
 * Starts a new Listener.
 *
 * @see eu.uberdust.testbedlistener.factory.AbstractListenerFactory
 */
public class Main {
    /**
     * The porperty file to use.
     */
    private static final String PROPERTY_FILE = "listener.properties";

    /**
     * Starts the application.
     * Launches different listeners based on the type property provided.
     *
     * @param args not used.
     * @see Main#PROPERTY_FILE
     */
    public static void main(final String[] args) {
        PropertyReader.getInstance().setFile(PROPERTY_FILE);

        final String type = PropertyReader.getInstance().getProperties().getProperty("type");

        if (type.equals("tr")) {
            TestbedRuntimeListenerFactory tr = new TestbedRuntimeListenerFactory();
            tr.run();
        } else if (type.equals("xbee")) {
            XbeeListenerFactory xbee = new XbeeListenerFactory();
            xbee.run();
        } else if (type.equals("coap")) {
            CoapListenerFactory coap = new CoapListenerFactory();
            coap.run();
        } else if (type.equals("cmd")) {
            CommandLineListenerFactory cmd = new CommandLineListenerFactory();
            cmd.run();
        }
    }
}
