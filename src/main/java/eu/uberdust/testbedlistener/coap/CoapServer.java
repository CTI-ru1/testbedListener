package eu.uberdust.testbedlistener.coap;

import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import com.rapplogic.xbee.api.XBeeAddress16;
import eu.mksense.XBeeRadio;
import eu.uberdust.testbedlistener.coap.udp.UDPhandler;
import eu.uberdust.testbedlistener.util.Converter;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: amaxilatis
 * Date: 5/19/12
 * Time: 6:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class CoapServer {
    private static final Logger LOGGER = Logger.getLogger(CoapServer.class);

    private static CoapServer instance = null;
    private transient final Map<String, Integer> endpoints;
    private transient final List<ActiveRequest> activeRequests;
    private transient DatagramSocket socket;

    /**
     * Constructor.
     */
    public CoapServer() {
        this.endpoints = new HashMap<String, Integer>();
        this.activeRequests = new ArrayList<ActiveRequest>();
        try {
            socket = new DatagramSocket(5683);
        } catch (SocketException e) {
            LOGGER.error(e.getMessage(), e);
        }

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
        synchronized (CoapServer.class) {
            if (endpoints.containsKey(endpoint)) {
                return false;
            } else {
                endpoints.put(endpoint, 1);
                return true;
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
    public void addRequest(final String address, final Message req, final SocketAddress sAddress, boolean hasQuery) {
        synchronized (CoapServer.class) {
            activeRequests.add(new ActiveRequest(req.getUriPath(), req.getMID(), req.getTokenString(), address, sAddress, hasQuery));
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
            LOGGER.info(response.getPayloadString());
            LOGGER.info(response.hasOption(OptionNumberRegistry.TOKEN));
            LOGGER.info(response.getOptionCount());
            for (ActiveRequest activeRequest : activeRequests) {
                if (!activeRequest.getHost().equals(address)) {
                    continue;
                }
                LOGGER.info(activeRequest.getToken() + "--" + response.getTokenString());
                if ((response.hasOption(OptionNumberRegistry.TOKEN))
                        && (response.getTokenString().equals(activeRequest.getToken()))) {
                    LOGGER.info("Found By Token " + response.getTokenString() + "==" + activeRequest.getToken());
                    respondToUDP(response, activeRequest);
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
                    respondToUDP(response, activeRequest);
                    activeRequests.remove(activeRequest);

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
     */
    private void respondToUDP(final Message response, final ActiveRequest activeRequest) {
        if (activeRequest.getSocketAddress() != null) {
            try {
                response.setURI("/urn:pspace:0x472" + activeRequest.getUriPath());
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
}
