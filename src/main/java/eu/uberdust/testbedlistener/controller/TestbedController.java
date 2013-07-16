package eu.uberdust.testbedlistener.controller;

import eu.uberdust.DeviceCommand;
import eu.uberdust.testbedlistener.coap.CoapServer;
import eu.uberdust.testbedlistener.datacollector.collector.TestbedRuntimeCollector;
import eu.uberdust.testbedlistener.util.Converter;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.net.InetAddress;
import java.net.UnknownHostException;
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
        LOGGER.info("Starting TestbedController...");
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
        if (testbed != null) {
            testbed.sendMessage(payload, destination);
        } else {
            LOGGER.debug("Testbed==null");
        }

    }

    public void sendMessage(byte[] payload, String destination) {
        try {
            testbed.sendMessage(payload, destination);
        } catch (Exception e) {
            LOGGER.error(e, e);
        }
    }


    public static void main(final String[] args) {
        TestbedController.getInstance();
    }

    @Override
    public void update(final Observable observable, final Object o) {
        LOGGER.info("called update ");
        if (o instanceof DeviceCommand) {
            final DeviceCommand command = (DeviceCommand) o;


            if (command.getDestination().contains("150.140.5")) {
                try {
                    InetAddress.getByName(command.getDestination().substring(command.getDestination().lastIndexOf(":") + 1));
                    CoapServer.getInstance().sendEthernetRequest(command);
                    LOGGER.debug("EthernetTO:" + command.getDestination() + " BYTES:" + command.getPayload());
                } catch (UnknownHostException e) {
                    sendCommand(command.getDestination(), command.getPayload());
                    LOGGER.debug("TO:" + command.getDestination() + " BYTES:" + command.getPayload());

                }
            } else {

                final byte[] payload = Converter.getInstance().commaPayloadtoBytes(command.getPayload());
                final byte[] headlessPayload =new byte[payload.length-1];
                System.arraycopy(payload,1,headlessPayload,0,headlessPayload.length);

                CoapServer.getInstance().sendRequest(headlessPayload, command.getDestination().substring(command.getDestination().lastIndexOf(":0x") + 3));

                LOGGER.debug("MQttTO:" + command.getDestination() + " BYTES:" + command.getPayload());
            }

        }
    }

    public void setTestbed(TestbedRuntimeCollector testbed) {
        LOGGER.info("setting testbed to " + testbed);
        this.testbed = testbed;
    }
}
