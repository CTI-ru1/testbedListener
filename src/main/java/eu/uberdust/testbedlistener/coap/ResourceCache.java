package eu.uberdust.testbedlistener.coap;

import eu.uberdust.testbedlistener.datacollector.collector.CollectorMqtt;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * An in Memory Data Structure used to store the latest values of all the IoT resources registered in the Listener.
 *
 * @author Dimitrios Amaxilatis
 */
public class ResourceCache {
    /**
     * The Data Structure.
     */
    private transient final Map<String, CacheEntry> cache;
    /**
     * Singleton Instance.
     */
    private static ResourceCache instance = null;

    /**
     * Constructor.
     */
    public ResourceCache() {
        cache = new TreeMap<>(new Comparator<String>() {
            @Override
            public int compare(final String s, final String s1) {
                return s.compareTo(s1);
            }
        });
    }

    /**
     * Get the Single Instance.
     *
     * @return the Singleton Instance.
     */
    public static ResourceCache getInstance() {
        synchronized (PendingRequestHandler.class) {
            if (instance == null) {
                instance = new ResourceCache();
            }
            return instance;
        }
    }

    /**
     * Get a the status of a single IoT Resource.
     *
     * @param resourceURIString the IoT Resource URI.
     * @return the {@see CacheEntry} for this IoT resource. null if the resource is not registered.
     */
    public CacheEntry getValue(final String resourceURIString) {
        if (cache.containsKey(resourceURIString)) {
            if (cache.containsKey(resourceURIString)) {
                return cache.get(resourceURIString);
            }
        }
        return null;
    }

    /**
     * Add or Update the status of a single IoT Resource.
     *
     * @param resourceURIString the URI of the IoT Resource.
     * @param maxAge            the validity of the value.
     * @param contentType       the content type of the value.
     * @param value             the value.
     * @param collector         the collector associated with this IoT resource.
     */
    public void setValue(final String resourceURIString, final int maxAge, final int contentType, final String value, final CollectorMqtt collector) {
        if (!cache.containsKey(resourceURIString)) {
            final CacheEntry pair = new CacheEntry(value, System.currentTimeMillis(), maxAge, contentType, collector);
            cache.put(resourceURIString, pair);
        } else {
            final CacheEntry element = cache.get(resourceURIString);

            element.setValue(value);
            element.setTimestamp(System.currentTimeMillis());
            element.setMaxAge(maxAge);
            element.setContentType(contentType);
            element.setCollector(collector);

        }
    }

    /**
     * Get all the contents of the cache. Use with caution!
     *
     * @return the contents of the cache.
     */
    public Map<String, CacheEntry> getCache() {
        return cache;
    }

    /**
     * Clear all the data stored in the Cache. Use with caution!
     */
    public void clearCache() {
        cache.clear();
    }
}
