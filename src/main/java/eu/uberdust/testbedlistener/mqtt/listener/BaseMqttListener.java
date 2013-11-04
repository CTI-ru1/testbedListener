package eu.uberdust.testbedlistener.mqtt.listener;

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.Callback;
import org.fusesource.mqtt.client.CallbackConnection;
import org.fusesource.mqtt.client.Listener;
import org.fusesource.mqtt.client.QoS;

/**
 * Base Listener Class for MQTT channels.
 *
 * @author Dimitrios Amaxilatis
 * @date 04/10/2013
 */
public class BaseMqttListener implements Listener {
    protected final static String MQTT_SEPARATOR = ",";
    protected final String topic;

    protected CallbackConnection connection;

    public BaseMqttListener(final String topic) {
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
        connection.publish(topic, messageBytes, QoS.AT_MOST_ONCE, false, new Callback<Void>() {

            @Override
            public void onSuccess(Void value) {

            }

            @Override
            public void onFailure(Throwable value) {

            }
        });
    }
}