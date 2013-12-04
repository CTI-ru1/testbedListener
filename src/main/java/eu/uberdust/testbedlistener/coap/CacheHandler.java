package eu.uberdust.testbedlistener.coap;

import eu.uberdust.testbedlistener.datacollector.collector.CollectorMqtt;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by IntelliJ IDEA.
 * User: ven0m
 * Date: 11/19/12
 * Time: 3:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class CacheHandler {
    private transient final Map<String, Cache> cache;
    private static CacheHandler instance = null;

    public CacheHandler() {
        cache = new TreeMap<>(new Comparator<String>() {
            @Override
            public int compare(final String s, final String s1) {
                return s.compareTo(s1);
            }
        });
    }

    public static CacheHandler getInstance() {
        synchronized (PendingRequestHandler.class) {
            if (instance == null) {
                instance = new CacheHandler();
            }
            return instance;
        }
    }

    public Cache getValue(final String resourceURIString) {
        if (cache.containsKey(resourceURIString)) {
            if (cache.containsKey(resourceURIString)) {
                return cache.get(resourceURIString);
            }
        }
        return null;
    }

    public void setValue(final String resourceURIString, final int maxAge, final int contentType, final String value,final CollectorMqtt collector) {
        if (!cache.containsKey(resourceURIString)) {
            final Cache pair = new Cache(value, System.currentTimeMillis(), maxAge, contentType,collector);
            cache.put(resourceURIString, pair);
        } else {
            cache.get(resourceURIString).put(value, System.currentTimeMillis(), maxAge, contentType,collector);
        }
    }

    public Map<String, Cache> getCache() {
        return cache;
    }

    public void clearCache() {
        cache.clear();
    }
}
