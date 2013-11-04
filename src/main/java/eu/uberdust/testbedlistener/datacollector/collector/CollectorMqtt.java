package eu.uberdust.testbedlistener.datacollector.collector;

import eu.uberdust.testbedlistener.datacollector.parsers.CoapMessageParser;
import eu.uberdust.testbedlistener.mqtt.listener.BaseMqttListener;
import eu.uberdust.testbedlistener.util.HereIamMessage;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.Callback;
import org.fusesource.mqtt.client.QoS;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Class used to handle messages from a specific Gateway Device.
 * Passes all incoming CoAP and HEREIAM messages to a {@see CoapMessageParser} for processing.
 *
 * @author Dimitrios Amaxilatis
 */
public class CollectorMqtt extends BaseMqttListener {
    /**
     * LOGGER.
     */
    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(CollectorMqtt.class);
    private String testbedUrn;

    private final ExecutorService executorService;
    private String urnPrefix;
    private String urnCapabilityPrefix;

    public CollectorMqtt(final String deviceID, final String testbedUrn) {
        super(testbedUrn + MQTT_SEPARATOR + deviceID);
        this.testbedUrn = testbedUrn;

        this.executorService = Executors.newCachedThreadPool();
        urnPrefix = "";
        urnCapabilityPrefix = "";
    }

    public void onPublish(final UTF8Buffer topic, final Buffer body, Runnable ack) {


        try {
            LOGGER.debug("onPublish: " + topic + " --> " + Arrays.toString(body.toByteArray()));

            String macAddress = "0x"
                    + Integer.toHexString(body.toByteArray()[1]);
            if (Integer.toHexString(body.toByteArray()[0]).length() == 1) {
                macAddress += "0";
            }
            macAddress += Integer.toHexString(body.toByteArray()[0]);

            byte byteData[] = body.toByteArray();

            //fix arduino endiannes
            byte tmp = byteData[0];
            byteData[0] = byteData[1];
            byteData[1] = tmp;


            HereIamMessage mess = new HereIamMessage(byteData);
            if (mess.isValid()) {
                byte finalData[] = new byte[byteData.length + 2];
                finalData[0] = 0x69;
                finalData[1] = 0x69;
                System.arraycopy(byteData, 0, finalData, 2, byteData.length);
                executorService.submit(new CoapMessageParser(macAddress, finalData, urnPrefix, urnCapabilityPrefix, this));
            } else {
                byte finalData[] = new byte[body.toByteArray().length - 2 + 1];
                finalData[0] = 0x69;
                finalData[1] = 0x69;
                System.arraycopy(byteData, 3, finalData, 2, body.toByteArray().length - 2 - 1);
                executorService.submit(new CoapMessageParser(macAddress, finalData, urnPrefix, urnCapabilityPrefix, this));
            }
        } catch (Exception e) {
            LOGGER.error(e, e);
        }
        ack.run();

    }

    public String getTestbedUrn() {
        return testbedUrn;
    }

    @Override
    public void publish(final byte[] messageBytes) {
        connection.publish("s" + topic, messageBytes, QoS.AT_MOST_ONCE, false, new Callback<Void>() {

            @Override
            public void onSuccess(Void value) {
            }

            @Override
            public void onFailure(Throwable e) {
                LOGGER.error(e, e);
            }
        });
    }
}
