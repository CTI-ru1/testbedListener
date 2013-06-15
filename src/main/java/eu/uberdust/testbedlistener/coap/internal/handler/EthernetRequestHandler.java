package eu.uberdust.testbedlistener.coap.internal.handler;

import ch.ethz.inf.vs.californium.coap.Message;
import eu.uberdust.testbedlistener.coap.CoapServer;
import eu.uberdust.testbedlistener.coap.EthernetSupport;

/**
 * Created with IntelliJ IDEA.
 * User: amaxilatis
 * Date: 6/13/13
 * Time: 3:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class EthernetRequestHandler implements InternalRequestHandler {
    @Override
    public void handle(Message udpRequest, Message response) {
        StringBuilder payload = new StringBuilder("");
        if (udpRequest.getCode() == 2) {
            (new EthernetSupport(CoapServer.getInstance().getEthernetUDPHandler(), udpRequest.getPayloadString())).start();
        } else {
            for (CoapServer.TokenItem item : CoapServer.getInstance().getObservers()) {
                payload.append(item.getBytes()).append(" > ").append(item.getPath()).append("\n");
            }
        }
        response.setPayload(payload.toString());
    }
}
