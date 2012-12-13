package eu.uberdust.testbedlistener.coap;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import org.apache.log4j.Logger;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: amaxilatis
 * Date: 10/30/12
 * Time: 11:56 AM
 * To change this template use File | Settings | File Templates.
 */
public class PendingRequestHandler {
    /**
     * LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(PendingRequestHandler.class);

    /**
     * the only instance of PendingRequestHandler.
     */
    private static PendingRequestHandler instance = null;

    /**
     * List of All pending Requests.
     */
    List<PendingRequest> pendingRequestList;

    /**
     * Constructor.
     */
    public PendingRequestHandler() {
        pendingRequestList = new ArrayList<PendingRequest>();
    }

    /**
     * Singleton getInstance.
     *
     * @return the only instance of PendingRequestHandler.
     */
    public static PendingRequestHandler getInstance() {
        synchronized (PendingRequestHandler.class) {
            if (instance == null) {
                instance = new PendingRequestHandler();
            }
            return instance;
        }
    }

    /**
     * Adds a new Request to the list of Pending Request to be served.
     *
     * @param URIHost       the destinantion URIHost.
     * @param coapRequest   the CoAPrequest.
     * @param socketAddress the Socket Address the request originated from.
     */
    public void addRequest(final String URIHost, final Message coapRequest, final SocketAddress socketAddress) {
        if (coapRequest.hasOption(OptionNumberRegistry.TOKEN)) {
            for (PendingRequest pendingRequest : pendingRequestList) {
                if (pendingRequest.getToken().equals(coapRequest.getTokenString())) {
                    pendingRequest.setMID(coapRequest.getMID());
                    pendingRequest.setFirst(true);
                    return;
                }
            }
        }
        pendingRequestList.add(new PendingRequest(
                URIHost,
                coapRequest.getMID(),
                coapRequest.getTokenString(),
                coapRequest.getUriPath(),
                socketAddress,
                coapRequest.hasOption(OptionNumberRegistry.OBSERVE),
                coapRequest.isConfirmable()));
        LOGGER.info("added new pending ");
    }

    public synchronized SocketAddress isPending(final Message response) {
        final int messageMID = response.getMID();
        final String messageTOKEN = response.getTokenString();

        LOGGER.info("Looking for " + messageMID + "/" + messageTOKEN);
        SocketAddress socket = null;

        synchronized (PendingRequestHandler.class) {
            if (pendingRequestList.isEmpty()) {
                return null;
            }
            PendingRequest mp = null;
            for (PendingRequest pendingRequest : pendingRequestList) {
                if (response.hasOption(OptionNumberRegistry.OBSERVE)) {
                    if ((messageTOKEN.equals(pendingRequest.getToken()))
                            && ((messageMID != pendingRequest.getMid()) || pendingRequest.isFirst())) {
                        LOGGER.debug("Looking for " + messageMID + "/" + messageTOKEN + "@" + pendingRequest.getMid() + "/" + pendingRequest.getToken());
                        mp = pendingRequest;
                        LOGGER.debug("Updating message id to " + response.getMID());
                        pendingRequest.setFirst(false);
                        pendingRequest.setMID(response.getMID());
                        socket = pendingRequest.getSocketAddress();
                        break;
                    }
                } else {
                    LOGGER.debug("checking " + pendingRequest.getMid());
                    if (messageMID == pendingRequest.getMid()) {
                        LOGGER.debug("Looking for " + messageMID + "/" + messageTOKEN + "@" + pendingRequest.getMid() + "/" + pendingRequest.getToken());
                        if (!pendingRequest.isObserve()) {
                            mp = pendingRequest;
                        }
                        socket = pendingRequest.getSocketAddress();
                        break;
                    }
                }

            }
            if (response.getCode() == CodeRegistry.RESP_CONTENT || response.getCode() == CodeRegistry.RESP_CHANGED || response.getCode() == CodeRegistry.RESP_CREATED) {
                int maxAge;
                if (response.hasOption(OptionNumberRegistry.MAX_AGE)) {
                    maxAge = response.getMaxAge();
                } else {
                    maxAge = 120;
                }
//                try {
//                    CacheHandler.getInstance().setValue(mp.getUriHost(), mp.getUriPath(), maxAge, response.getContentType(), response.getPayloadString());
//                } catch (NullPointerException e) {
//                    LOGGER.error(e, e);
//                }
            }
            if (mp != null && !response.hasOption(OptionNumberRegistry.OBSERVE)) {
                pendingRequestList.remove(mp);
            }
        }
        //NotFound return false
        return socket;

    }

    public String matchMID4Host(final Message udpRequest) {
        final int messageMID = udpRequest.getMID();

        LOGGER.info("Looking for " + messageMID);

        synchronized (PendingRequestHandler.class) {
            if (pendingRequestList.isEmpty()) {
                return "";
            }
            for (PendingRequest pendingRequest : pendingRequestList) {
                LOGGER.info("Looking for " + messageMID + "@" + pendingRequest.getMid());
                if (messageMID == pendingRequest.getMid()) {
                    return pendingRequest.getUriHost();
                }
            }
        }
        return "";
    }

    public List<PendingRequest> getPendingRequestList() {
        return pendingRequestList;
    }

    public void removeRequest(Message udpRequest) {
        final int messageMID = udpRequest.getMID();

        LOGGER.info("Looking for " + messageMID);

        synchronized (PendingRequestHandler.class) {
            if (pendingRequestList.isEmpty()) {
                return;
            }
            for (PendingRequest pendingRequest : pendingRequestList) {
                LOGGER.info("Looking for " + messageMID + "@" + pendingRequest.getMid());
                if (messageMID == pendingRequest.getMid()) {
                    pendingRequestList.remove(pendingRequest);
                }
            }
        }
    }
}
