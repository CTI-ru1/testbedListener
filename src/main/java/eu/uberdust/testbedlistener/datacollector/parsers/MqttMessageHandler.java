package eu.uberdust.testbedlistener.datacollector.parsers;

import eu.uberdust.communication.UberdustClient;
import eu.uberdust.testbedlistener.util.HereIamMessage;
import org.apache.log4j.Logger;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private final ExecutorService executorService;
    private String urnPrefix;
    private String urnCapabilityPrefix;


    public MqttMessageHandler(int testbedID) {
        this.executorService = Executors.newCachedThreadPool();
        urnPrefix = null;
        urnCapabilityPrefix = null;
        try {
            urnPrefix = UberdustClient.getInstance().getUrnPrefix(testbedID);
            urnCapabilityPrefix = UberdustClient.getInstance().getUrnCapabilityPrefix(testbedID);
        } catch (JSONException e) {
            LOGGER.error(e, e);
        }

    }

    public void onConnected() {
        LOGGER.trace("Listener onConnected");
    }

    public void onDisconnected() {
        LOGGER.trace("Listener onDisconnected");
    }

    public void onPublish(UTF8Buffer topic, Buffer body, Runnable ack) {

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
            executorService.submit(new CoapMessageParser(macAddress, finalData,urnPrefix,urnCapabilityPrefix));
        } else {
            byte finalData[] = new byte[body.toByteArray().length - 2 + 1];
            finalData[0] = 0x69;
            finalData[1] = 0x69;
            System.arraycopy(byteData, 3, finalData, 2, body.toByteArray().length - 2 - 1);
            executorService.submit(new CoapMessageParser(macAddress, finalData,urnPrefix,urnCapabilityPrefix));
        }


        ack.run();
    }

    public void onFailure(Throwable value) {
        LOGGER.error("Listener onFailure: ", value);
//        done.countDown();
    }
}
