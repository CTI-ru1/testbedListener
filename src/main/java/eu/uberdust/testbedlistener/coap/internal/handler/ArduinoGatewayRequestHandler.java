package eu.uberdust.testbedlistener.coap.internal.handler;

import ch.ethz.inf.vs.californium.coap.Message;
import eu.uberdust.testbedlistener.coap.CoapServer;

import java.util.Date;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: amaxilatis
 * Date: 6/13/13
 * Time: 4:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class ArduinoGatewayRequestHandler implements InternalRequestHandler {
    @Override
    public void handle(Message udpRequest, Message response) {
        StringBuilder payload= new StringBuilder("");
        Map<String, Long> gways = CoapServer.getInstance().getArduinoGateways();
        for (String gateway : gways.keySet()) {
            payload.append(gateway).append("@").append(new Date(gways.get(gateway)).toString()).append("\n");
        }

        response.setPayload(payload.toString());
    }
}
