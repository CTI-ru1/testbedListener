package eu.uberdust.testbedlistener.datacollector.parsers;

import org.apache.log4j.Logger;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;

import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * User: amaxilatis
 * Date: 6/7/13
 * Time: 1:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class MqttMessageHandler implements org.fusesource.mqtt.client.Listener {
    /**
     * LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(MqttMessageHandler.class);

    public void onConnected() {
        LOGGER.info("Listener onConnected");
    }

    public void onDisconnected() {
        LOGGER.info("Listener onDisconnected");
    }

    public void onPublish(UTF8Buffer topic, Buffer body, Runnable ack) {

        LOGGER.info(topic + " --> " + Arrays.toString(body.toByteArray()));
        ack.run();
    }

    public void onFailure(Throwable value) {
        LOGGER.info("Listener onFailure: " + value);
        LOGGER.info(value);
//        done.countDown();
    }
}
