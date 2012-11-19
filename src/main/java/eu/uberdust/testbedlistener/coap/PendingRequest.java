package eu.uberdust.testbedlistener.coap;

import org.apache.log4j.Logger;

import java.net.SocketAddress;

/**
 * Contains all information about an active request from a source.
 */
public class PendingRequest {

    /**
     * LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(PendingRequest.class);
    /**
     * MID of the CoAP Message.
     */
    private transient int mid;
    /**
     * Token of the CoAP message (if Exists).
     */
    private transient final String token;
    /**
     * URIHost of the CoAP Message.
     */
    private transient final String uriHost;
    /**
     * SocketAddress of the Incoming CoAP message Request.
     */
    private transient final SocketAddress socketAddress;
    /**
     * True for Observe CoAP Messages.
     */
    private transient final boolean isObserve;
    /**
     * True for ConfirmAble CoAP Messages.
     */
    private transient final boolean isConfirm;
    private transient boolean isFirst;
    private transient String uriPath;

    /**
     * @return the uri path of the request
     */
    public String getUriHost() {
        return uriHost;
    }

    /**
     * Get the id of the message.
     *
     * @return the id of the message.
     */
    public int getMid() {
        return mid;
    }

    /**
     * Get the token of the query if it exists.
     *
     * @return the token of the query if it exists.
     */
    public String getToken() {
        return token;
    }

    /**
     * Get the socket address fo the sender.
     *
     * @return the socket address fo the sender.
     */
    public SocketAddress getSocketAddress() {
        return socketAddress;
    }

    /**
     * Get true if the request was a observe.
     *
     * @return true if the request was a observe.
     */
    public boolean isObserve() {
        return isObserve;
    }

    /**
     * Get true if the request was a confirm.
     *
     * @return true if the request was a confirm.
     */
    public boolean isConfirm() {
        return isConfirm;
    }

    /**
     * Constructor.
     *
     * @param uriHost       the uri path
     * @param mid           the message id
     * @param token         the message token
     * @param uriPath
     * @param socketAddress the socket address of the sender
     * @param observe       true if it is a observe
     * @param confirm       true if it is a confirm
     */
    public PendingRequest(final String uriHost, final int mid, final String token, String uriPath, final SocketAddress socketAddress,
                          final boolean observe, final boolean confirm) {
        this.uriHost = uriHost;
        this.mid = mid;
        this.token = token;
        this.uriPath = uriPath;
        this.socketAddress = socketAddress;
        this.isObserve = observe;
        this.isConfirm = confirm;
        this.isFirst = true;
    }

    public boolean isFirst() {
        return isFirst;
    }

    public void setFirst(boolean first) {
        isFirst = first;
    }

    public void setMID(final int MID) {
        this.mid = MID;
    }

    public String getUriPath() {
        return uriPath;
    }
}
