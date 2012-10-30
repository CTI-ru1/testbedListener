package eu.uberdust.testbedlistener.datacollector.parsers;

import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import eu.uberdust.testbedlistener.coap.BlockWiseCoapRequest;
import eu.uberdust.testbedlistener.coap.CoapServer;
import eu.uberdust.testbedlistener.coap.PendingRequestHandler;
import eu.uberdust.testbedlistener.util.Converter;
import eu.uberdust.testbedlistener.util.HereIamMessage;
import eu.uberdust.testbedlistener.util.PropertyReader;
import eu.uberdust.testbedlistener.util.commiter.WsCommiter;
import org.apache.log4j.Logger;

import java.net.SocketAddress;
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
    private transient final String address;

    /**
     * The payload of the received message.
     */
    private transient final byte[] payload;
    private transient final String mac;

    private transient final Random mid;
    private boolean isBlockwise;

    /**
     * Default Constructor.
     *
     * @param address the address of the node
     * @param payload the payload message to be parsed.
     */
    public CoapMessageParser(String address, byte[] payload) {

        this.payload = payload.clone();
        this.address = address;
        this.mac = address.split("0x")[1];
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
        LOGGER.debug("from " + address + " {" + mac + "} with payload length " + payload.length);

        HereIamMessage hereIamMessage = new HereIamMessage(payload);

        if (hereIamMessage.isValid())   // check for broadcasting message
        {
            handleHereIAm();
        } else {
            LOGGER.info("Valid Coap Response Message from" + address);
            Message response = Message.fromByteArray(payload);

            //Message mess = Response.fromByteArray(payload);

            /**TODO:SEND ack only when from inside**/
            // Send Ack if requested
            //            if (response.getType() == Message.messageType.CON) {
            //                CoapServer.getInstance().sendAck(response.getMID(), address);
            //            }

            //Process message
//            LOGGER.info(response.getPayload()[0]);
            SocketAddress originSocketAddress = PendingRequestHandler.getInstance().isPending(response);
            if (originSocketAddress != null) {
                LOGGER.info("IS OUTSIDE");
                /**TODO:Check if needed**/
                //CoapServer.getInstance().endpointIsAlive(address);
                CoapServer.getInstance().sendReply(response.toByteArray(), originSocketAddress);
//                final String uriPath = CoapServer.getInstance().matchResponse(address, response);
//                LOGGER.info(uriPath);
//                CoapServer.getInstance().printAll();
            } else if (payload[3] == 0 && payload[4] == 0) {  //getting .well-known/core autoconfig phase
                LOGGER.info("IS INSIDE");

                if (!CoapServer.getInstance().registerEndpoint(address)) {
                    LOGGER.info("endpoint was already registered");
//                    return;
                }


                final byte[] inPayload = response.getPayload();
                final StringBuilder message = new StringBuilder(CoapServer.getInstance().getPending(address));
                for (int i = 0; i < inPayload.length; i++) {
                    message.append((char) inPayload[i]);

                }
                LOGGER.info(message.toString());
                List<String> capabilities = Converter.extractCapabilities(message.toString());
                isBlockwise = false;
                if (response.hasOption(OptionNumberRegistry.BLOCK2)) {
                    isBlockwise = true;
                    LOGGER.info("REQ BLOCKWISE");
                    String remainder = Converter.extractRemainder(message.toString());
                    LOGGER.info(remainder);
                    CoapServer.getInstance().addPending(address, remainder);
                }

                LOGGER.info(capabilities.size());
                reportToUberdustCapability(capabilities, address);

                for (String capability : capabilities) {
                    if (!capability.equals(".well-known/core")) {
                        LOGGER.info("cap:" + capability);
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


                if (isBlockwise) {


                    byte[] blockIdx = response.getOptions(OptionNumberRegistry.BLOCK2).get(0).getRawValue();
                    byte m = blockIdx[0];
                    m = (byte) (m >> 4);
                    m = (byte) (m & 0x01);
                    if (m == 0x00) {

                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        }
                        LOGGER.info("Requesting next block! " + Byte.toString(blockIdx[0]) + " " + blockIdx.length);
                        BlockWiseCoapRequest nextBlock = new BlockWiseCoapRequest(address, blockIdx);
                        nextBlock.run();
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
                /**TODO: fix response to uberdust to store readings

                 LOGGER.info("from" + address);

                 String uriPath = CoapServer.getInstance().matchMID(mess.getMID()).substring(1).replaceAll("/", ":");
                 StringBuilder stringReading = new StringBuilder();
                 int[] conts = Converter.getInstance().ByteToInt(mess.getPayload());
                 for (int cont : conts) {
                 stringReading.append((char) cont);
                 }
                 LOGGER.info("new Reading : " + address + " @ " + uriPath + ": " + stringReading.toString());

                 sendToUberdust(uriPath, address, mess);
                 **/
            }
        }

    }

    private void handleHereIAm() {
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
    }

    private void reportToUberdustCapability(List<String> capabilities, String address) {
        eu.uberdust.communication.protobuf.Message.NodeReadings.Builder readings = eu.uberdust.communication.protobuf.Message.NodeReadings.newBuilder();
        for (String capability : capabilities) {
            if (capability.equals("temp")) {
                capability = "temperature";
            }
            final String nodeUrn = testbedPrefix + "0x" + address;
            final String capabilityName = (capability).toLowerCase();

            final eu.uberdust.communication.protobuf.Message.NodeReadings.Reading reading = eu.uberdust.communication.protobuf.Message.NodeReadings.Reading.newBuilder()
                    .setNode(nodeUrn)
                    .setCapability("report")
                    .setTimestamp(System.currentTimeMillis())
                    .setStringReading(capabilityName)
                    .build();
            readings.addReading(reading);
        }
        new WsCommiter(readings.build());
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
