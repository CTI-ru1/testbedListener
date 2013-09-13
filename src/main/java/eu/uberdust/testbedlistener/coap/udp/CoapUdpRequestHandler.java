package eu.uberdust.testbedlistener.coap.udp;

import ch.ethz.inf.vs.californium.coap.EndpointAddress;
import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import eu.uberdust.Evaluator;
import eu.uberdust.testbedlistener.coap.*;
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
    private final byte[] inData;
    private long timeStart;

    /**
     * Constructor.
     *
     * @param packet The UDP packet of the request.
     */
    public CoapUdpRequestHandler(final DatagramPacket packet) {
        this.timeStart = System.currentTimeMillis();
        this.packet = packet;
        inData = cleanupData(packet.getData());
        LOGGER.info("Inco" + inData.length);
        udpRequest = Message.fromByteArray(inData);
    }

    @Override
    public void run() {
//        Message mes = Message.fromByteArray(inData);
//        LOGGER.info("MESSAGE TYPE:" + mes.getType() + " " + mes.isAcknowledgement());
        if (udpRequest.isAcknowledgement()) { //handle CoAP ACK
            handleCoAPAcknowledgement();
            new Evaluator("UDPRequestAckHandler", (System.currentTimeMillis() - timeStart), "millis");
        } else if (udpRequest.isReset()) { //handle CoAP RESET
            handleCoAPReset();
            new Evaluator("UDPRequestResetHandler", (System.currentTimeMillis() - timeStart), "millis");
        } else { //handle Other CoAP Requests
            handleCoAPRequest();
            new Evaluator("UDPRequestCoAPHandler", (System.currentTimeMillis() - timeStart), "millis");
        }
    }

    /**
     * Handles a Coap ACK.
     * Finds the URI_HOST based on the MID of the CoAP ACK.
     * Send the CoAP ACK to the device.
     */
    private void handleCoAPAcknowledgement() {
        String uriHost = PendingRequestHandler.getInstance().matchMID4Host(udpRequest);
        printOptions(udpRequest);
        CoapServer.getInstance().sendRequest(udpRequest.toByteArray(), uriHost);
    }

    /**
     * Handles a Coap Reset Request.
     * Finds the URI_HOST based on the MID of the RST CoAP Request.
     * Send the RST CoAP Request to the device.
     */
    private void handleCoAPReset() {
        String uriHost = PendingRequestHandler.getInstance().matchMID4Host(udpRequest);
        printOptions(udpRequest);
        CoapServer.getInstance().sendRequest(udpRequest.toByteArray(), uriHost);
        PendingRequestHandler.getInstance().removeRequest(udpRequest);
    }

    /**
     * Handles a new CoAP request.
     * Does any transformation need to send the message to the WSN.
     * Sends it using the used connection manager {TR/XBEE}.
     */
    private void handleCoAPRequest() {
        //print all options of packet
        printOptions(udpRequest);


//        LOGGER.info("COAP uriPath: " + coapRequest.getUriPath());
//        LOGGER.info("COAP uriQuery: " + coapRequest.getQuery());
//
//        printOptions(coapRequest);

        String uriHost = getURIHost(udpRequest);
        LOGGER.info("UDP path:" + udpRequest.getUriPath());
        udpRequest.setPeerAddress(new EndpointAddress(packet.getAddress()));
        udpRequest = InternalCoapRequest.getInstance().handleRequest(uriHost, udpRequest, packet.getSocketAddress());
        if ("".equals(uriHost)) {
            uriHost = getURIHost(udpRequest);
        }

//        if ("".equals(uriHost)) {
//            udpRequest = InternalCoapRequest.getInstance().handleRequest(udpRequest, packet.getSocketAddress());
//            uriHost = getURIHost(udpRequest);
//        }
        if (udpRequest == null)   {
            LOGGER.info("UDP null");
            return;
        }

        LOGGER.info("UDP {source:" + packet.getSocketAddress()
                + ", uriPath:" + udpRequest.getUriPath()
                + ", uriQuery:" + udpRequest.getQuery()
                + ", uriHost:" + uriHost
                + "}");

        final byte[] data = udpRequest.toByteArray();

        PendingRequestHandler.getInstance().addRequest(uriHost, udpRequest, packet.getSocketAddress());
        CoapServer.getInstance().sendRequest(data, uriHost);
    }

    /**
     * Extracts the URI_HOST from the message if it exists.
     *
     * @param request The CoAP request.
     * @return the URI_HOST as String.
     */
    private String getURIHost(final Message request) {
        if (request.hasOption(OptionNumberRegistry.URI_HOST)) {
            List<Option> options = request.getOptions(OptionNumberRegistry.URI_HOST);
            return options.get(0).getStringValue();
        }
        //return "ffff";
        return "";
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
