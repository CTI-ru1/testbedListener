package eu.uberdust.testbedlistener.datacollector.collector.mqtt.listener;

import eu.uberdust.testbedlistener.coap.CoapServer;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;

import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * User: amaxilatis
 * Date: 6/7/13
 * Time: 12:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class MqttStatsListener extends MqttBaseListener {
    /**
     * LOGGER.
     */
    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(MqttStatsListener.class);

    public MqttStatsListener() {
        super("stats");
    }


    @Override
    public void onPublish(UTF8Buffer topic, final Buffer body, Runnable ack) {
        try {
            (new Thread() {
                @Override
                public void run() {

                    byte[] data = new byte[body.getLength()];
                    System.arraycopy(body.getData(), body.getOffset(), data, 0, data.length);
                    LOGGER.debug("Body: " + Arrays.toString(body.getData()));
                    LOGGER.debug("Data: " + Arrays.toString(data));
                    final String[] parts = new String(data).split(MQTT_SEPARATOR);
                    final String testbedHash = parts[0];
                    final String deviceId = parts[1];
                    final String statKey = parts[2];
                    final String statValue = parts[3];

                    LOGGER.info("Stat Message id:" + deviceId + " testbed:" + testbedHash + " key:" + statKey + " value:" + statValue);

                    //TODO: make this report stats messages
                    CoapServer.getInstance().appendGatewayStat(false, deviceId, testbedHash, statKey, statValue);
                }
            }).start();
        } catch (Exception e) {
            LOGGER.error(e, e);
        }
    }

}
