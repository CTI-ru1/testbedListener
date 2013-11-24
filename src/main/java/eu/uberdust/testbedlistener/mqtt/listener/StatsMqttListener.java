package eu.uberdust.testbedlistener.mqtt.listener;

import eu.uberdust.testbedlistener.coap.CoapServer;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;

import java.util.Arrays;

/**
 * Receives and Processes incoming statistics messages from connected Gateway Devices.
 *
 * @author Dimitrios Amaxilatis
 * @date 04/10/2013
 */
public class StatsMqttListener extends BaseMqttListener {
    /**
     * LOGGER.
     */
    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(StatsMqttListener.class);

    public StatsMqttListener() {
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
                    LOGGER.info("Data: " + new String(data));
                    final String[] parts = new String(data).split(MQTT_SEPARATOR);
                    final String deviceId = parts[0];
                    final String statKey = parts[1];
                    final String statValue = parts[2];

                    LOGGER.info("Stat Message id:" + deviceId + " key:" + statKey + " value:" + statValue);

                    //TODO: make this report stats messages
                    CoapServer.getInstance().appendGatewayStat(deviceId, statKey, statValue);
                }
            }).start();
        } catch (Exception e) {
            LOGGER.error(e, e);
        }
    }

}
