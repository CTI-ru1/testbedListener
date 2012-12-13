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

    private transient String uriPath;
    private transient int mid;
    private transient String token;
    private transient final String host;
    private transient SocketAddress socketAddress;
    private transient boolean query;
    private transient int count;

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    private long timestamp;

    /**
     * @return the uri path of the request
     */
    public String getUriPath() {
        return uriPath;
    }

    /**
     * @return the id of the message
     */
    public int getMid() {
        return mid;
    }

    /**
     * @return the token of the query if it exists
     */
    public String getToken() {
        return token;
    }

    /**
     * @return the host concerning the query
     */
    public String getHost() {
        return host;
    }

    /**
     * @return the socket address fo the sender
     */
    public SocketAddress getSocketAddress() {
        return socketAddress;
    }

    /**
     * @return true if the request was a query
     */
    public boolean hasQuery() {
        return query;
    }

    public int getCount() {
        return count;
    }

    /**
     * Constructor.
     *
     * @param uriPath   the uri path
     * @param mid       the message id
     * @param token     the message token
     * @param host      the host of the message
     * @param query     true if it is a query
     * @param timestamp
     */
    public ActiveRequest(final String uriPath, final int mid, final String token, final String host,
                         final boolean query, long timestamp) {
        LOGGER.debug("new ActiveRequest");
        this.uriPath = uriPath;
        this.mid = mid;
        this.token = token;
        this.host = host;
        this.query = query;
        this.timestamp = timestamp;
        this.count = 0;
    }

    public void setMid(int mid) {
        this.mid = mid;
    }

    public void setUriPath(String uriPath) {
        this.uriPath = uriPath;
    }

    public void setQuery(boolean query) {
        this.query = query;
    }

    public void setToken(String tokenString) {
        this.token = tokenString;
    }

    public void incCount() {
        this.count++;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
