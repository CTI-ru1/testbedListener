package eu.uberdust.testbedlistener.coap.internal.handler;

import ch.ethz.inf.vs.californium.coap.Message;
import eu.uberdust.testbedlistener.coap.GatewayManager;

/**
 * Created with IntelliJ IDEA.
 * User: amaxilatis
 * Date: 6/13/13
 * Time: 3:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class RoutesRequestHandler implements InternalRequestHandler {
    @Override
    public void handle(Message udpRequest, Message response) {
        StringBuilder payload = new StringBuilder("");
        if (udpRequest.getCode() == 2) {
            String myPayload = udpRequest.getPayloadString();
            if (myPayload.contains("->")) {
                final String dest = myPayload.split("->")[0];
                final String source = myPayload.split("->")[1];
                GatewayManager.getInstance().setGateway(dest, source);
            }
        }
        for (String key : GatewayManager.getInstance().getGateways().keySet()) {
            payload.append(key).append("->").append(GatewayManager.getInstance().getGateway(key)).append("\n");
        }
        response.setPayload(payload.toString());
    }
}
