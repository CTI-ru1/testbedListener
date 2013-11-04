package eu.uberdust.testbedlistener.mqtt.listener;

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;

/**
 * Receives and Processes incoming Heartbeat messages.
 *
 * @author Dimitrios Amaxilatis
 * @date 04/10/2013
 */
public class HeartbeatMqttListener extends BaseMqttListener {
    /**
     * LOGGER.
     */
    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(HeartbeatMqttListener.class);

    public HeartbeatMqttListener() {
        super("heartbeat");
    }


    @Override
    public void onPublish(UTF8Buffer topic, final Buffer body, Runnable ack) {
        try {
            (new Thread() {
                @Override
                public void run() {
                }
            }).start();
        } catch (Exception e) {
            LOGGER.error(e, e);
        }
    }
}
