package eu.uberdust.testbedlistener.coap.internal.handler;

import ch.ethz.inf.vs.californium.coap.Message;
import eu.mksense.XBeeRadio;

/**
 * Created with IntelliJ IDEA.
 * User: amaxilatis
 * Date: 6/13/13
 * Time: 4:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class XbeeGatewayRequestHandler implements InternalRequestHandler {
    @Override
    public void handle(Message udpRequest, Message response) {
        StringBuilder payload = new StringBuilder("");
        try {
            XBeeRadio.getInstance().setChannel(12);
            payload.append(XBeeRadio.getInstance().getMyXbeeAddress().toString()).append("\n");
            payload.append(XBeeRadio.getInstance().getMyAddress()).append("\n");
        } catch (Exception e) {
            payload.append(e.getMessage()).append("\n");
        }
        response.setPayload(payload.toString());
    }
}
