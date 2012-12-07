package eu.uberdust.testbedlistener.datacollector.parsers;

import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import eu.uberdust.Evaluator;
import eu.uberdust.testbedlistener.CoapHelper;
import eu.uberdust.testbedlistener.coap.BlockWiseCoapRequest;
import eu.uberdust.testbedlistener.coap.CoapServer;
import eu.uberdust.testbedlistener.coap.PendingRequestHandler;
import eu.uberdust.testbedlistener.datacollector.notify.CacheNotify;
import eu.uberdust.testbedlistener.datacollector.notify.UberdustNotify;
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
    private transient String mac;

    private transient final Random mid;
    private boolean isBlockwise;
    private byte type;

    /**
     * Default Constructor.
     *
     * @param address   the address of the node
     * @param payloadin the payload message to be parsed.
     */
    public CoapMessageParser(String address, byte[] payloadin) {
        this.timeStart = System.currentTimeMillis();
        this.payload = new byte[payloadin.length - 2];
        System.arraycopy(payloadin, 2, this.payload, 0, payloadin.length - 2);
        this.address = address;
        this.type = payloadin[0];
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
        synchronized (CoapMessageParser.class) {
            if (!(type == 0x69)) {
                return;
            }

//        LOGGER.info("from " + address + " {" + mac + "} with payload length " + payload.length + " fistByte " + payload[0]);
//        LOGGER.info(Converter.getInstance().payloadToString(payload));
            LOGGER.info(Converter.getInstance().payloadToString(payload));

            HereIamMessage hereIamMessage = new HereIamMessage(payload);

            if (hereIamMessage.isValid())   // check for broadcasting message
            {
                if (CoapServer.getInstance().rejectDuplicate(Message.fromByteArray(payload).toString())) {
                    LOGGER.info("Rejecting here I am");
                    return;
                }
                handleHereIAm();
            } else {

                final Message response = Message.fromByteArray(payload);
                if (!response.hasOption(OptionNumberRegistry.OBSERVE) && CoapServer.getInstance().rejectDuplicate(Message.fromByteArray(payload).toString())) {
                    LOGGER.info("Rejecting normal response");
                    return;
                }
//            response.prettyPrint();
                SocketAddress originSocketAddress = PendingRequestHandler.getInstance().isPending(response);
                LOGGER.info(originSocketAddress);
                if (originSocketAddress != null) {
                    LOGGER.info("Valid External Coap Response Message from " + mac);
                    CoapServer.getInstance().sendReply(response.toByteArray(), originSocketAddress);
                } else {  //getting .well-known/core autoconfig phase
                    LOGGER.info("Valid Internal Coap Response Message from " + mac);

                    String parts = CoapServer.getInstance().matchResponse(response);
                    if ((parts == null) || ("".equals(parts))) {
                        return;
                    }
                    String requestType = parts.split(",")[1];
                    mac = parts.split(",")[0];
                    LOGGER.info("RequstURI:" + requestType);
                    if ("".equals(requestType)) {
                        //Unknown or duplicate request
                        return;
                    }

                    if (requestType.equals("/.well-known/core")) {
                        LOGGER.info("WELL-KNOWN " + mac);

                        CoapServer.getInstance().updateEndpoint(mac, requestType.substring(1));

                        final String payloadStr = response.getPayloadString();

                        List<String> capabilities = Converter.extractCapabilities(payloadStr);

                        isBlockwise = false;
                        if (response.hasOption(OptionNumberRegistry.BLOCK2)) {
                            LOGGER.info("REQ BLOCKWISE");
//                    isBlockwise = true;
//                    String remainder = Converter.extractRemainder(payloadStr);
//                    LOGGER.info(remainder);
//                    CoapServer.getInstance().addPending(address, remainder);
                        }

                        LOGGER.info(capabilities.size());
                        reportToUberdustCapability(capabilities, mac);

                        for (String capability : capabilities) {
                            if (!capability.equals(".well-known/core")) {
                                LOGGER.info("cap:" + capability);
                            }
                        }

                        LOGGER.info(capabilities.size());
                        for (String capability : capabilities) {
                            if (!capability.equals(".well-known/core")) {
                                if (CoapServer.getInstance().isAlive(capability, mac)) {
                                    continue;
                                }
                                try {
                                    Thread.sleep(1000);
                                    CoapServer.getInstance().requestForResource(capability, mac, true);
                                } catch (Exception e) {
                                    LOGGER.error(e, e);
                                }
                            }
                        }

                        if (true) {
                            return;
                        }

                        if (isBlockwise) {
                            /**TODO : change**/
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
                                BlockWiseCoapRequest nextBlock = new BlockWiseCoapRequest(mac, blockIdx);
                                nextBlock.run();
                            }
                        }

                    } else {

                        LOGGER.info("is a response to a SREQ");
                        requestType = requestType.substring(1);
                        new Evaluator("TokenMatch ", requestType);
                        String responseContents = response.getPayloadString();
                        LOGGER.info(requestType + " @ " + responseContents);

                        if (response.isConfirmable()) {
                            LOGGER.info("WILL SEND ACK");
                            CoapServer.getInstance().sendAck(response.getMID(), mac);
                        }
                        CoapServer.getInstance().registerEndpoint(requestType, mac);

//                    Thread d = new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            sendToUberdust(finalRequestType, mac, response);
//                        }
//                    });
//                    d.start();

                        new Thread(new UberdustNotify(mac, requestType, response, testbedPrefix, capabilityPrefix)).start();
                        new Thread(new CacheNotify(mac, requestType, response)).start();


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
                    }
                }
// else {
//                LOGGER.info("was sent here? ? ");
//                /**TODO: fix response to uberdust to store readings
//
//                 LOGGER.info("from" + address);
//
//                 String uriPath = CoapServer.getInstance().matchMID(mess.getMID()).substring(1).replaceAll("/", ":");
//                 StringBuilder stringReading = new StringBuilder();
//                 int[] conts = Converter.getInstance().ByteToInt(mess.getPayload());
//                 for (int cont : conts) {
//                 stringReading.append((char) cont);
//                 }
//                 LOGGER.info("new Reading : " + address + " @ " + uriPath + ": " + stringReading.toString());
//
//                 sendToUberdust(uriPath, address, mess);
//                 **/
//            }
            }

            new Evaluator("TRCoAPMessageParser", (System.currentTimeMillis() - timeStart), "millis");
        }
    }

    private void handleHereIAm() {
        final byte macMSB = payload[0];
        final byte macLSB = payload[1];
        final String macStr = Converter.byteToString(macMSB) + Converter.byteToString(macLSB);
//        if (macStr.contains("1ccd")) return;
        //if (!CoapServer.getInstance().registerEndpoint(".well-known/core", macStr)) return;
        CoapServer.getInstance().registerEndpoint(".well-known/core", macStr);
        LOGGER.info("Requesting .well-known/core uri_host:" + macStr);
        Request request = CoapHelper.getWellKnown(macStr);
        CoapServer.getInstance().addRequest(macStr, request, false);
        CoapServer.getInstance().sendRequest(request.toByteArray(), macStr);
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

//    private void sendToUberdust(final String uriPath, String address, final Message response) {
//
//    }

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
