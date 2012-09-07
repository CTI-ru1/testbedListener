package eu.uberdust.testbedlistener.datacollector.parsers;

import ch.ethz.inf.vs.californium.coap.Message;
import com.rapplogic.xbee.api.XBeeAddress16;
import eu.uberdust.testbedlistener.coap.CoapServer;
import eu.uberdust.testbedlistener.util.Converter;
import eu.uberdust.testbedlistener.util.PropertyReader;
import eu.uberdust.testbedlistener.util.commiter.WsCommiter;
import org.apache.log4j.Logger;

import java.util.Date;
import java.util.Locale;
import java.util.Random;

/**
 * Parses an XBee message received and adds data to a wisedb database.
 */
public class CoapMessageParser extends AbstractMessageParser {

    /**
     * LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(CoapMessageParser.class);

    /**
     * Testbed Capability prefix.
     */
    private transient final String capabilityPrefix;

    /**
     * The testbed prefix.
     */
    private transient final String testbedPrefix;

    /**
     * The Mac Address of the remote node.
     */
    private transient final XBeeAddress16 remoteAddress;

    /**
     * The payload of the received message.
     */
    private transient final int[] payload;
    private transient final Random mid;

    /**
     * Default Constructor.
     *
     * @param address the address of the node
     * @param payload the payload message to be parsed.
     */
    public CoapMessageParser(final XBeeAddress16 address, final int[] payload) {

        this.payload = payload.clone();
        remoteAddress = address;
        this.testbedPrefix = PropertyReader.getInstance().getTestbedPrefix();
        this.capabilityPrefix = PropertyReader.getInstance().getTestbedCapabilitiesPrefix();
        mid = new Random();
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {

        final String address = Integer.toHexString(remoteAddress.getAddress()[0]) + Integer.toHexString(remoteAddress.getAddress()[1]);

        LOGGER.debug("from " + address + " with " + payload[0] + " Length is: " + payload.length + "@ " + new Date(System.currentTimeMillis()));


        if (payload[0] == 1)   // check for broadcasting message
        {

        } else if (payload[0] == 51) {
            LOGGER.info(Converter.getInstance().payloadToString(payload));
            // Coap messages start with 51

            byte byteArr[] = new byte[payload.length - 1];
            for (int i = 1; i < payload.length; i++) {
                byteArr[i - 1] = (byte) payload[i];
            }

            Message response = Message.fromByteArray(byteArr);

            // Send Ack if requested
            if (response.getType() == Message.messageType.CON) {
                CoapServer.getInstance().sendAck(response.getMID(), address);
            }
            //Process message
            if (payload[3] == 0 && payload[4] == 0) {  //getting .well-known/core autoconfig phase

                if (!CoapServer.getInstance().registerEndpoint(address)) {
                    LOGGER.info("endpoint was already registered");
                    return;
                }


                final byte[] inPayload = response.getPayload();
                final StringBuilder message = new StringBuilder("");
                for (int i = 0; i < inPayload.length; i++) {
                    message.append((char) inPayload[i]);

                }
                LOGGER.info(message.toString());
                String[] capabilities = Converter.extractCapabilities(message.toString());
                LOGGER.info(capabilities.length);
                for (String capability : capabilities) {
                    LOGGER.info("cap:" + capability);
                    reportToUberdustCapability(capability, address);
                    if (capability.equals(".well-known/core")) {
                        LOGGER.info("skipping .well-known/core");
                        continue;

                    }
                    try {
                        Thread.sleep(300);
                        CoapServer.getInstance().registerForResource(capability, address);
                    } catch (Exception e) {
                    }
                }
            } else {
                CoapServer.getInstance().endpointIsAlive(address);
                LOGGER.info("activeRequests.matchResponse");
                final String uriPath = CoapServer.getInstance().matchResponse(address, response);
                sendToUberdust(uriPath, address, response);

            }
        }
    }

    private void reportToUberdustCapability(String capability, String address) {
        String myuripath = "report";
        commitNodeReading("0x" + address, myuripath, capability);
    }

    private void sendToUberdust(final String uriPath, String address, final Message response) {
        LOGGER.debug(uriPath);
        if (uriPath != null) {
            String myuripath = "";
            myuripath = uriPath.replaceAll("/", ":");
            if (':' == myuripath.charAt(0)) {
                myuripath = myuripath.substring(1);
            }
            try {
                Double capabilityValue = Double.valueOf(response.getPayloadString());
                commitNodeReading("0x" + address, myuripath, capabilityValue);
            } catch (final NumberFormatException e) {
                commitNodeReading("0x" + address, myuripath, response.getPayloadString());
                return;
            }
        }
    }

    private void commitNodeReading(final String nodeId, final String capability, final Double value) {

        final String nodeUrn = testbedPrefix + nodeId;
        final String capabilityName = (capabilityPrefix + capability).toLowerCase(Locale.US);

        final eu.uberdust.communication.protobuf.Message.NodeReadings.Reading reading = eu.uberdust.communication.protobuf.Message.NodeReadings.Reading.newBuilder()
                .setNode(nodeUrn)
                .setCapability(capabilityName)
                .setTimestamp(System.currentTimeMillis())
                .setDoubleReading(value)
                .build();

        new WsCommiter(reading);
    }

    private void commitNodeReading(final String nodeId, final String capability, final String value) {

        final String nodeUrn = testbedPrefix + nodeId;
        final String capabilityName = (capabilityPrefix + capability).toLowerCase(Locale.US);

        final eu.uberdust.communication.protobuf.Message.NodeReadings.Reading reading = eu.uberdust.communication.protobuf.Message.NodeReadings.Reading.newBuilder()
                .setNode(nodeUrn)
                .setCapability(capabilityName)
                .setTimestamp(System.currentTimeMillis())
                .setStringReading(value)
                .build();

        new WsCommiter(reading);
    }
}
