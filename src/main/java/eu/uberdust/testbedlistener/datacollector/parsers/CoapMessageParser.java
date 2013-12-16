package eu.uberdust.testbedlistener.datacollector.parsers;

import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import eu.uberdust.Evaluator;
import eu.uberdust.testbedlistener.CoapHelper;
import eu.uberdust.testbedlistener.coap.BlockWiseCoapRequest;
import eu.uberdust.testbedlistener.coap.PendingRequestHandler;
import eu.uberdust.testbedlistener.datacollector.collector.CollectorMqtt;
import eu.uberdust.testbedlistener.datacollector.notify.CacheNotify;
import eu.uberdust.testbedlistener.datacollector.notify.RabbitMQNotify;
import eu.uberdust.testbedlistener.util.Converter;
import eu.uberdust.testbedlistener.util.HereIamMessage;
import org.apache.log4j.Logger;

import java.net.SocketAddress;
import java.util.Arrays;
import java.util.List;

//import eu.uberdust.testbedlistener.datacollector.collector.CollectorMqtt;

/**
 * Parses an XBee message received and adds data to a wisedb database.
 */
public class CoapMessageParser extends AbstractMessageParser {

    /**
     * LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(CoapMessageParser.class);

    /**
     * The Mac Address of the remote node.
     */
    private transient final String address;

    /**
     * The payload of the received message.
     */
    private transient final byte[] payload;
    private transient String mac;

    private boolean isBlockwise;
    private byte type;
    private final CollectorMqtt mqttCollector;

    /**
     * Default Constructor.
     *
     * @param address       the address of the node
     * @param payloadin     the payload message to be parsed.
     * @param mqttCollector
     */
    public CoapMessageParser(final String address, final byte[] payloadin, final CollectorMqtt mqttCollector) {
        this.timeStart = System.currentTimeMillis();
        this.mqttCollector = mqttCollector;
//        this.apayload = new byte[payloadin.length - 2];
//        System.arraycopy(payloadin, 2, this.apayload, 0, payloadin.length - 2);
        this.payload = new byte[payloadin.length - 2];
        System.arraycopy(payloadin, 2, this.payload, 0, payloadin.length - 2);
        this.address = address;
        this.type = payloadin[0];
        this.mac = address.split("0x")[1];
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
//        synchronized (CoapMessageParser.class) {
        if (!(type == 0x69)) {
            return;
        }
//        LOGGER.info("from " + address + " {" + mac + "} with payload length " + payload.length + " fistByte " + payload[0]);
//        LOGGER.info(Converter.getInstance().payloadToString(payload));
        LOGGER.debug(Converter.getInstance().payloadToString(payload));

        if (mac.equals("ca8")) {
            LOGGER.info(Converter.getInstance().payloadToString(payload));
            final Message response = Message.fromByteArray(payload);
            response.prettyPrint();
        }


        HereIamMessage hereIamMessage = new HereIamMessage(payload);

        if (hereIamMessage.isValid())   // check for broadcasting message
        {

            handleHereIAm();
        } else {

            final Message response = Message.fromByteArray(payload);

            if (mac.equals("ca8")) {
                response.prettyPrint();
            }

            SocketAddress originSocketAddress = PendingRequestHandler.getInstance().isPending(response);
            if (originSocketAddress != null) {
                LOGGER.debug("External Coap Message from:" + mac);

                mqttCollector.sendReply(response.toByteArray(), originSocketAddress);
            } else {  //getting .well-known/core autoconfig phase
                LOGGER.debug("Coap Observe Response from: " + mac);

                String parts = mqttCollector.matchResponse(response);
                if ((parts == null) || ("".equals(parts))) {
                    return;
                }
                String requestType = parts.split(",")[1];
                mac = parts.split(",")[0];
                LOGGER.info("RequstURI {" + mac + "}" + requestType);
                if ("".equals(requestType)) {
                    //Unknown or duplicate request
                    return;
                }

                if (requestType.equals("/.well-known/core")) {

                    mqttCollector.updateEndpoint(mac, requestType.substring(1));

                    final String payloadStr = response.getPayloadString();

                    List<String> capabilities = Converter.extractCapabilities(payloadStr);

                    isBlockwise = false;
                    if (response.hasOption(OptionNumberRegistry.BLOCK2)) {
                        isBlockwise = true;
                        String remainder = Converter.extractRemainder(payloadStr);
                        LOGGER.info(remainder);
                        mqttCollector.addPending(address, remainder);
                    }

                    LOGGER.info("Capabilities {" + mac + "}" + Arrays.toString(capabilities.toArray()));

                    for (String capability : capabilities) {
                        if (capability.equals(".well-known/core")) continue;
                        if (capability.equals("r")) continue;
                        if (capability.equals("rdf")) continue;

                        if (mqttCollector.isAlive(capability, mac)) {
                            continue;
                        }
                        try {
                            Thread.sleep(250);
                        } catch (Exception e) {
                            LOGGER.error(e, e);
                        }
                        mqttCollector.requestForResource(capability, mac, true);

                    }


//                    if (true) {
//                        return;
//                    }

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
                                LOGGER.error(e, e);
                            }
                            LOGGER.info("Requesting next block! " + Byte.toString(blockIdx[0]) + " " + blockIdx.length);
                            BlockWiseCoapRequest nextBlock = new BlockWiseCoapRequest(mac, blockIdx);
                            nextBlock.run();
                        }
                    }

                } else {


                    requestType = requestType.substring(1);
                    new Evaluator("TokenMatch ", requestType);
                    String responseContents = response.getPayloadString();

                    if (response.isConfirmable()) {
                        LOGGER.info("Sending ack message...");
                        mqttCollector.sendAck(response.getMID(), mac);
                    }
                    mqttCollector.registerEndpoint(requestType, mac);

                    LOGGER.info("Notify " + mac + "/" + requestType);
                    new Thread(new RabbitMQNotify(mac, requestType, response, mqttCollector)).start();
                    new Thread(new CacheNotify(mqttCollector.getDeviceID() + "/" + mac + "/" + requestType, response, mqttCollector)).start();
                    mqttCollector.incResponseObserveCounter();
                }
            }

        }

        new Evaluator("TRCoAPMessageParser", (System.currentTimeMillis() - timeStart), "millis");
//        }
    }

    private void handleHereIAm() {
        final byte macMSB = payload[0];
        final byte macLSB = payload[1];
        final String macStr = Converter.byteToString(macMSB) + Converter.byteToString(macLSB);
//        if (macStr.contains("1ccd")) return;
        //if (!CoapServer.getInstance().registerEndpoint(".well-known/core", macStr)) return;
//        CoapServer.getInstance().registerEndpoint(".well-known/core", macStr);
        mqttCollector.registerEndpoint(".well-known/core", macStr);
        LOGGER.info("Requesting .well-known/core uri_host:" + macStr);
        Request request = CoapHelper.getWellKnown(macStr, mqttCollector.nextMID());
//        CoapServer.getInstance().addRequest(macStr, request, false);
        mqttCollector.addRequest(macStr, request, false);
//        System.out.println("request"+request.toByteArray().length);
        mqttCollector.sendRequest(request.toByteArray(), macStr);
//        CoapServer.getInstance().incRequestWellKnownCounter();
        mqttCollector.incRequestWellKnownCounter();
    }


}
