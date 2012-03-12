package eu.uberdust.controller;

import com.google.common.collect.Lists;
import de.uniluebeck.itm.wisebed.cmdlineclient.BeanShellHelper;
import de.uniluebeck.itm.wisebed.cmdlineclient.protobuf.ProtobufControllerClient;
import de.uniluebeck.itm.wisebed.cmdlineclient.wrapper.WSNAsyncWrapper;
import eu.uberdust.uberlogger.UberLogger;
import eu.uberdust.util.PropertyReader;
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
 * Created by IntelliJ IDEA.
 * User: akribopo
 * Date: 10/3/11
 * Time: 2:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestbedController implements Observer {

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

    private void connectToRuntime() {

        String wsnEndpointURL = null;
        final SessionManagement sessionManagement = WSNServiceHelper.getSessionManagementService(sessionManagementEndpointURL);
        try {
            wsnEndpointURL = sessionManagement.getInstance(BeanShellHelper.parseSecretReservationKeys(secretReservationKeys), "NONE");
        } catch (final ExperimentNotRunningException_Exception e) {
            LOGGER.error(e);
        } catch (final UnknownReservationIdException_Exception e) {
            LOGGER.error(e);
        }
        LOGGER.info("Got a WSN instance URL, endpoint is: " + wsnEndpointURL);

        final WSN wsnService = WSNServiceHelper.getWSNService(wsnEndpointURL);
        wsn = WSNAsyncWrapper.of(wsnService);
        LOGGER.info("Retrieved the following node URNs: {}" + nodeURNs);

        final ProtobufControllerClient pcc = ProtobufControllerClient.create(pccHost, pccPort, BeanShellHelper.parseSecretReservationKeys(secretReservationKeys));
        pcc.connect();
        //pcc.addListener(new ControllerClientListener());

    }

    public void sendCommand(final String destination, final String payloadIn) {

        // Send a message to nodes via uart (to receive them enable RX_UART_MSGS in the fronts_config.h-file)
        final Message msg = new Message();
        if (destination.contains("494")) {
            final String pl = payloadIn.replaceAll(",", "");
            final String nodeId = pl.substring(3);
            UberLogger.getInstance().log(nodeId, "T91");
        }

        final String macAddress = destination.
                substring(destination.indexOf("0x") + 2);
        final byte[] macBytes = new byte[2];
        if (macAddress.length() == 4) {
            macBytes[0] = Integer.valueOf(macAddress.substring(0, 2), 16).byteValue();
            macBytes[1] = Integer.valueOf(macAddress.substring(2, 4), 16).byteValue();
        } else if (macAddress.length() == 3) {
            macBytes[0] = Integer.valueOf(macAddress.substring(0, 1), 16).byteValue();
            macBytes[1] = Integer.valueOf(macAddress.substring(1, 3), 16).byteValue();
        }

        final String[] strPayload = payloadIn.split(",");
        final byte[] payload = new byte[strPayload.length];
        for (int i = 0; i < payload.length; i++) {
            payload[i] = Integer.valueOf(strPayload[i].replaceAll("\n", ""), 16).byteValue();
        }

        final byte[] newPayload = new byte[macBytes.length + payload.length + 1 + PAYLOAD_HEADERS.length];
        newPayload[0] = PAYLOAD_PREFIX;
        System.arraycopy(macBytes, 0, newPayload, 1, macBytes.length);
        System.arraycopy(PAYLOAD_HEADERS, 0, newPayload, 3, PAYLOAD_HEADERS.length);
        System.arraycopy(payload, 0, newPayload, 6, payload.length);
        msg.setBinaryData(newPayload);
        msg.setSourceNodeId("urn:wisebed:ctitestbed:0x1");

        LOGGER.info("Sending message - " + Arrays.toString(newPayload));
        try {
            msg.setTimestamp(DatatypeFactory.newInstance().newXMLGregorianCalendar(
                    (GregorianCalendar) GregorianCalendar.getInstance()));
        } catch (final DatatypeConfigurationException e) {
            LOGGER.error(e);
        }

        if (destination.contains("494")) {
            final String pl = payloadIn.replaceAll(",", "");
            final String nodeId = pl.substring(3);
            UberLogger.getInstance().log(nodeId, "T10");
        }

        wsn.send(nodeURNs, msg, 10, TimeUnit.SECONDS);
        if (destination.contains("494")) {
            final String pl = payloadIn.replaceAll(",", "");
            final String nodeId = pl.substring(3);
            UberLogger.getInstance().log(nodeId, "T101");
        }
    }

    public static void main(final String[] args) {
        TestbedController.getInstance();
    }

    @Override
    public void update(final Observable observable, final Object o) {
        LOGGER.info("called update ");

        final String[] commandParts = o.toString().split("@");
        if (commandParts.length == 2) {

            final String nodeId = commandParts[0];
            final String bytes = commandParts[1];
            LOGGER.info("sending to " + nodeId);
            LOGGER.info("sending bytes " + bytes);
            sendCommand(nodeId, bytes);
        }
    }
}
