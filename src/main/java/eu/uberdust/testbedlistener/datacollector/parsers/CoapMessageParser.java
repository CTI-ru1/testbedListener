package eu.uberdust.testbedlistener.datacollector.parsers;

import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.coap.TokenManager;
import com.rapplogic.xbee.api.XBeeAddress16;
import eu.mksense.XBeeRadio;
import eu.uberdust.testbedlistener.coap.ActiveRequest;
import eu.uberdust.testbedlistener.coap.CoapServer;
import eu.uberdust.testbedlistener.datacollector.commiter.WsCommiter;
import eu.uberdust.testbedlistener.util.PropertyReader;
import org.apache.log4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import static ch.ethz.inf.vs.californium.coap.CodeRegistry.METHOD_GET;

/**
 * Parses an XBee message received and adds data to a wisedb database.
 */
public class CoapMessageParser implements Runnable {

    /**
     * LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(CoapMessageParser.class);

    /**
     * Testbed Capability prefix.
     */
    private final String capabilityPrefix;

    /**
     * Index of capability.
     */
    private final static int INDEX = 7;

    /**
     * The testbed prefix.
     */
    private final String testbedPrefix;

    /**
     * The testbed id.
     */
    private final int testbedId;

    /**
     * The Mac Address of the remote node.
     */
    private final XBeeAddress16 remoteAddress;

    /**
     * The payload of the received message.
     */
    private final int[] payload;
    private Random mid;
    private ActiveRequest activeRequests;

    /**
     * Default Constructor.
     *
     * @param address the address of the node
     * @param payload the payload message to be parsed.
     */
    public CoapMessageParser(final XBeeAddress16 address, final int[] payload) {

        this.payload = payload;
        remoteAddress = address;
        this.testbedPrefix = PropertyReader.getInstance().getTestbedPrefix();
        this.capabilityPrefix = PropertyReader.getInstance().getTestbedCapabilitiesPrefix();
        this.testbedId = PropertyReader.getInstance().getTestbedId();
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

        LOGGER.info("RECEIVED FROM " + address);
        if (!address.equals("472")) return;
        LOGGER.info("from " + address + " with " + payload[0] + " Length is: " + payload.length + "@ " + new Date(System.currentTimeMillis()));
        StringBuilder stringBuilder = new StringBuilder("contents:");
        for (int i : payload) {
            stringBuilder.append(Integer.toHexString(i)).append("|");
        }
        LOGGER.info(stringBuilder.toString());
        if (payload[0] == 1)   // check for broadcasting message
        {
//            if (CoapServer.getInstance().registerEndpoint(address)) {
            //Message request = new Message();
            //request.setURI("/.well-known/core");
            int[] mpayload = new int["33:52:01:00:00:9b:2e:77:65:6c:6c:2d:6b:6e:6f:77:6e:04:63:6f:72:65".split(":").length];
            int i = 0;
            for (String mbyte : "33:52:01:00:00:9b:2e:77:65:6c:6c:2d:6b:6e:6f:77:6e:04:63:6f:72:65".split(":")) {
                mpayload[i] = Integer.parseInt(mbyte, 16);
                i++;
            }
            try {
                LOGGER.info("Sending to arduino");
                XBeeRadio.getInstance().send(remoteAddress, 112, mpayload);
            } catch (Exception e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
//            }
        } else if (payload[0] == 51) // coap message
        {
            byte byteArr[] = new byte[payload.length - 1];
            for (int i = 1; i < payload.length; i++) {
                byteArr[i - 1] = (byte) payload[i];
            }
            Message response = Message.fromByteArray(byteArr);

            // SEND ACK
            if (response.getType() == Message.messageType.CON) {
                Message ack = new Message(Message.messageType.ACK, 0);
                ack.setMID(response.getMID());
                CoapServer.getInstance().sendRequest(ack.toByteArray(), address);
            } // END OF SEND ACK

            if (payload[3] == 0 && payload[4] == 0) {  //getting .well-known/core autoconfig phase
                byte[] inPayload = response.getPayload();
                StringBuilder message = new StringBuilder("Response:");
                for (int i = 0; i < inPayload.length; i++) {
                    message.append((char) inPayload[i]);

                }
                LOGGER.info(message.toString());
                String[] temp = message.toString().split("<");
                for (int i = 2; i < temp.length; i++) {
                    String[] temp2 = temp[i].split(">");
                    URI uri = null;
                    try {
                        uri = new URI(new StringBuilder().append("/").append(temp2[0]).toString());
                    } catch (URISyntaxException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                    Message request = new Message(Message.messageType.NON, METHOD_GET);
                    request.setMID(mid.nextInt() % 65535);
                    List<Option> uriPath = Option.split(OptionNumberRegistry.URI_PATH, uri.getPath(), "/");
                    request.setOptions(OptionNumberRegistry.URI_PATH, uriPath);
                    request.setOption(new Option(0, OptionNumberRegistry.OBSERVE));
                    request.setToken(TokenManager.getInstance().acquireToken());

                    CoapServer.getInstance().addRequest(address, request, null, false);


                    CoapServer.getInstance().sendRequest(request.toByteArray(), address);

                }
                return;
            } else {
                LOGGER.info("activeRequests.matchResponse");
                String uriPath = CoapServer.getInstance().matchResponse(address, response);
                LOGGER.info(uriPath);
                if (uriPath != null) {
                    LOGGER.info(response.getPayloadString());

                    Double capabilityValue;
                    try {
                        capabilityValue = Double.valueOf(response.getPayloadString());
                    } catch (final NumberFormatException e) {
                        LOGGER.error(e);
                        LOGGER.info(address);
                        commitNodeReading("0x" + address, uriPath.substring(uriPath.lastIndexOf("/") + 1), response.getPayloadString());
                        return;
                    }
                    commitNodeReading(address, uriPath, capabilityValue);
                }
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

        eu.uberdust.communication.protobuf.Message.NodeReadings.Reading reading = eu.uberdust.communication.protobuf.Message.NodeReadings.Reading.newBuilder()
                .setNode(nodeUrn)
                .setCapability(capabilityName)
                .setTimestamp(System.currentTimeMillis())
                .setStringReading(value)
                .build();

        new WsCommiter(reading);
    }
}
