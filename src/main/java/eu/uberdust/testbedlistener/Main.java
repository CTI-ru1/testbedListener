package eu.uberdust.testbedlistener;

import eu.uberdust.testbedlistener.factory.*;
import eu.uberdust.testbedlistener.util.PropertyReader;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

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
//        BasicConfigurator.configure();
        PropertyConfigurator.configure("log4j.properties");

        final String type = PropertyReader.getInstance().getProperties().getProperty("type");

        if ("tr".equals(type)) {
            TestbedRuntimeListenerFactory tr = new TestbedRuntimeListenerFactory();
            tr.run();
        } else if ("xbee".equals(type)) {
            XbeeListenerFactory xbee = new XbeeListenerFactory();
            xbee.run();
        } else if ("coap".equals(type)) {
            COAPTRListener2Factory coap = new COAPTRListener2Factory();
            coap.run();
        } else if ("cmd".equals(type)) {
            CommandLineListenerFactory cmd = new CommandLineListenerFactory();
            cmd.run();
        }
    }
}
