package eu.uberdust.testbedlistener.coap;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: ven0m
 * Date: 11/19/12
 * Time: 3:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class CacheHandler {
    private static CacheHandler instance = null;
    
    private transient Map<String, Map<String, Cache>> cache;

    public CacheHandler() {
        cache = new HashMap<String, Map<String, Cache>>();
    }

    public static CacheHandler getInstance() {
        synchronized (PendingRequestHandler.class) {
            if (instance == null) {
                instance = new CacheHandler();
            }
            return instance;
        }
    }

    public Cache getValue(String uriHost, String uriPath) {
        for (String device : cache.keySet()) {
            if ( device.equals(uriHost)) {
                for (String resource : cache.get(device).keySet()) {
                    if ( resource.equals(uriPath)) {
                        Cache pair = cache.get(device).get(resource);
                        if ( pair.getTimestamp() > System.currentTimeMillis() - 3*60*1000) {
                            return pair;
                        }
                    }
                }
            }
        }
        return null;
    }

    public void setValue(String uriHost, String uriPath, String value) {
        if ( cache.containsKey(uriHost)) {
            if ( cache.get(uriHost).containsKey(uriPath)) {
                cache.get(uriHost).get(uriPath).put(value, System.currentTimeMillis());
            }
            else {
                Cache pair = new Cache(value, System.currentTimeMillis());
                cache.get(uriHost).put(uriPath, pair);
            }
        }
        else {
            Cache pair = new Cache(value, System.currentTimeMillis());
            HashMap<String, Cache> map = new HashMap<String, Cache>();
            map.put(uriPath, pair);
            cache.put(uriHost, map);
        }
    }
}
