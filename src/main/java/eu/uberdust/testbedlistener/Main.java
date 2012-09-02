package eu.uberdust.testbedlistener;

import eu.uberdust.testbedlistener.factory.CoapListenerFactory;
import eu.uberdust.testbedlistener.factory.CommandLineListenerFactory;
import eu.uberdust.testbedlistener.factory.TestbedRuntimeListenerFactory;
import eu.uberdust.testbedlistener.factory.XbeeListenerFactory;
import eu.uberdust.testbedlistener.util.PropertyReader;

/**
 * Created by IntelliJ IDEA.
 * User: amaxilatis
 * Date: 8/30/12
 * Time: 2:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class Main {
    /**
     * Starts the application.
     *
     * @param args not used.
     */
    public static void main(final String[] args) {
        PropertyReader.getInstance().setFile("listener.properties");

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
