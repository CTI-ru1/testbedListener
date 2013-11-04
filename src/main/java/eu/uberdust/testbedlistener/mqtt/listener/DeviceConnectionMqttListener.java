package eu.uberdust.testbedlistener.mqtt.listener;

import eu.uberdust.testbedlistener.coap.CoapServer;
import eu.uberdust.testbedlistener.datacollector.collector.CollectorMqtt;
import eu.uberdust.testbedlistener.mqtt.MqttConnectionManager;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;

import java.util.Arrays;

/**
 * Receives and Processes incoming Connection messages from Gateway Devices.
 * All Gateway Devices periodically send a message to the connect/# MQTT Channel to inform the server of their existence.
 *
 * @author Dimitrios Amaxilatis
 * @date 04/10/2013
 */
public class DeviceConnectionMqttListener extends BaseMqttListener {
    /**
     * LOGGER.
     */
    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(DeviceConnectionMqttListener.class);


    public DeviceConnectionMqttListener() {
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
                    MqttConnectionManager.getInstance().listen(testbedHash + MQTT_SEPARATOR + deviceId + "/#", new CollectorMqtt(deviceId, testbedHash));
                }
            }).start();
        } catch (Exception e) {
            LOGGER.error(e, e);
        }
    }

}
