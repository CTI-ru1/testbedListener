package eu.uberdust.testbedlistener.coap.udp;

import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import eu.uberdust.testbedlistener.coap.CoapServer;
import eu.uberdust.testbedlistener.coap.PendingRequestHandler;
import org.apache.log4j.Logger;

import java.net.DatagramPacket;
import java.util.List;

/**
 * Handler Thread fro the COAP upd packets of the server.
 */
public class CoapUdpRequestHandler implements Runnable {//NOPMD

    /**
     * LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(CoapUdpRequestHandler.class);
    /**
     * The UDP Packet Received.
     */
    private transient final DatagramPacket packet;
    /**
     * The CoAP request extracted from the UDP Packet.
     */
    private transient Message udpRequest;

    /**
     * Constructor.
     *
     * @param packet The UDP packet of the request.
     */
    public CoapUdpRequestHandler(final DatagramPacket packet) {
        this.packet = packet;
        final byte[] inData = cleanupData(packet.getData());
        udpRequest = Message.fromByteArray(inData);

    }

    @Override
    public void run() {
        if (udpRequest.isAcknowledgement()) { //handle CoAP ACK
            handleCoAPAcknowledgement();
        } else if (udpRequest.isReset()) { //handle CoAP RESET
            handleCoAPReset();
        } else { //handle Other CoAP Requests
            handleCoAPRequest();
        }
    }

    private void handleCoAPAcknowledgement() {
        /** TODO **/
    }

    private void handleCoAPReset() {
        /** TODO **/
    }

    /**
     * Handles a new CoAP request.
     * Does any transformation need to send the message to the WSN.
     * Sends it using the used connection manager {TR/XBEE}.
     */
    private void handleCoAPRequest() {
        //print all options of packet
        printOptions(udpRequest);

        Request coapRequest = rebuildRequest(udpRequest);

        LOGGER.info("UDP uriPath: " + udpRequest.getUriPath());
        LOGGER.info("UDP uriQuery: " + udpRequest.getQuery());
        LOGGER.info("COAP uriPath: " + coapRequest.getUriPath());
        LOGGER.info("COAP uriQuery: " + coapRequest.getQuery());

        printOptions(coapRequest);

        final String nodeUrn = getURIHost(coapRequest);
        if ("".equals(nodeUrn)) {
            LOGGER.error("No URI HOST found!");
            return;
        }
        LOGGER.info("NodeUrn: " + nodeUrn);

        final byte[] data = coapRequest.toByteArray();

        PendingRequestHandler.getInstance().addRequest(nodeUrn, coapRequest, packet.getSocketAddress());

        final boolean hasQuery = coapRequest.hasOption(OptionNumberRegistry.URI_QUERY);
        LOGGER.info(packet.getSocketAddress());
//            CoapServer.getInstance().addRequest(nodeUrn, coapRequest, packet.getSocketAddress(), hasQuery);
        CoapServer.getInstance().sendRequest(data, nodeUrn);
    }

    /**
     * Extracts the URI_HOST from the message if it exists.
     *
     * @param request The CoAP request.
     * @return the URI_HOST as String.
     */
    private String getURIHost(final Request request) {
        if (request.hasOption(OptionNumberRegistry.URI_HOST)) {
            List<Option> options = request.getOptions(OptionNumberRegistry.URI_HOST);
            return options.get(0).getStringValue();
        }
        return "";
    }

    /**
     * Rebuilds the ready to send to the WSN CoAP request.
     *
     * @param request The incoming CoAP request.
     * @return The outgoing The CoAP request.
     */
    private Request rebuildRequest(final Message request) {
        Request newRequest = new Request(request.getCode(), request.getType().equals(Message.messageType.CON));
        LOGGER.info("incoming request of type:" + request.getType());
        newRequest.setType(request.getType());
        //set MID
        newRequest.setMID(request.getMID());
        //set Token
        if (request.hasOption(OptionNumberRegistry.TOKEN)) {
            LOGGER.debug("HAS TOKEN");
            newRequest.setOptions(OptionNumberRegistry.TOKEN, request.getOptions(OptionNumberRegistry.TOKEN));
        }
        //set URI_PATH
        if (request.hasOption(OptionNumberRegistry.URI_PATH)) {
            LOGGER.debug("HAS URI_PATH");
            final List<Option> uriPatha = Option.split(OptionNumberRegistry.URI_PATH, request.getUriPath(), "/");
            newRequest.setOptions(OptionNumberRegistry.URI_PATH, uriPatha);
        }
        //set URI_HOST
        if (request.hasOption(OptionNumberRegistry.URI_HOST)) {
            LOGGER.debug("HAS URI_HOST");
            newRequest.setOptions(OptionNumberRegistry.URI_HOST, request.getOptions(OptionNumberRegistry.URI_HOST));
        }
        //set URI_QUERY
        if (request.hasOption(OptionNumberRegistry.URI_QUERY)) {
            LOGGER.debug("HAS URI_QUERY");
            final List<Option> uriPathb = Option.split(OptionNumberRegistry.URI_QUERY, udpRequest.getQuery(), "&");
            newRequest.setOptions(OptionNumberRegistry.URI_QUERY, uriPathb);
        }
        //set BLOCK2
        if (request.hasOption(OptionNumberRegistry.BLOCK2)) {
            LOGGER.debug("HAS URI_BLOCK2");
            newRequest.setOption(request.getOptions(OptionNumberRegistry.BLOCK2).get(0));
        }
        //set OBSERVE
        if (request.hasOption(OptionNumberRegistry.OBSERVE)) {
            LOGGER.debug("HAS OBSERVE");
            newRequest.setOptions(OptionNumberRegistry.OBSERVE, request.getOptions(OptionNumberRegistry.OBSERVE));
        }
        //set PAYLOAD
        newRequest.setPayload(request.getPayload());

        return newRequest;
    }

    /**
     * Prints all option of the CoAP request.
     *
     * @param udpRequest The CoAP request.
     */
    private void printOptions(final Message udpRequest) {
        List<Option> udpOptions = udpRequest.getOptions();
        for (Option udpOption : udpOptions) {
            LOGGER.info("CoAPoption: " + udpOption.getName());
        }
    }

    /**
     * Clear tailing zeros from the udpPacket.
     *
     * @param data the incoming packet.
     * @return the data from the udp packet without tailing zeros.
     */
    private byte[] cleanupData(final byte[] data) {
        LOGGER.info(data.length);
        int myLen = data.length;
        for (int i = data.length - 1; i >= 0; i--) {
            if (data[i] != 0) {
                myLen = i + 1;
                break;
            }
        }
        final byte[] adata = new byte[myLen];
        System.arraycopy(data, 0, adata, 0, myLen);
        LOGGER.info(adata.length);
        return adata;
    }
}
