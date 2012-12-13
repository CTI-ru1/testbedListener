package eu.uberdust.testbedlistener.datacollector.notify;

import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import eu.uberdust.testbedlistener.coap.CacheHandler;
import org.apache.log4j.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: amaxilatis
 * Date: 11/29/12
 * Time: 3:18 PM
 */
public class CacheNotify implements Runnable {
    /**
     * LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(CacheNotify.class);

    private transient final Message response;
    private transient final String requestType;
    private transient final String mac;

    public CacheNotify(final String mac, final String requestType, final Message response) {
        this.response = response;
        this.requestType = requestType;
        this.mac = mac;
    }

    @Override
    public void run() {
        int maxAge;
        if (response.hasOption(OptionNumberRegistry.MAX_AGE)) {
            maxAge = response.getMaxAge();
        } else {
            maxAge = 60;
        }
        CacheHandler.getInstance().setValue(mac, "/" + requestType, maxAge, response.getContentType(), response.getPayloadString());
    }
}