package eu.uberdust.testbedlistener.coap;

import java.util.Comparator;
import java.util.Map;
import java.util.Timer;
import java.util.TreeMap;

/**
 * Created by IntelliJ IDEA.
 * User: ven0m
 * Date: 11/19/12
 * Time: 3:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class CacheHandler {
    private transient final Map<String, Map<String, Cache>> cache;
    private static CacheHandler instance = null;

    private Timer timer;

    public CacheHandler() {
        cache = new TreeMap<String, Map<String, Cache>>(new Comparator<String>() {
            @Override
            public int compare(String s, String s1) {
                return s.compareTo(s1);
            }
        });

//        timer = new Timer();
//        timer.scheduleAtFixedRate(new TimerTask() {
//            @Override
//            public void run() {
//                for (String node : cache.keySet()) {
//                    for (String capability : cache.get(node).keySet()) {
//                        if (System.currentTimeMillis() - cache.get(node).get(capability).getTimestamp() < 120000) {
//                            System.out.println("CacheCheck " + node + " @ " + capability + " : outofdate");
//
//                        }
//                    }
//                }
//
//            }
//        }, 60000, 60000);
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
        if (cache.containsKey(uriHost) && cache.get(uriHost).containsKey(uriPath)) {
            Cache element = cache.get(uriHost).get(uriPath);
            if ((element.getTimestamp() > System.currentTimeMillis() - element.getMaxAge() * 1000)) {
                return element;
            }
        }
        return null;
    }

    public void setValue(String uriHost, String uriPath, int maxAge, int contentType, String value) {
        if (cache.containsKey(uriHost)) {
            if (cache.get(uriHost).containsKey(uriPath)) {
                cache.get(uriHost).get(uriPath).put(value, System.currentTimeMillis(), maxAge, contentType, cache.get(uriHost).get(uriPath).getLostCounter());
            } else {
                Cache pair = new Cache(value, System.currentTimeMillis(), maxAge, contentType);
                cache.get(uriHost).put(uriPath, pair);
            }
        } else {
            Cache pair = new Cache(value, System.currentTimeMillis(), maxAge, contentType);
            TreeMap<String, Cache> map = new TreeMap<String, Cache>(new Comparator<String>() {
                @Override
                public int compare(String s, String s1) {
                    return s.compareTo(s1);
                }
            });
            map.put(uriPath, pair);
            cache.put(uriHost, map);
        }
    }

    public Map<String, Map<String, Cache>> getCache() {
        return cache;
    }

    public void clearCache() {
        cache.clear();
    }
}
