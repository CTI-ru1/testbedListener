package eu.uberdust.testbedlistener.coap.internal.handler;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Message;
import eu.uberdust.testbedlistener.coap.PendingRequest;
import eu.uberdust.testbedlistener.coap.PendingRequestHandler;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: amaxilatis
 * Date: 6/13/13
 * Time: 3:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class PendingRequestsRequestHandler implements InternalRequestHandler {
    @Override
    public void handle(Message udpRequest, Message response) {

        if (udpRequest.getCode() == CodeRegistry.METHOD_GET) {
            StringBuilder payload = new StringBuilder("");
            final List<PendingRequest> pendingRequests = PendingRequestHandler.getInstance().getPendingRequestList();
            for (PendingRequest pendingRequest : pendingRequests) {
                payload.append(pendingRequest.getUriHost());
                if (pendingRequest.getUriHost().length() == 3) {
                    payload.append(" ");
                }
                payload.append(" - ");
                payload.append(pendingRequest.getSocketAddress()).append(" - ");
                payload.append(pendingRequest.getMid()).append(" - ");
                payload.append(pendingRequest.getToken()).append("\n");
            }
            response.setContentType(MediaTypeRegistry.TEXT_PLAIN);
            response.setPayload(payload.toString());
        } else {
            response.setCode(CodeRegistry.RESP_METHOD_NOT_ALLOWED);
        }
    }
}
