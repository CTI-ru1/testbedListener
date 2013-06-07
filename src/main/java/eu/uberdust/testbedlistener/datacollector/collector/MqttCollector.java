package eu.uberdust.testbedlistener.datacollector.collector;

import eu.uberdust.testbedlistener.coap.CoapServer;
import eu.uberdust.testbedlistener.datacollector.parsers.MqttMessageHandler;
import org.fusesource.mqtt.client.*;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created with IntelliJ IDEA.
 * User: amaxilatis
 * Date: 6/7/13
 * Time: 12:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class MqttCollector implements Runnable {
    /**
     * LOGGER.
     */
    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(MqttCollector.class);
    private static final long DEFAULT_SLEEP_BEFORE_RE_ATTEMPT_IN_SECONDS = 5000;
    private static final long DEFAULT_MAX_RE_ATTEMPT_DURATION_IN_SECONDS = 3600 * 3;

    private long listenerSleepBeforeReAttemptInSeconds;
    private long listenerMaxReAttemptDurationInSeconds;
    private MQTT mqtt;

    private ArrayList<Topic> topics;
    private String listenerHostURI;
    private String listenerTopic;
    private long listenerLastSuccessfulSubscription;

    private String NEW_LINE = System.getProperty("line.separator");
    private CallbackConnection connection;

    public MqttCollector(String listenerHostURI, String listenerTopic, boolean debug) {
        this(listenerHostURI, listenerTopic, DEFAULT_SLEEP_BEFORE_RE_ATTEMPT_IN_SECONDS, DEFAULT_MAX_RE_ATTEMPT_DURATION_IN_SECONDS, debug);
    }

    public MqttCollector(String listenerHostURI, String listenerTopic, long listenerSleepBeforeReAttemptInSeconds, long listenerMaxReAttemptDurationInSeconds, boolean debug) {
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
        CoapServer.getInstance().setMqtt(this);
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



       /* Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run() {
                setName("MQTT client shutdown");
                stderr("Disconnecting the client.");

                connection.getDispatchQueue().execute(new Runnable() {
                    public void run() {
                        connection.disconnect(new Callback<Void>() {
                            public void onSuccess(Void value) {
                                stdout("Disconnecting onSuccess.");
                                done.countDown();
                            }
                            public void onFailure(Throwable value) {
                                stderr("Disconnecting onFailure: " + value);
                                stderr(value);
                                done.countDown();
                            }
                        });
                    }
                });
            }
        });
        */
        MqttMessageHandler mqttMessageHandler = new MqttMessageHandler();;
        connection.listener(mqttMessageHandler);


        connection.resume();
//        new Thread() {
//            @Override
//            public void run() {
//                while (true) {
//                    try {
//                        Thread.sleep(5000);
//                    } catch (InterruptedException e) {
//                        LOGGER.error(e.getMessage(), e);
//                    }
//                    connection.publish("inTopic", "test".getBytes(), QoS.AT_MOST_ONCE, false, new Callback() {
//                        @Override
//                        public void onSuccess(Object o) {
//                            LOGGER.info("onSuccess");
//                        }
//
//                        @Override
//                        public void onFailure(Throwable throwable) {
//                            LOGGER.info("onFailure");
//                        }
//                    });
//                }
//            }
//        };
        //.start();

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

    public void sendPayload(final String destination, final byte[] payloadIn) {
        connection.publish("arduinoGateway", payloadIn, QoS.AT_MOST_ONCE, false, new Callback() {
            @Override
            public void onSuccess(Object o) {
                LOGGER.info("onSuccess");
            }

            @Override
            public void onFailure(Throwable throwable) {
                LOGGER.info("onFailure");
            }
        });
    }


}
