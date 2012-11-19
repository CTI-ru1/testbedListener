package eu.uberdust.testbedlistener.coap;

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
        for (PendingRequest pendingRequest : pendingRequestList) {
            if (pendingRequest.getToken().equals(coapRequest.getTokenString())) {
                pendingRequest.setMID(coapRequest.getMID());
                pendingRequest.setFirst(true);
                return;
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

    }

    public synchronized SocketAddress isPending(final Message response) {
        final int messageMID = response.getMID();
        final String messageTOKEN = response.getTokenString();
        int observeCount = 0;
        if (response.hasOption(OptionNumberRegistry.OBSERVE)) {
            observeCount = response.getOptions(OptionNumberRegistry.OBSERVE).get(0).getIntValue();
        }
        LOGGER.debug("Looking for " + messageMID + "/" + messageTOKEN + " OBSC " + observeCount);
        SocketAddress socket = null;

        synchronized (PendingRequestHandler.class) {
            if (pendingRequestList.isEmpty()) {
                LOGGER.error("pendig is empty");
                return null;
            }
            PendingRequest mp = null;
            for (PendingRequest pendingRequest : pendingRequestList) {
                if (response.hasOption(OptionNumberRegistry.OBSERVE)) {
                    if ((messageTOKEN.equals(pendingRequest.getToken()))
                            && ((messageMID != pendingRequest.getMid()) || pendingRequest.isFirst())) {
                        LOGGER.debug("Looking for " + messageMID + "/" + messageTOKEN + "@" + pendingRequest.getMid() + "/" + pendingRequest.getToken() + " OBSC " + observeCount);
                        mp = null;
                        LOGGER.debug("Updating message id to " + response.getMID());
                        pendingRequest.setFirst(false);
                        pendingRequest.setMID(response.getMID());
                        socket = pendingRequest.getSocketAddress();
                        break;
                    }
                } else {
                    LOGGER.debug("checking " + pendingRequest.getMid());
                    if (messageMID == pendingRequest.getMid()) {
                        LOGGER.debug("Looking for " + messageMID + "/" + messageTOKEN + "@" + pendingRequest.getMid() + "/" + pendingRequest.getToken() + " OBSC " + observeCount);
                        if (!pendingRequest.isObserve()) {
                            mp = pendingRequest;
                        }
                        socket = pendingRequest.getSocketAddress();
                        break;
                    }
                }

            }
            if (response.getCode() == 69 || response.getCode() == 68 || response.getCode() == 65) {
                CacheHandler.getInstance().setValue(mp.getUriHost(), mp.getUriPath(), response.getPayloadString());
            }
            if (mp != null) {
                pendingRequestList.remove(mp);
            }
        }
        //NotFound return false
        return socket;

    }

    public void respond(final SocketAddress originSocketAddress, final Message response) {

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
