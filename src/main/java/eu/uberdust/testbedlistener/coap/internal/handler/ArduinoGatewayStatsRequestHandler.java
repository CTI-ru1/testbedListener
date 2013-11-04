package eu.uberdust.testbedlistener.coap.internal.handler;

import ch.ethz.inf.vs.californium.coap.Message;
import eu.uberdust.testbedlistener.coap.CoapServer;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: amaxilatis
 * Date: 6/13/13
 * Time: 4:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class ArduinoGatewayStatsRequestHandler implements InternalRequestHandler {
    @Override
    public void handle(final Message udpRequest,final Message response) {
        final StringBuilder payload = new StringBuilder("");

        final Map<String, Map<String, Map<String, String>>> gways = CoapServer.getInstance().getArduinoGatewayStats();
        for (final String gateway : gways.keySet()) {
            payload.append(gateway);
            payload.append("\n");
            for (final String deviceId : gways.get(gateway).keySet()) {
                for (String key : gways.get(gateway).get(deviceId).keySet()) {
                    payload.append("\t");
                    payload.append(deviceId).append(" @ ").append(key).append("->").append(gways.get(gateway).get(deviceId).get(key)).append("\n");
                }
            }
        }
        response.setPayload(payload.toString());
    }
}
