package eu.uberdust.testbedlistener.coap.internal.handler;

import ch.ethz.inf.vs.californium.coap.*;
import eu.uberdust.testbedlistener.coap.CacheEntry;
import eu.uberdust.testbedlistener.coap.ResourceCache;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Internal Request Handler used to show the contents of the CacheEntry for all the registered IoT devices.
 *
 * @author Dimitrios Amaxilatis
 * @date 13/6/13
 */
public class CacheRequestHandler implements InternalRequestHandlerInterface {
    @Override
    public void handle(Message udpRequest, Message response) {
        if (udpRequest.getCode() == CodeRegistry.METHOD_GET) {
            StringBuilder payload = new StringBuilder("");
            final Map<String, CacheEntry> cache = ResourceCache.getInstance().getCache();
            payload.append("Host\t\t\t\t\tValue\tTimestamp\t\t\tAge\tObserves lost\n");

            List<Option> uriHostsOptions = udpRequest.getOptions(OptionNumberRegistry.URI_HOST);
            String uriHost = "";
            if (uriHostsOptions.size() > 0) {
                uriHost = uriHostsOptions.get(0).getStringValue();
            }
            final Pattern pattern = Pattern.compile(uriHost);
            for (final String resourceURIString : cache.keySet()) {

                if (!"".equals(uriHost) && !(pattern.matcher(resourceURIString).find())) {
                    continue;
                }

                final CacheEntry pair = cache.get(resourceURIString);
                final long timediff = (System.currentTimeMillis() - pair.getTimestamp()) / 1000;
                payload.append(resourceURIString).append("\t").append("\t").append(pair.getValue()).append("\t").append(new Date(pair.getTimestamp())).append("\t").append(timediff).append("sec").append(timediff > pair.getMaxAge() ? " *" : "").append("\t").append(pair.getLostCounter()).append("\n");

            }
            response.setContentType(MediaTypeRegistry.TEXT_PLAIN);
            response.setPayload(payload.toString());
        } else if (udpRequest.getCode() == CodeRegistry.METHOD_DELETE) {
            StringBuilder payload = new StringBuilder("");
            ResourceCache.getInstance().clearCache();
            payload.append("Cache cleared");
            response.setContentType(MediaTypeRegistry.TEXT_PLAIN);
            response.setCode(CodeRegistry.RESP_CHANGED);
            response.setPayload(payload.toString());
        } else {

            response.setCode(CodeRegistry.RESP_METHOD_NOT_ALLOWED);
        }

    }
}
