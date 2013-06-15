package eu.uberdust.testbedlistener.coap.internal.handler;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Message;
import eu.uberdust.testbedlistener.coap.ActiveRequest;
import eu.uberdust.testbedlistener.coap.CoapServer;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: amaxilatis
 * Date: 6/13/13
 * Time: 3:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class ActiveRequestsRequestHandler implements InternalRequestHandler {
    @Override
    public void handle(Message udpRequest, Message response) {
        if (udpRequest.getCode() == CodeRegistry.METHOD_GET) {
            StringBuilder payload = new StringBuilder("");
            final Map<Integer, ActiveRequest> activeRequests = CoapServer.getInstance().getActiveRequestsMID();
            for (int key : activeRequests.keySet()) {
                final ActiveRequest activeRequest = activeRequests.get(key);
                payload.append(activeRequest.getHost()).append("\t").append(activeRequest.getToken()).append("\t").append(activeRequest.getMid()).append("\t").append(activeRequest.getUriPath()).append("\t").append(activeRequest.getTimestamp()).append("\t").append(activeRequest.getMid()).append("\t").append(activeRequest.getCount()).append("\n");
            }
            payload.append("\n");
            final Map<String, ActiveRequest> activeRequests2 = CoapServer.getInstance().getActiveRequestsTOKEN();
            for (String key : activeRequests2.keySet()) {
                final ActiveRequest activeRequest = activeRequests2.get(key);
                payload.append(activeRequest.getHost()).append("\t").append(activeRequest.getToken()).append("\t").append(activeRequest.getMid()).append("\t").append(activeRequest.getUriPath()).append("\t").append(activeRequest.getTimestamp()).append("\t").append(activeRequest.getMid()).append("\t").append(activeRequest.getCount()).append("\n");
            }
            response.setContentType(MediaTypeRegistry.TEXT_PLAIN);
            response.setPayload(payload.toString());
        } else {
            response.setCode(CodeRegistry.RESP_METHOD_NOT_ALLOWED);
        }
    }
}
