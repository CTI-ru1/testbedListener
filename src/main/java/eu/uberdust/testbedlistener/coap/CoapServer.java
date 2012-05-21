package eu.uberdust.testbedlistener.coap;

import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import eu.uberdust.testbedlistener.coap.udp.UDPhandler;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
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
     * Adds the request to the list of avctive requests.
     *
     * @param address the address from which the request originated.
     * @param request the request message.
     */
    public void addRequest(final String address, final Message request) {
        synchronized (CoapServer.class) {
            activeRequests.add(new ActiveRequest(request.getUriPath(), request.getMID(), request.getTokenString(), address));
        }
    }

    /**
     * Matches a message to a previously received request.
     *
     * @param response The response received.
     * @return The URI of the request or null.
     */
    public String matchResponse(final Message response) {
        synchronized (CoapServer.class) {
            if (activeRequests.isEmpty()) {
                LOGGER.info("no active request");
                return null;
            }
            LOGGER.info(response.getPayloadString());
            LOGGER.info(response.hasOption(OptionNumberRegistry.TOKEN));
            LOGGER.info(response.getOptionCount());
            if (response.getTokenString().isEmpty()) {
                for (ActiveRequest activeRequest : activeRequests) {
                    LOGGER.info(activeRequest.getMid() + "--" + response.getMID());
                    if (response.getMID() == activeRequest.getMid()) {
                        final String retVal = activeRequest.getUriPath();
                        activeRequests.remove(activeRequest);
                        return retVal;
                    }
                }
            } else {
                for (ActiveRequest activeRequest : activeRequests) {
                    LOGGER.info(activeRequest.getToken() + "--" + response.getTokenString());
                    if (response.getTokenString().equals(activeRequest.getToken())) {
                        LOGGER.info("found");
                        return activeRequest.getUriPath();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Testing Main function.
     *
     * @param args command line arguments.
     */
    public static void main(final String[] args) {
        CoapServer.getInstance();
    }
}
