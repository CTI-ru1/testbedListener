package eu.uberdust.testbedlistener.datacollector.collector.mqtt.listener;

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.Callback;
import org.fusesource.mqtt.client.CallbackConnection;
import org.fusesource.mqtt.client.Listener;
import org.fusesource.mqtt.client.QoS;

/**
 * Created with IntelliJ IDEA.
 * User: amaxilatis
 * Date: 6/7/13
 * Time: 12:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class MqttBaseListener implements Listener {
    protected final static String MQTT_SEPARATOR = ",";
    private final String topic;

    private CallbackConnection connection;

    public MqttBaseListener(final String topic) {
        this.topic = topic;
    }

    public CallbackConnection getConnection() {
        return connection;
    }

    public void setConnection(CallbackConnection connection) {
        this.connection = connection;
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
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onFailure(Throwable value) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void publish(final String message) {
        publish(message.getBytes());
    }

    public void publish(final byte[] messageBytes) {
        connection.publish("s" + topic, messageBytes, QoS.AT_MOST_ONCE, false, new Callback<Void>() {

            @Override
            public void onSuccess(Void value) {

            }

            @Override
            public void onFailure(Throwable value) {

            }
        });
    }
}