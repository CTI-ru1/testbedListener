package eu.uberdust.testbedlistener.coap;

import org.apache.log4j.Logger;

import java.net.SocketAddress;

/**
 * Contains all information about an active request from a source.
 */
public class ActiveRequest {

    /**
     * LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(ActiveRequest.class);

    private transient final String uriPath;
    private transient final int mid;
    private transient final String token;
    private transient final String host;
    private transient final SocketAddress socketAddress;

    public String getUriPath() {
        return uriPath;
    }

    public int getMid() {
        return mid;
    }

    public String getToken() {
        return token;
    }

    public String getHost() {
        return host;
    }

    public SocketAddress getSocketAddress() {
        return socketAddress;
    }

    public ActiveRequest(final String uriPath, final int mid, final String token, final String host, SocketAddress socketAddress) {
        LOGGER.debug("new ActiveRequest");
        this.uriPath = uriPath;
        this.mid = mid;
        this.token = token;
        this.host = host;
        this.socketAddress = socketAddress;
    }
}
