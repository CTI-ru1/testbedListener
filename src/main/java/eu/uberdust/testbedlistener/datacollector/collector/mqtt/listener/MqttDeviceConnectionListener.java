package eu.uberdust.testbedlistener.datacollector.collector.mqtt.listener;

import eu.uberdust.testbedlistener.coap.CoapServer;
import eu.uberdust.testbedlistener.datacollector.collector.AMqttCollector;
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
public class MqttDeviceConnectionListener extends MqttBaseListener {
    /**
     * LOGGER.
     */
    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(MqttDeviceConnectionListener.class);


    public MqttDeviceConnectionListener() {
        super("connect");
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
                    boolean reConnect = "1".equals(parts[0]);
                    String testbedHash = parts[1];
                    String deviceId = parts[2];

                    if (reConnect) {
                        LOGGER.info("Reconnect id:" + deviceId + " testbed:" + testbedHash);
                    } else {
                        LOGGER.info("Alive id:" + deviceId + " testbed:" + testbedHash);
                    }
                    //TODO: make this report connect messages
                    CoapServer.getInstance().registerGateway(reConnect, deviceId, testbedHash);
                    //Connect a new Listener for this Gateway
                    MqttConnectionManager.getInstance().listen(testbedHash + MQTT_SEPARATOR + deviceId + "/#", new AMqttCollector(deviceId, testbedHash));
                }
            }).start();
        } catch (Exception e) {
            LOGGER.error(e, e);
        }
    }

}
