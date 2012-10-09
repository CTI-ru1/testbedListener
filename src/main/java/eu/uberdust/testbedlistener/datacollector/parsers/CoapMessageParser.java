package eu.uberdust.testbedlistener.datacollector.parsers;

import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.Response;
import com.rapplogic.xbee.api.XBeeAddress16;
import eu.uberdust.testbedlistener.coap.CoapServer;
import eu.uberdust.testbedlistener.util.Converter;
import eu.uberdust.testbedlistener.util.HereIamMessage;
import eu.uberdust.testbedlistener.util.PropertyReader;
import eu.uberdust.testbedlistener.util.commiter.WsCommiter;
import org.apache.log4j.Logger;

import java.util.Date;
import java.util.List;
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

//        if (!address.contains("9a8")) {
//            if (!address.contains("181")) {
//                return;
//            }
//        } else {
//            LOGGER.info("from 9a8 @ " + new Date());
//            LOGGER.info(Converter.getInstance().payloadToString(payload));
////            HereIamMessage mess = new HereIamMessage(payload);
////            LOGGER.info(mess.getMess());
////            LOGGER.info(mess.isValid());
//
//        }
        HereIamMessage hereIamMessage = new HereIamMessage(payload);

        if (hereIamMessage.isValid())   // check for broadcasting message
        {
//            LOGGER.info("hereIam");
//            Request request = new Request(CodeRegistry.METHOD_GET, false);
//            request.setURI("/.well-known/core");
//            try {
//                int[] zpayloap = new int[1 + request.toByteArray().length];
//                zpayloap[0] = 51;
//                System.arraycopy(Converter.getInstance().ByteToInt(request.toByteArray()), 0, zpayloap, 1, Converter.getInstance().ByteToInt(request.toByteArray()).length);
//                LOGGER.info(Converter.getInstance().payloadToString(zpayloap));
//                XBeeRadio.getInstance().send(remoteAddress, 112, zpayloap);
//            } catch (Exception e) {     //NOPMD
//                LOGGER.error(e.getMessage(), e);
//            }
        } else if (payload[0] == 51) {
            byte byteArr[] = new byte[payload.length - 1];
            for (int i = 1; i < payload.length; i++) {
                byteArr[i - 1] = (byte) payload[i];
            }
            LOGGER.info(Converter.getInstance().payloadToString(payload));
            // Coap messages start with 51


            Message response = Message.fromByteArray(byteArr);
            Message mess = Response.fromByteArray(byteArr);

            // Send Ack if requested
            if (response.getType() == Message.messageType.CON) {
                CoapServer.getInstance().sendAck(response.getMID(), address);
            }
            LOGGER.info("from"+address);
            //Process message
            LOGGER.info(response.getPayload()[0]);
            if (CoapServer.getInstance().isOutside(address, response)) {
                LOGGER.info("IS OUTSIDE");
                CoapServer.getInstance().endpointIsAlive(address);
                final String uriPath = CoapServer.getInstance().matchResponse(address, response);
                LOGGER.info(uriPath);
//                CoapServer.getInstance().printAll();
            } else if (payload[3] == 0 && payload[4] == 0) {  //getting .well-known/core autoconfig phase
                LOGGER.info("IS INSIDE");

                if (!CoapServer.getInstance().registerEndpoint(address)) {
                    LOGGER.info("endpoint was already registered");
//                    return;
                }


                final byte[] inPayload = response.getPayload();
                final StringBuilder message = new StringBuilder("");
                for (int i = 0; i < inPayload.length; i++) {
                    message.append((char) inPayload[i]);

                }
                LOGGER.info(message.toString());
                List<String> capabilities = Converter.extractCapabilities(message.toString());

                LOGGER.info(capabilities.size());
                for (String capability : capabilities) {
                    if (!capability.equals(".well-known/core")) {
                        LOGGER.info("cap:" + capability);
                        reportToUberdustCapability(capability, address);
                        LOGGER.info("skipping .well-known/core");
                        continue;

                    }
                }

                LOGGER.info(capabilities.size());
                for (String capability : capabilities) {
                    if (!capability.equals(".well-known/core")) {
                        try {
                            Thread.sleep(1000);
                            CoapServer.getInstance().requestForResource(capability, address);
                        } catch (Exception e) {
                        }
                    }
                }
//            } else {


//                if (response.hasOption(OptionNumberRegistry.BLOCK2)) {
//                    LOGGER.debug("Broadcast Message from server");
//                    Request request = new Request(CodeRegistry.METHOD_GET, false);
//                    request.setURI("/.well-known/core");
//                    Option opt = new Option(OptionNumberRegistry.BLOCK2);
//                    opt.setIntValue(1);
//                    request.addOption(opt);
////            LOGGER.info(Converter.getInstance().payloadToString(request.toByteArray()));
//                    try {
//                        int[] zpayloap = new int[1 + request.toByteArray().length];
//                        zpayloap[0] = 51;
//                        System.arraycopy(Converter.getInstance().ByteToInt(request.toByteArray()), 0, zpayloap, 1, Converter.getInstance().ByteToInt(request.toByteArray()).length);
//                        LOGGER.info(Converter.getInstance().payloadToString(zpayloap));
//                        XBeeRadio.getInstance().send(remoteAddress, 112, zpayloap);
//                    } catch (Exception e) {     //NOPMD
//                        LOGGER.error(e.getMessage(), e);
//                    }
//            }

//                }
            } else {
                LOGGER.info("from"+address);

                String uriPath = CoapServer.getInstance().matchMID(mess.getMID()).substring(1).replaceAll("/", ":");
                StringBuilder stringReading = new StringBuilder();
                int[] conts = Converter.getInstance().ByteToInt(mess.getPayload());
                for (int cont : conts) {
                    stringReading.append((char) cont);
                }
                LOGGER.info("new Reading : " + address + " @ " + uriPath + ": " + stringReading.toString());

                sendToUberdust(uriPath, address, mess);
            }
        }

    }

    private void reportToUberdustCapability(String capability, String address) {

        commitNodeReading("0x" + address, "report", capability);
    }

    private void sendToUberdust(final String uriPath, String address, final Message response) {
        LOGGER.debug(uriPath);
        if (uriPath != null) {
            String myuripath = "";
            myuripath = uriPath.replaceAll("\\/", ":");
            if (':' == myuripath.charAt(0)) {
                myuripath = myuripath.substring(1);
            }
            if (myuripath.length() > 3) {

                LOGGER.info(myuripath);
                LOGGER.info(myuripath.length());
            }
//            myuripath = myuripath.replace("lz", "light");


            try {
                Double capabilityValue = Double.valueOf(response.getPayloadString());
                commitNodeReading("0x" + address, myuripath, capabilityValue);
            } catch (final NumberFormatException e) {
                String res = response.getPayloadString().substring(0, response.getPayloadString().indexOf("\\"));
                commitNodeReading("0x" + address, myuripath, res);
                return;
            }
        }
    }

    private void commitNodeReading(final String nodeId, String capability, final Double value) {
        if (capability.equals("temp")) {
            capability = "temperature";
        }
        final String nodeUrn = testbedPrefix + nodeId;
        final String capabilityName = (capabilityPrefix + capability).toLowerCase();

        final eu.uberdust.communication.protobuf.Message.NodeReadings.Reading reading = eu.uberdust.communication.protobuf.Message.NodeReadings.Reading.newBuilder()
                .setNode(nodeUrn)
                .setCapability(capabilityName)
                .setTimestamp(System.currentTimeMillis())
                .setDoubleReading(value)
                .build();

        new WsCommiter(reading);
    }

    private void commitNodeReading(final String nodeId, String capability, final String value) {

        if (capability.equals("temp")) {
            capability = "temperature";
        }
        final String nodeUrn = testbedPrefix + nodeId;
        final String capabilityName = (capabilityPrefix + capability).toLowerCase();
        LOGGER.info(value);
        final eu.uberdust.communication.protobuf.Message.NodeReadings.Reading reading = eu.uberdust.communication.protobuf.Message.NodeReadings.Reading.newBuilder()
                .setNode(nodeUrn)
                .setCapability(capabilityName)
                .setTimestamp(System.currentTimeMillis())
                .setStringReading(value)
                .build();

        new WsCommiter(reading);
    }
}
