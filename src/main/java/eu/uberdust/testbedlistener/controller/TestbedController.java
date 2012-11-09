package eu.uberdust.testbedlistener.controller;

import eu.uberdust.DeviceCommand;
import eu.uberdust.testbedlistener.datacollector.TestbedRuntimeCollector;
import eu.uberdust.testbedlistener.util.Converter;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.util.Observable;
import java.util.Observer;


/**
 * Sends Commands to the Testbed.
 */
public final class TestbedController implements Observer {
    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(TestbedController.class);

    /**
     * static instance(ourInstance) initialized as null.
     */
    private static TestbedController ourInstance = null;
    /**
     * Testbed Runtime Controller instance.
     */
    private TestbedRuntimeCollector testbed;

    /**
     * TestbedController is loaded on the first execution of TestbedController.getInstance()
     * or the first access to TestbedController.ourInstance, not before.
     *
     * @return ourInstance
     */
    public static TestbedController getInstance() {
        synchronized (TestbedController.class) {
            if (ourInstance == null) {
                ourInstance = new TestbedController();
            }
        }

        return ourInstance;
    }

    /**
     * Private constructor suppresses generation of a (public) default constructor.
     */
    private TestbedController() {
        PropertyConfigurator.configure(this.getClass().getClassLoader().getResource("log4j.properties"));
    }

    public void sendCommand(final String destination, final String payloadIn) {

        // Send a message to nodes via uart (to receive them enable RX_UART_MSGS in the fronts_config.h-file)
//        final Message msg = new Message();
        //Check for a real mac address.
        if (!destination.contains("0x")) {
            return;
        }
        LOGGER.debug(payloadIn);
        final byte[] payload = Converter.getInstance().commaPayloadtoBytes(payloadIn);
        testbed.sendMessage(payload, destination);
    }

    public void sendMessage(byte[] payload, String destination) {
        testbed.sendMessage(payload, destination);
    }


    public static void main(final String[] args) {
        TestbedController.getInstance();
    }

    @Override
    public void update(final Observable observable, final Object o) {
        LOGGER.debug("called update ");
        if (o instanceof DeviceCommand) {
            final DeviceCommand command = (DeviceCommand) o;
            LOGGER.debug("TO:" + command.getDestination() + " BYTES:" + command.getPayload());
//            sendCommand(command.getDestination(), command.getPayload());
        }
    }

    public void setTestbed(TestbedRuntimeCollector testbed) {
        this.testbed = testbed;
    }
}
