package eu.uberdust.testbedlistener.datacollector.collector.mqtt.listener;

import eu.uberdust.testbedlistener.coap.CoapServer;
import org.apache.commons.lang3.ArrayUtils;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.Listener;

import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * User: amaxilatis
 * Date: 6/7/13
 * Time: 12:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class MqttStatsListener extends MqttBaseListener implements Runnable, Listener {
    /**
     * LOGGER.
     */
    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(MqttStatsListener.class);

    public MqttStatsListener(String broker) {
        super(broker, "stats/#");
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
                    int deviceId = data[1] + data[2] * 256;
                    byte[] deviceMessage = new byte[4];
                    System.arraycopy(data, 3, deviceMessage, 0, 4);
                    ArrayUtils.reverse(deviceMessage);
                    final byte[] rest = new byte[data.length - 7];
                    System.arraycopy(data, 7, rest, 0, rest.length);
                    final Buffer restBuffer = new Buffer(rest);//.utf8().toString()
                    final String statMessage = restBuffer.utf8().toString();
                    LOGGER.info(statMessage);
                    final String[] statParts = statMessage.split(":");
                    final String key = statParts[1];
                    final String value = statParts[2];
                    LOGGER.info("Stat Message id:" + deviceId + " testbed:" + Arrays.toString(deviceMessage) + " key:" + key + " value:" + value);

                    //TODO: make this report stats messages
                    CoapServer.getInstance().appendGatewayStat(false, deviceId, deviceMessage, key, value);
                }
            }).start();
        } catch (Exception e) {
            LOGGER.error(e, e);
        }
    }

}
