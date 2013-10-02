package eu.uberdust.testbedlistener.datacollector.collector.mqtt.listener;

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.*;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

/**
 * Created with IntelliJ IDEA.
 * User: amaxilatis
 * Date: 6/7/13
 * Time: 12:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class MqttBaseListener implements Runnable, Listener {
    /**
     * LOGGER.
     */
    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(MqttBaseListener.class);
    private static final long DEFAULT_SLEEP_BEFORE_RE_ATTEMPT_IN_SECONDS = 5000;
    private static final long DEFAULT_MAX_RE_ATTEMPT_DURATION_IN_SECONDS = 3600 * 3;

    private long listenerSleepBeforeReAttemptInSeconds;
    private long listenerMaxReAttemptDurationInSeconds;
    private MQTT mqtt;

    private ArrayList<Topic> topics;
    private String listenerHostURI;
    private String listenerTopic;
    private long listenerLastSuccessfulSubscription;

    protected CallbackConnection connection;

    public MqttBaseListener(String mqttBroker, String topic) {
        this(mqttBroker, topic, DEFAULT_SLEEP_BEFORE_RE_ATTEMPT_IN_SECONDS, DEFAULT_MAX_RE_ATTEMPT_DURATION_IN_SECONDS, true);
    }

//    public MqttBaseListener(String mqttBroker) {
//        this(mqttBroker, "connect/#", DEFAULT_SLEEP_BEFORE_RE_ATTEMPT_IN_SECONDS, DEFAULT_MAX_RE_ATTEMPT_DURATION_IN_SECONDS, true);
//    }
//
//    public MqttBaseListener(String listenerHostURI, String listenerTopic, boolean debug) {
//        this(listenerHostURI, listenerTopic, DEFAULT_SLEEP_BEFORE_RE_ATTEMPT_IN_SECONDS, DEFAULT_MAX_RE_ATTEMPT_DURATION_IN_SECONDS, debug);
//    }

    public MqttBaseListener(String listenerHostURI, String listenerTopic, long listenerSleepBeforeReAttemptInSeconds, long listenerMaxReAttemptDurationInSeconds, boolean debug) {
        init(listenerHostURI, listenerTopic, listenerSleepBeforeReAttemptInSeconds, listenerMaxReAttemptDurationInSeconds, debug);
    }

    private void init(String listenerHostURI, String listenerTopic, long listenerSleepBeforeReAttemptInSeconds, long listenerMaxReAttemptDurationInSeconds, boolean debug) {
        this.listenerHostURI = listenerHostURI;
        this.listenerTopic = listenerTopic;
        this.listenerSleepBeforeReAttemptInSeconds = listenerSleepBeforeReAttemptInSeconds;
        this.listenerMaxReAttemptDurationInSeconds = listenerMaxReAttemptDurationInSeconds;

        initMQTT();
    }

    private void initMQTT() {
        mqtt = new MQTT();
        listenerLastSuccessfulSubscription = System.currentTimeMillis();

        try {
            mqtt.setHost(listenerHostURI);
        } catch (URISyntaxException e) {
            LOGGER.error("setHost failed", e);
        }
        QoS qos = QoS.AT_MOST_ONCE;
        topics = new ArrayList<Topic>();
        topics.add(new Topic(listenerTopic, qos));
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

    private void listen() {
        connection = mqtt.callbackConnection();
        final CountDownLatch done = new CountDownLatch(1);

        connection.listener(this);


        connection.resume();

        connection.connect(new Callback<Void>() {
            public void onFailure(Throwable value) {
                LOGGER.error("Connect onFailure...: " + value);
//                done.countDown();
            }

            public void onSuccess(Void value) {
                final Topic[] ta = topics.toArray(new Topic[topics.size()]);
                connection.subscribe(ta, new Callback<byte[]>() {
                    public void onSuccess(byte[] value) {
                        for (int i = 0; i < value.length; i++) {
                            LOGGER.info("Subscribed to Topic: " + ta[i].name() + " with QoS: " + QoS.values()[value[i]]);
                        }
                        subscriptionSuccessful();
                    }

                    public void onFailure(Throwable value) {
                        LOGGER.error("Subscribe failed: " + value);
                        done.countDown();
                    }
                });
            }
        });

        try {
            done.await();
        } catch (Exception e) {
            LOGGER.error(e);
        }
    }

    @Override
    public void run() {
        while (tryToListen()) {
            initMQTT();
            listen();
            try {
                sleepBeforeReAttempt();
            } catch (InterruptedException e) {
                LOGGER.error("Sleep failed", e);
            }
        }

        listenerReAttemptsOver();
    }

    @Override
    public void onConnected() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onDisconnected() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onPublish(UTF8Buffer topic, final Buffer body, Runnable ack) {
        try {
            (new Thread() {
                @Override
                public void run() {
                    //TODO: make this listen for stats messages
//                        CoapServer.getInstance().registerGateway(body.utf8().toString());
                }
            }).start();
        } catch (Exception e) {
            LOGGER.error(e, e);
        }
    }

    @Override
    public void onFailure(Throwable value) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void publish(String arduinoGateway, byte[] payloadWithDestination, QoS atMostOnce, boolean b, Callback callback) {
        connection.publish(arduinoGateway, payloadWithDestination, atMostOnce, b, callback);
    }
}
