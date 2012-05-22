package eu.uberdust.testbedlistener.controller;

import com.google.common.collect.Lists;
import de.uniluebeck.itm.wisebed.cmdlineclient.BeanShellHelper;
import de.uniluebeck.itm.wisebed.cmdlineclient.protobuf.ProtobufControllerClient;
import de.uniluebeck.itm.wisebed.cmdlineclient.wrapper.WSNAsyncWrapper;
import eu.uberdust.DeviceCommand;
import eu.uberdust.testbedlistener.util.Converter;
import eu.uberdust.testbedlistener.util.PropertyReader;
import eu.wisebed.api.common.Message;
import eu.wisebed.api.sm.ExperimentNotRunningException_Exception;
import eu.wisebed.api.sm.SessionManagement;
import eu.wisebed.api.sm.UnknownReservationIdException_Exception;
import eu.wisebed.api.wsn.WSN;
import eu.wisebed.testbed.api.wsn.WSNServiceHelper;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.TimeUnit;


/**
 * Sends Commands to the Testbed.
 */
public final class TestbedController implements Observer {

    private static final Logger LOGGER = Logger.getLogger(TestbedController.class);
    private static final byte PAYLOAD_PREFIX = 0xb;
    private static final byte[] PAYLOAD_HEADERS = new byte[]{0x7f, 0x69, 0x70};

    private String secretReservationKeys;
    private String sessionManagementEndpointURL;
    private String nodeUrnsToListen;
    private String pccHost;
    private Integer pccPort;

    private WSNAsyncWrapper wsn;
    private List<String> nodeURNs = new ArrayList<String>();


    /**
     * static instance(ourInstance) initialized as null.
     */
    private static TestbedController ourInstance = null;

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
        readProperties();
        connectToRuntime();

    }

    private void readProperties() {
        secretReservationKeys = PropertyReader.getInstance().getProperties().getProperty("testbed.secretReservationKeys");
        sessionManagementEndpointURL = PropertyReader.getInstance().getProperties().getProperty("testbed.sm.endpointurl");
        nodeUrnsToListen = PropertyReader.getInstance().getProperties().getProperty("testbed.nodeUrns");
        pccHost = PropertyReader.getInstance().getProperties().getProperty("testbed.hostname");
        pccPort = Integer.parseInt(PropertyReader.getInstance().getProperties().getProperty("testbed.port"));
        nodeURNs = Lists.newArrayList(nodeUrnsToListen.split(","));

        if (nodeURNs.isEmpty()) {
            throw new RuntimeException("No node Urns To Listen");
        } else {
            for (String nodeURN : nodeURNs) {
                LOGGER.info(nodeURN);
            }
        }
    }

    public void connectToRuntime() {

        String wsnEndpointURL = null;
        final SessionManagement sessionManagement = WSNServiceHelper.getSessionManagementService(sessionManagementEndpointURL);
        try {
            wsnEndpointURL = sessionManagement.getInstance(BeanShellHelper.parseSecretReservationKeys(secretReservationKeys), "NONE");
        } catch (final ExperimentNotRunningException_Exception e) {
            LOGGER.error(e);
        } catch (final UnknownReservationIdException_Exception e) {
            LOGGER.error(e);
        }
        LOGGER.debug("Got a WSN instance URL, endpoint is: " + wsnEndpointURL);

        final WSN wsnService = WSNServiceHelper.getWSNService(wsnEndpointURL);
        wsn = WSNAsyncWrapper.of(wsnService);
        LOGGER.debug("Retrieved the following node URNs: {}" + nodeURNs);

        final ProtobufControllerClient pcc = ProtobufControllerClient.create(pccHost, pccPort, BeanShellHelper.parseSecretReservationKeys(secretReservationKeys));
        pcc.connect();
        //pcc.addListener(new ControllerClientListener());

    }

    public void sendCommand(final String destination, final String payloadIn) {

        // Send a message to nodes via uart (to receive them enable RX_UART_MSGS in the fronts_config.h-file)
        final Message msg = new Message();

        final String macAddress = destination.
                substring(destination.indexOf("0x") + 2);
        final byte[] macBytes = Converter.AddressToByte(macAddress);
        LOGGER.info(payloadIn);
        final String[] strPayload = payloadIn.split(",");
        final byte[] payload = new byte[strPayload.length];
        for (int i = 0; i < payload.length; i++) {
            payload[i] = Integer.valueOf(strPayload[i].replaceAll("\n", ""), 16).byteValue();
        }

        final byte[] newPayload = new byte[macBytes.length + payload.length + 1];
        newPayload[0] = PAYLOAD_PREFIX;
        System.arraycopy(macBytes, 0, newPayload, 1, macBytes.length);
//        System.arraycopy(PAYLOAD_HEADERS, 0, newPayload, 3, PAYLOAD_HEADERS.length);

        LOGGER.info("sizeIs " + payload.length);
        LOGGER.info("sizeIs " + newPayload.length);

        System.arraycopy(payload, 0, newPayload, 3, payload.length);
        msg.setBinaryData(newPayload);
        msg.setSourceNodeId("urn:wisebed:ctitestbed:0x1");

        LOGGER.debug("Sending message - " + Arrays.toString(newPayload));
        try {
            msg.setTimestamp(DatatypeFactory.newInstance().newXMLGregorianCalendar(
                    (GregorianCalendar) GregorianCalendar.getInstance()));
        } catch (final DatatypeConfigurationException e) {
            LOGGER.error(e);
        }

        wsn.send(nodeURNs, msg, 10, TimeUnit.SECONDS);

    }

    public static void main(final String[] args) {
        TestbedController.getInstance();
    }

    @Override
    public void update(final Observable observable, final Object o) {
        LOGGER.debug("called update ");
        if (o instanceof DeviceCommand) {

            final DeviceCommand command = (DeviceCommand) o;

            LOGGER.info("TO:" + command.getDestination() + " BYTES:" + command.getPayload());
            sendCommand(command.getDestination(), command.getPayload());

        }
//        if (o instanceof eu.uberdust.communication.protobuf.Message.Control) {
//            final eu.uberdust.communication.protobuf.Message.Control command
//                    = (eu.uberdust.communication.protobuf.Message.Control) o;
//
//            if (command.hasPayload()) {
//                LOGGER.info("sending to " + command.getDestination());
//                LOGGER.info("sending bytes " + command.getPayload());
//                sendCommand(command.getDestination(), command.getPayload());
//            }
//        }
    }
}
