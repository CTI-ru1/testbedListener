package eu.uberdust.testbedlistener.datacollector.collector.mqtt.listener;

import eu.uberdust.testbedlistener.coap.CoapServer;
import org.apache.commons.lang3.ArrayUtils;
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
public class MqttConnectionListener extends MqttBaseListener {
    /**
     * LOGGER.
     */
    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(MqttConnectionListener.class);


    public MqttConnectionListener(String mqttBroker) {
        super(mqttBroker, "connect/#");
        CoapServer.getInstance().setMqtt(this);
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
                    byte connectionStatus = data[0];
                    int deviceId = data[1] + data[2] * 256;
                    byte[] deviceMessage = new byte[4];
                    System.arraycopy(data, 3, deviceMessage, 0, 4);
                    ArrayUtils.reverse(deviceMessage);
                    if (connectionStatus == 0) {
                        LOGGER.info("Alive id:" + deviceId + " testbed:" + Arrays.toString(deviceMessage));
                    } else {
                        LOGGER.info("Reconnect id:" + deviceId + " testbed:" + Arrays.toString(deviceMessage));
                    }
                    //TODO: make this report connect messages
                    CoapServer.getInstance().registerGateway(connectionStatus == 1, deviceId, deviceMessage);
                }
            }).start();
        } catch (Exception e) {
            LOGGER.error(e, e);
        }
    }


}
