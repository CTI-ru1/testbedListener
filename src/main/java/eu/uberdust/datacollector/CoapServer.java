package eu.uberdust.datacollector;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: amaxilatis
 * Date: 5/19/12
 * Time: 6:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class CoapServer {

    private static CoapServer instance = null;
    private Map<String, Integer> endpoints;
    private DatagramSocket socket;

    public CoapServer() {
        this.endpoints = new HashMap<String, Integer>();
        try {
            socket = new DatagramSocket(5683);
        } catch (SocketException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        UDPhandler thread = new UDPhandler(socket);
        thread.start();
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
}
