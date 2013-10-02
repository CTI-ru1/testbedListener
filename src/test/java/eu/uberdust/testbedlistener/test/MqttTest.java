package eu.uberdust.testbedlistener.test;

import eu.uberdust.testbedlistener.datacollector.collector.MqttCollector;
import eu.uberdust.testbedlistener.util.PropertyReader;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.*;

import java.io.IOException;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

/**
 * Created with IntelliJ IDEA.
 * User: amaxilatis
 * Date: 6/4/13
 * Time: 12:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class MqttTest {

    public static void main(String[] args) throws SocketException {

        PropertyReader.getInstance().setFile("listener.properties");
        MqttTestClient mqList = new MqttTestClient("tcp://150.140.5.11:61616", "heartbeat/#", true);
        new Thread(mqList).start();



    }

}


class MqttTestClient implements Runnable, Listener {
    /**
     * LOGGER.
     */
    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(MqttCollector.class);
    private static final long DEFAULT_SLEEP_BEFORE_RE_ATTEMPT_IN_SECONDS = 5000;
    private static final long DEFAULT_MAX_RE_ATTEMPT_DURATION_IN_SECONDS = 3600 * 3;
    private int testbedID;

    private long listenerSleepBeforeReAttemptInSeconds;
    private long listenerMaxReAttemptDurationInSeconds;
    private MQTT mqtt;

    private ArrayList<Topic> topics;
    private String listenerHostURI;
    private String listenerTopic;
    private long listenerLastSuccessfulSubscription;

    private CallbackConnection connection;

    public MqttTestClient(String listenerHostURI, String listenerTopic, boolean debug) {
        this(listenerHostURI, listenerTopic, DEFAULT_SLEEP_BEFORE_RE_ATTEMPT_IN_SECONDS, DEFAULT_MAX_RE_ATTEMPT_DURATION_IN_SECONDS, debug);
    }

    public MqttTestClient(String listenerHostURI, String listenerTopic, long listenerSleepBeforeReAttemptInSeconds, long listenerMaxReAttemptDurationInSeconds, boolean debug) {
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

//        String mqttUser = PropertyReader.getInstance().getProperties().getProperty("mqtt.username");
//        String mqttPass = PropertyReader.getInstance().getProperties().getProperty("mqtt.password");
//
//        if (mqttUser != null) {
//            mqtt.setUserName(mqttUser);
//            mqtt.setPassword(mqttPass);
//        }

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
    public void onPublish(UTF8Buffer topic, Buffer body, Runnable ack) {
//        if (!body.toString().contains("reset")) {
//            try {
//                Runtime.getRuntime().exec("gntp-send Uberdust \"" + body.utf8().toString().replaceAll(" ", "") + "\"");
//            } catch (IOException e) {
//                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//            }
//        }
        LOGGER.info("Topic:" + topic.toString() + " Message:" + body.toString());

    }

    @Override
    public void onFailure(Throwable value) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}



