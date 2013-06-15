package eu.uberdust.testbedlistener.coap.internal.handler;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Message;
import eu.uberdust.testbedlistener.coap.CoapServer;

import java.util.Date;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: amaxilatis
 * Date: 6/13/13
 * Time: 3:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class EndpointsRequestHandler implements InternalRequestHandler {
    @Override
    public void handle(Message udpRequest, Message response) {
        if (udpRequest.getCode() == CodeRegistry.METHOD_GET) {
            StringBuilder payload = new StringBuilder("");
            final Map<String, Map<String, Long>> endpoints = CoapServer.getInstance().getEndpoints();
            for (String endpoint : endpoints.keySet()) {
                for (String uripath : endpoints.get(endpoint).keySet()) {
                    payload.append(new Date(endpoints.get(endpoint).get(uripath)));
                    payload.append(" - ");
                    if (endpoint.length() == 3) {
                        payload.append(" ");
                    }
                    payload.append(endpoint);
                    payload.append("/").append(uripath);
                    payload.append("\n");
                }
            }
            response.setContentType(MediaTypeRegistry.TEXT_PLAIN);
            response.setPayload(payload.toString());
        } else {
            response.setCode(CodeRegistry.RESP_METHOD_NOT_ALLOWED);
        }
    }
}
