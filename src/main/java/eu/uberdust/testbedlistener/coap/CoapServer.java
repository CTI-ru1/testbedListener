package eu.uberdust.testbedlistener.coap;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.TokenManager;
import com.rapplogic.xbee.api.XBeeAddress16;
import eu.mksense.XBeeRadio;
import eu.uberdust.testbedlistener.coap.udp.UDPhandler;
import eu.uberdust.testbedlistener.util.Converter;
import eu.uberdust.testbedlistener.util.PropertyReader;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;

/**
 * Created by IntelliJ IDEA.
 * User: amaxilatis
 * Date: 5/19/12
 * Time: 6:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class CoapServer {
    /**
     * LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(CoapServer.class);
    /**
     * Singleton instance.
     */
    private static CoapServer instance = null;
    /**
     * Registered Endpoints.
     */
    private transient final Map<String, Long> endpoints;
    /**
     * Active Requests.
     */
    private transient final Map<String, List<ActiveRequest>> activeRequests;
    /**
     * COAP Server socket.
     */
    private transient DatagramSocket socket;

    /**
     * Random number generator.
     */
    private transient final Random mid;
    private String testbedPrefix;
    private static final long MILLIS_TO_STALE = 3 * 60 * 1000;

    /**
     * Constructor.
     */
    public CoapServer() {
        this.endpoints = new HashMap<String, Long>();
        this.activeRequests = new HashMap<String, List<ActiveRequest>>();
        this.testbedPrefix = PropertyReader.getInstance().getTestbedPrefix();
        mid = new Random();

        Timer discoveryTimer = new Timer();
        discoveryTimer.scheduleAtFixedRate(new BroadcastCoapRequest(), 20000, 60000);

        //Start the udp socket
        try {
            socket = new DatagramSocket(5683);
        } catch (SocketException e) {
            LOGGER.error(e.getMessage(), e);
        }

        //Start the handler
        final UDPhandler thread = new UDPhandler(socket);
        thread.start();

        LOGGER.info("started CoapServer");
    }

    /**
     * Singleton Class.
     *
     * @return The unique instance of CoapServer.
     */
    public static CoapServer getInstance() {
        synchronized (CoapServer.class) {
            if (instance == null) {
                instance = new CoapServer();
            }
        }
        return instance;
    }

    /**
     * Adds an endpoint to the list of endpoints provided by the server.
     *
     * @param endpoint an endpoint address.
     * @return if the endoint existed in the server.
     */
    public boolean registerEndpoint(final String endpoint) {
        LOGGER.info("registerEndpoint");

        synchronized (CoapServer.class) {
            if (endpoints.containsKey(endpoint)) {
                if (System.currentTimeMillis() - endpoints.get(endpoint) > MILLIS_TO_STALE) {
                    endpoints.put(endpoint, System.currentTimeMillis());
                    LOGGER.info("endpoint was stale");
                    return true;
                } else {
                    return false;
                }
            } else {
                LOGGER.info("inserting endpoint");
                endpoints.put(endpoint, System.currentTimeMillis());
                return true;
            }
        }
    }

    public void endpointIsAlive(final String endpoint) {
        LOGGER.info("endpointIsAlive");
        synchronized (CoapServer.class) {
            if (endpoints.containsKey(endpoint)) {
                endpoints.put(endpoint, System.currentTimeMillis());
            }
        }
    }

    /**
     * Sends a reply to a packet using the socket.
     *
     * @param replyPacket the packet to use to reply.
     */

    public void sendReply(final DatagramPacket replyPacket) {
        synchronized (CoapServer.class) {
            try {
                socket.send(replyPacket);
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Sends a reply to a packet using the socket.
     *
     * @param buf           the bytes to send as a reply
     * @param socketAddress the address of the udp client
     */
    public void sendReply(final byte[] buf, final SocketAddress socketAddress) {
        final DatagramPacket replyPacket = new DatagramPacket(buf, buf.length);
        replyPacket.setData(buf);
        replyPacket.setSocketAddress(socketAddress);

        synchronized (CoapServer.class) {
            try {
                socket.send(replyPacket);
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Adds the req to the list of active requests.
     *
     * @param address  the address from which the req originated.
     * @param req      the req message.
     * @param sAddress the socket address of the sender if any
     */
    public void addRequest(final String address, final Message req, final SocketAddress sAddress, final boolean hasQuery) {
        synchronized (CoapServer.class) {
            if (!activeRequests.containsKey(address)) {
                activeRequests.put(address, new ArrayList<ActiveRequest>());
            }
            activeRequests.get(address).add(new ActiveRequest(req.getUriPath(), req.getMID(), req.getTokenString(), address, sAddress, hasQuery));
        }
    }

    /**
     * Matches a message to a previously received request.
     *
     * @param response The response received.
     * @return The URI of the request or null.
     */

    public String matchResponse(final String address, final Message response) {
        synchronized (CoapServer.class) {
            if (activeRequests.isEmpty()) {
                LOGGER.info("no active request");
                return null;
            }
            LOGGER.debug(response.getPayloadString());
            LOGGER.debug(response.hasOption(OptionNumberRegistry.TOKEN));
            LOGGER.debug(response.getOptionCount());
            if (!activeRequests.containsKey(address)) return null;
            for (ActiveRequest activeRequest : activeRequests.get(address)) {

                LOGGER.debug(activeRequest.getToken() + "--" + response.getTokenString());

                if ((response.hasOption(OptionNumberRegistry.TOKEN))
                        && (response.getTokenString().equals(activeRequest.getToken()))) {
                    LOGGER.info("Found By Token " + response.getTokenString() + "==" + activeRequest.getToken());
                    respondToUDP(response, activeRequest, address);
                    if (activeRequest.hasQuery()) {
                        return null;
                    } else {
                        return activeRequest.getUriPath();
                    }
                }
                LOGGER.info(activeRequest.getMid() + "--" + response.getMID());
                if (response.getMID() == activeRequest.getMid()) {
                    String retVal = activeRequest.getUriPath();
                    if (activeRequest.hasQuery()) {
                        retVal = null;
                    }
                    LOGGER.info("Found By MID");
                    respondToUDP(response, activeRequest, address);
                    activeRequests.get(address).remove(activeRequest);

                    return retVal;
                }
            }
        }

        return null;
    }

    /**
     * Responds to a UDP request using the packet received from the device.
     *
     * @param response      the response received.
     * @param activeRequest the request for the response.
     * @param address
     */
    private void respondToUDP(final Message response, final ActiveRequest activeRequest, String address) {
        if (activeRequest.getSocketAddress() != null) {
            try {
                response.setURI("/" + testbedPrefix + address + activeRequest.getUriPath());
                LOGGER.info("/" + testbedPrefix + address + activeRequest.getUriPath());
                LOGGER.info("Sending Response to: " + activeRequest.getSocketAddress());
                socket.send(new DatagramPacket(response.toByteArray(), response.toByteArray().length, activeRequest.getSocketAddress()));
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Testing Main function.
     *
     * @param args command line arguments.
     */
    public static void main(final String[] args) {
        CoapServer.getInstance();
    }

    /**
     * Sends a payload to a device and add the message id at the beginning of the message.
     *
     * @param data    the data to send.
     * @param nodeUrn the destination device.
     */
    public void sendRequest(final byte[] data, final String nodeUrn) {
        final int[] bytes = new int[data.length + 1];
        bytes[0] = 51;
        for (int i = 0; i < data.length; i++) {
            final short read = (short) ((short) data[i] & 0xff);
            bytes[i + 1] = read;
        }

        final StringBuilder messageBinary = new StringBuilder("Requesting[Bytes]:");
        for (int i = 0; i < data.length + 1; i++) {
            messageBinary.append(bytes[i]).append("|");
        }
        LOGGER.info(messageBinary.toString());

        final int[] macAddress = Converter.getInstance().addressToInteger(nodeUrn);

        final XBeeAddress16 address16 = new XBeeAddress16(macAddress[0], macAddress[1]);


        LOGGER.info("sending to device");
        try {
            XBeeRadio.getInstance().send(address16, 112, bytes);
        } catch (Exception e) {//NOPMD
            LOGGER.error(e.getMessage(), e);
        }


    }

    /**
     * Send a COAP ACK to a device containing a single mid.
     *
     * @param mid     the mid to ack.
     * @param nodeUrn the destination device.
     */
    public void sendAck(final int mid, final String nodeUrn) {
        final Message ack = new Message(Message.messageType.ACK, 0);
        ack.setMID(mid);

        final byte[] data = ack.toByteArray();
        final int[] bytes = new int[data.length + 1];
        bytes[0] = 51;
        for (int i = 0; i < data.length; i++) {
            final short read = (short) ((short) data[i] & 0xff);
            bytes[i + 1] = read;
        }
        final int[] macAddress = Converter.getInstance().addressToInteger(nodeUrn);
        final XBeeAddress16 address16 = new XBeeAddress16(macAddress[0], macAddress[1]);
        LOGGER.info("Sending Ack to " + nodeUrn);
        try {
            XBeeRadio.getInstance().send(address16, 112, bytes);
        } catch (Exception e) {//NOPMD
            LOGGER.error(e.getMessage(), e);
        }
    }

    public void registerForResource(final String capability, final String address) {
        {
            URI uri = null;
            try {
                uri = new URI(new StringBuilder().append("/").append(capability).toString());
            } catch (URISyntaxException e) {
                LOGGER.error(e.getLocalizedMessage(), e);
            }
            final Request request = new Request(CodeRegistry.METHOD_GET, false);
            request.setMID(mid.nextInt() % 65535);
            request.setURI(uri);
//            List<Option> uriPath = Option.split(OptionNumberRegistry.URI_PATH, uri.getPath(), "/");
//            request.setOptions(OptionNumberRegistry.URI_PATH, uriPath);
            request.setOption(new Option(0, OptionNumberRegistry.OBSERVE));
            request.setToken(TokenManager.getInstance().acquireToken());

            addRequest(address, request, null, false);
            sendRequest(request.toByteArray(), address);
        }

    }
}
