package eu.uberdust.testbedlistener.mqtt;

import eu.uberdust.testbedlistener.mqtt.listener.BaseMqttListener;
import org.fusesource.mqtt.client.*;

import java.net.URISyntaxException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: amaxilatis
 * Date: 11/4/13
 * Time: 11:18 AM
 * To change this template use File | Settings | File Templates.
 */
public class MqttConnectionManager {
    /**
     * LOGGER.
     */
    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(MqttConnectionManager.class);

    private static MqttConnectionManager ourInstance = new MqttConnectionManager();

    private static final long DEFAULT_SLEEP_BEFORE_RE_ATTEMPT_IN_SECONDS = 5000;
    private static final long DEFAULT_MAX_RE_ATTEMPT_DURATION_IN_SECONDS = 3600 * 3;

    private long listenerSleepBeforeReAttemptInSeconds;
    private long listenerMaxReAttemptDurationInSeconds;
    private MQTT mqtt;

    private String listenerHostURI;
    private String listenerTopic;
    private long listenerLastSuccessfulSubscription;

    Map<String, CallbackConnection> listeners;

    public static MqttConnectionManager getInstance() {
        return ourInstance;
    }


    private MqttConnectionManager() {
        listeners = new HashMap<String, CallbackConnection>();

    }

    public void connect(final String listenerHostURI) {
        this.listenerHostURI = listenerHostURI;
        this.listenerSleepBeforeReAttemptInSeconds = DEFAULT_SLEEP_BEFORE_RE_ATTEMPT_IN_SECONDS;
        this.listenerMaxReAttemptDurationInSeconds = DEFAULT_MAX_RE_ATTEMPT_DURATION_IN_SECONDS;

        mqtt = new MQTT();

        listenerLastSuccessfulSubscription = System.currentTimeMillis();

        try {
            mqtt.setHost(listenerHostURI);
        } catch (URISyntaxException e) {
            LOGGER.error("setHost failed", e);
        }
        LOGGER.info("MQTT connected!");
    }

    private void subscriptionSuccessful() {
        listenerLastSuccessfulSubscription = System.currentTimeMillis();
    }

    private boolean tryToListen() {
        return ((System.currentTimeMillis() - listenerLastSuccessfulSubscription) < listenerMaxReAttemptDurationInSeconds * 1000);
    }

    private void sleepBeforeReAttempt() throws InterruptedException {
        LOGGER.info("Listener stopped, re-attempt in " + listenerSleepBeforeReAttemptInSeconds + " seconds.");
        Thread.sleep(listenerSleepBeforeReAttemptInSeconds);
    }

    private void listenerReAttemptsOver() {
        LOGGER.info("Listener stopped since reattempts have failed for " + listenerMaxReAttemptDurationInSeconds + " seconds.");
    }

    public CallbackConnection listen(final String topic, final BaseMqttListener listener) {
        return listen(new String[]{topic}, listener);
    }

    public CallbackConnection listen(final String[] topics, final BaseMqttListener listener) {
        if (listeners.containsKey(topics[0])) {
            return listeners.get(topics[0]);
        }
        LOGGER.info("listen to " + Arrays.toString(topics));
        final CallbackConnection connection = mqtt.callbackConnection();
//        final CountDownLatch done = new CountDownLatch(1);

        connection.listener((Listener) listener);

        connection.resume();

        connection.connect(new Callback<Void>() {
            public void onFailure(Throwable error) {
                LOGGER.error(error, error);
            }

            public void onSuccess(Void value) {
                final List<Topic> mqttTopics = new ArrayList<Topic>();
                for (final String topic : topics) {
                    mqttTopics.add(new Topic(topic, QoS.AT_MOST_ONCE));
                }
                final Topic[] allTopics = mqttTopics.toArray(new Topic[mqttTopics.size()]);
                connection.subscribe(allTopics, new Callback<byte[]>() {
                    public void onSuccess(byte[] value) {
                        for (int i = 0; i < value.length; i++) {
                            LOGGER.info("Subscribed to Topic: " + allTopics[i].name() + " with QoS: " + QoS.values()[value[i]]);
                        }
                        subscriptionSuccessful();
                    }

                    public void onFailure(Throwable value) {
                        LOGGER.error("Subscribe failed: " + value);
//                        done.countDown();
                    }
                });
            }
        });
        listeners.put(topics[0], connection);

        listener.setConnection(connection);
//        try {
////            done.await();
//        } catch (Exception e) {
//            LOGGER.error(e);
//        }
        return connection;
    }

    public void publish(final String topic, final String message) {
        if (mqtt == null) {
            LOGGER.warn("mqtt not connected yet!");
            return;
        }
        CallbackConnection connection = null;
        if (!listeners.containsKey(topic)) {
            listeners.put(topic, mqtt.callbackConnection());
            connection = listeners.get(topic);
            connection.resume();
            connection.connect(new Callback<Void>() {
                @Override
                public void onSuccess(Void value) {
                    LOGGER.info("connected");

                }

                @Override
                public void onFailure(Throwable value) {
                    LOGGER.info("Failed to connect");
                }
            });
        }
        connection = listeners.get(topic);

        connection.publish(topic, message.getBytes(), QoS.AT_MOST_ONCE, false, new Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                LOGGER.info("Publish: " + topic + "/" + message);
            }

            @Override
            public void onFailure(Throwable value) {
                LOGGER.error("mqtt publish failed!");
            }
        });
    }
}
