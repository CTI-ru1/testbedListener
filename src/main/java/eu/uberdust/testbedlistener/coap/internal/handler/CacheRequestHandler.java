package eu.uberdust.testbedlistener.coap.internal.handler;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Message;
import eu.uberdust.testbedlistener.coap.Cache;
import eu.uberdust.testbedlistener.coap.CacheHandler;

import java.util.Date;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: amaxilatis
 * Date: 6/13/13
 * Time: 3:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class CacheRequestHandler implements InternalRequestHandler {
    @Override
    public void handle(Message udpRequest, Message response) {
        if (udpRequest.getCode() == CodeRegistry.METHOD_GET) {
            StringBuilder payload = new StringBuilder("");
            final Map<String, Map<String, Cache>> cache = CacheHandler.getInstance().getCache();
            payload.append("Host\tPath\tValue\tTimestamp\t\t\tAge\tObserves lost\n");
            for (String device : cache.keySet()) {
//                if (!"".equals(uriHost) && !device.equals(uriHost)) {
//                    continue;
//                }
                for (String uriPath : cache.get(device).keySet()) {
                    final Cache pair = cache.get(device).get(uriPath);
                    final long timediff = (System.currentTimeMillis() - pair.getTimestamp()) / 1000;
                    payload.append(device).append("\t").append(uriPath).append("\t").append(pair.getValue()).append("\t").append(new Date(pair.getTimestamp())).append("\t").append(timediff).append("sec").append(timediff > pair.getMaxAge() ? " *" : "").append("\t").append(pair.getLostCounter()).append("\n");
                }
            }
            response.setContentType(MediaTypeRegistry.TEXT_PLAIN);
            response.setPayload(payload.toString());
        } else if (udpRequest.getCode() == CodeRegistry.METHOD_DELETE) {
            StringBuilder payload = new StringBuilder("");
            CacheHandler.getInstance().clearCache();
            payload.append("Cache cleared");
            response.setContentType(MediaTypeRegistry.TEXT_PLAIN);
            response.setCode(CodeRegistry.RESP_CHANGED);
            response.setPayload(payload.toString());
        } else {

            response.setCode(CodeRegistry.RESP_METHOD_NOT_ALLOWED);
        }

    }
}
