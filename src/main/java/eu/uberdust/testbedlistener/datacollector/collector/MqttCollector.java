package eu.uberdust.testbedlistener.datacollector.collector;

import eu.uberdust.communication.UberdustClient;
import eu.uberdust.testbedlistener.coap.CoapServer;
import eu.uberdust.testbedlistener.datacollector.collector.mqtt.listener.MqttBaseListener;
import eu.uberdust.testbedlistener.datacollector.parsers.CoapMessageParser;
import eu.uberdust.testbedlistener.util.HereIamMessage;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.Callback;
import org.fusesource.mqtt.client.QoS;
import org.json.JSONException;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created with IntelliJ IDEA.
 * User: amaxilatis
 * Date: 6/7/13
 * Time: 12:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class MqttCollector extends MqttBaseListener {
    /**
     * LOGGER.
     */
    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(MqttCollector.class);
    private int testbedID;

    private final ExecutorService executorService;
    private String urnPrefix;
    private String urnCapabilityPrefix;

    public MqttCollector(String mqttBroker, int i) {
        super(mqttBroker, "testbed" + i + "/#");
        this.testbedID = i;

        this.executorService = Executors.newCachedThreadPool();
        urnPrefix = null;
        urnCapabilityPrefix = null;
        try {
            try {
                urnPrefix = UberdustClient.getInstance().getUrnPrefix(testbedID);
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            try {
                urnCapabilityPrefix = UberdustClient.getInstance().getUrnCapabilityPrefix(testbedID);
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        } catch (JSONException e) {
            LOGGER.error(e, e);
        }

    }

    public void onPublish(final UTF8Buffer topic, final Buffer body, Runnable ack) {
        new Thread() {

            @Override
            public void run() {

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
                        executorService.submit(new CoapMessageParser(macAddress, finalData, urnPrefix, urnCapabilityPrefix));
                    } else {
                        byte finalData[] = new byte[body.toByteArray().length - 2 + 1];
                        finalData[0] = 0x69;
                        finalData[1] = 0x69;
                        System.arraycopy(byteData, 3, finalData, 2, body.toByteArray().length - 2 - 1);
                        executorService.submit(new CoapMessageParser(macAddress, finalData, urnPrefix, urnCapabilityPrefix));
                    }
                } catch (Exception e) {
                    LOGGER.error(e, e);
                }
            }
        }.start();

        ack.run();
    }
}
