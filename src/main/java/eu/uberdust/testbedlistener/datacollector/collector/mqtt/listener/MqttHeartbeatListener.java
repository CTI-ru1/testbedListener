package eu.uberdust.testbedlistener.datacollector.collector.mqtt.listener;

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;

/**
 * Created with IntelliJ IDEA.
 * User: amaxilatis
 * Date: 6/7/13
 * Time: 12:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class MqttHeartbeatListener extends MqttBaseListener {
    /**
     * LOGGER.
     */
    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(MqttHeartbeatListener.class);

    public MqttHeartbeatListener() {
        super("heartbeat");
    }


    @Override
    public void onPublish(UTF8Buffer topic, final Buffer body, Runnable ack) {
        try {
            (new Thread() {
                @Override
                public void run() {
                    if (!body.toString().contains("reset")) {
//                        CoapServer.getInstance().registerGateway(body.utf8().toString());
                    }
                }
            }).start();
        } catch (Exception e) {
            LOGGER.error(e, e);
        }
    }
}
