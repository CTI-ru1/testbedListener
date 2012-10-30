package eu.uberdust.testbedlistener.coap;

import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
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
    public void addRequest(final String URIHost, final Request coapRequest, final SocketAddress socketAddress) {
        pendingRequestList.add(new PendingRequest(
                URIHost,
                coapRequest.getMID(),
                coapRequest.getTokenString(),
                socketAddress,
                coapRequest.hasOption(OptionNumberRegistry.BLOCK2),
                coapRequest.isConfirmable()));

    }

    public SocketAddress isPending(final Message response) {
        final int messageMID = response.getMID();
        final String messageTOKEN = response.getTokenString();
        LOGGER.info("Looking for " + messageMID + "/" + messageTOKEN);

        synchronized (PendingRequestHandler.class) {
            if (pendingRequestList.isEmpty()) {
                return null;
            }
            for (PendingRequest pendingRequest : pendingRequestList) {
                LOGGER.info("Looking for " + messageMID + "/" + messageTOKEN + "@" + pendingRequest.getMid() + "/" + pendingRequest.getToken());
                if ((response.hasOption(OptionNumberRegistry.BLOCK2))
                        && (messageTOKEN.equals(pendingRequest.getToken()))) {
                    return pendingRequest.getSocketAddress();
                }
                if (messageMID == pendingRequest.getMid()) {
                    return pendingRequest.getSocketAddress();
                }
            }
        }
        //NotFound return false
        return null;

    }

    public void respond(final SocketAddress originSocketAddress, final Message response) {

    }
}
