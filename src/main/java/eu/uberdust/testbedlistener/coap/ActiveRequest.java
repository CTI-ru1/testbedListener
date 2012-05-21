package eu.uberdust.testbedlistener.coap;

import org.apache.log4j.Logger;

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

    public ActiveRequest(final String uriPath, final int mid, final String token, final String host) {
        LOGGER.debug("new ActiveRequest");
        this.uriPath = uriPath;
        this.mid = mid;
        this.token = token;
        this.host = host;
    }
}
