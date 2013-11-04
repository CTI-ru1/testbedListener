package eu.uberdust.testbedlistener.coap.internal.handler;

import ch.ethz.inf.vs.californium.coap.*;
import eu.uberdust.testbedlistener.coap.Cache;
import eu.uberdust.testbedlistener.coap.CacheHandler;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Internal Request Handler used to show the contents of the Cache for all the registered IoT devices.
 *
 * @author Dimitrios Amaxilatis
 * @date 13/6/13
 */
public class CacheRequestHandler implements InternalRequestHandlerInterface {
    @Override
    public void handle(Message udpRequest, Message response) {
        if (udpRequest.getCode() == CodeRegistry.METHOD_GET) {
            StringBuilder payload = new StringBuilder("");
            final Map<String, Map<String, Cache>> cache = CacheHandler.getInstance().getCache();
            payload.append("Host\tPath\tValue\tTimestamp\t\t\tAge\tObserves lost\n");
            List<Option> uriHostsOptions = udpRequest.getOptions(OptionNumberRegistry.URI_HOST);
            String uriHost = "";
            if (uriHostsOptions.size() > 0) {
                uriHost = uriHostsOptions.get(0).getStringValue();
            }
            for (String device : cache.keySet()) {
                if (!"".equals(uriHost) && !device.equals(uriHost)) {
                    continue;
                }
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
