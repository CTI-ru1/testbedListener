package eu.uberdust.testbedlistener.datacollector.notify;

import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import eu.uberdust.testbedlistener.coap.ResourceCache;
import eu.uberdust.testbedlistener.datacollector.collector.CollectorMqtt;

/**
 * Created by IntelliJ IDEA.
 * User: amaxilatis
 * Date: 11/29/12
 * Time: 3:18 PM
 */
public class CacheNotify implements Runnable {

    private transient final Message response;
    private transient final String resourceURIString;
    private transient final CollectorMqtt collector;


    public CacheNotify(final String resourceURIString, final Message response, final CollectorMqtt collector) {
        System.out.println("saving to cache " + resourceURIString);
        this.response = response;
        this.resourceURIString = resourceURIString;
        this.collector = collector;
    }

    @Override
    public void run() {
        int maxAge;
        if (response.hasOption(OptionNumberRegistry.MAX_AGE)) {
            maxAge = response.getMaxAge();
        } else {
            maxAge = 60;
        }
        ResourceCache.getInstance().setValue(resourceURIString, maxAge, response.getContentType(), response.getPayloadString(),collector);
    }
}
