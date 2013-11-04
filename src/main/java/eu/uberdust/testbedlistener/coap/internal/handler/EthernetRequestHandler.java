package eu.uberdust.testbedlistener.coap.internal.handler;

import ch.ethz.inf.vs.californium.coap.Message;
import eu.uberdust.testbedlistener.coap.CoapServer;
import eu.uberdust.testbedlistener.coap.EthernetSupport;
import org.apache.log4j.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: amaxilatis
 * Date: 6/13/13
 * Time: 3:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class EthernetRequestHandler implements InternalRequestHandlerInterface {
    private static final Logger LOGGER = Logger.getLogger(EthernetRequestHandler.class);

    @Override
    public void handle(Message udpRequest, Message response) {
        StringBuilder payload = new StringBuilder("");
        LOGGER.info("request");
        if (udpRequest.getCode() == 2) {
            LOGGER.info("request=2");
            try {
                new EthernetSupport(CoapServer.getInstance().getEthernetUDPHandler(), udpRequest).start();
            } catch (Exception e) {
                LOGGER.error("request", e);
            }
            LOGGER.info("request=2+");
        } else {
            for (CoapServer.TokenItem item : CoapServer.getInstance().getObservers()) {
                payload.append(item.getBytes()).append(" > ").append(item.getPath()).append("\n");
            }
        }
        response.setPayload(payload.toString());
    }
}
