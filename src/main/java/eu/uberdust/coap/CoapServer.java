package eu.uberdust.coap;

import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import eu.uberdust.coap.udp.UDPhandler;
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
    private Map<String, Integer> endpoints;
    private DatagramSocket socket;
    private List<ActiveRequests> activeRequests;

    public CoapServer() {
        this.endpoints = new HashMap<String, Integer>();
        this.activeRequests = new ArrayList<ActiveRequests>();
        try {
            socket = new DatagramSocket(5683);
        } catch (SocketException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        UDPhandler thread = new UDPhandler(socket);
        thread.start();
        LOGGER.info("started CoapServer");
    }

    public static synchronized CoapServer getInstance() {
        if (instance == null) {
            instance = new CoapServer();
        }
        return instance;
    }

    public synchronized boolean registerEndpoint(final String endpoint) {
        if (endpoints.containsKey(endpoint)) {
            return false;
        } else {
            endpoints.put(endpoint, 1);
            return true;
        }
    }


    public static void main(String[] args) {
        CoapServer.getInstance();
    }

    public synchronized void sendReply(DatagramPacket replyPacket) {
        try {
            socket.send(replyPacket);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public synchronized void addRequest(String address, Message request) {
        activeRequests.add(new ActiveRequests(request.getUriPath(), request.getMID(), request.getTokenString(), address));
    }

    public synchronized String matchResponse(String address, Message response) {
        if (activeRequests.isEmpty()) LOGGER.info("no active request");
        LOGGER.info(response.getPayloadString());
        LOGGER.info(response.hasOption(OptionNumberRegistry.TOKEN));
        LOGGER.info(response.getOptionCount());
        if (response.getTokenString().isEmpty()) {
            for (ActiveRequests activeRequest : activeRequests) {
                LOGGER.info(activeRequest.getMid() + "--" + response.getMID());
                if (response.getMID() == activeRequest.getMid()) {
                    final String retVal = activeRequest.getUriPATH();
                    activeRequests.remove(activeRequest);
                    return retVal;
                }
            }
        } else {
            for (ActiveRequests activeRequest : activeRequests) {
                LOGGER.info(activeRequest.getToken() + "--" + response.getTokenString());
                if (response.getTokenString().equals(activeRequest.getToken())) {
                    LOGGER.info("found");
                    return activeRequest.getUriPATH();
                }
            }
        }
        return null;
    }
}
