package eu.uberdust.testbedlistener.coap.internal.handler;

import ch.ethz.inf.vs.californium.coap.Message;
import eu.uberdust.testbedlistener.coap.CoapServer;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: amaxilatis
 * Date: 6/13/13
 * Time: 4:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class ArduinoGatewayRequestHandler implements InternalRequestHandler {
    public ArduinoGatewayRequestHandler(final HashMap<String, InternalRequestHandler> internalRequestHandlers) {
        final InternalRequestHandler thisObject = this;
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Map<String, HashMap<String, Long>> gateways = CoapServer.getInstance().getArduinoGateways();
                for (final String testbedUrn : gateways.keySet()) {
                    for (final String gatewayDevice : gateways.get(testbedUrn).keySet()) {
                        internalRequestHandlers.put("/gateways/" + testbedUrn + "/" + gatewayDevice, thisObject);
                    }
                }
            }
        }, 1000, 10000);
    }

    @Override
    public void handle(Message udpRequest, Message response) {
        StringBuilder payload = new StringBuilder("");
        Map<String, HashMap<String, Long>> gways = CoapServer.getInstance().getArduinoGateways();

        for (String gateway : gways.keySet()) {
            payload.append(gateway);
            payload.append("\n");
            for (String deviceId : gways.get(gateway).keySet()) {
                payload.append("\t");
                payload.append(deviceId).append(" @ ").append(new Date(gways.get(gateway).get(deviceId)).toString()).append("\n");
            }
        }
        response.setPayload(payload.toString());
    }
}
