package eu.uberdust.testbedlistener.datacollector.collector;

import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import eu.uberdust.testbedlistener.coap.ActiveRequest;
import eu.uberdust.testbedlistener.datacollector.parsers.CoapMessageParser;
import eu.uberdust.testbedlistener.mqtt.listener.BaseMqttListener;
import eu.uberdust.testbedlistener.util.HereIamMessage;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.Callback;
import org.fusesource.mqtt.client.QoS;

import java.util.Arrays;
import java.util.Map;
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

    private transient final Map<String, ActiveRequest> activeRequestsTOKEN;


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
                executorService.submit(new CoapMessageParser(macAddress, finalData, this));
            } else {
                byte finalData[] = new byte[body.toByteArray().length - 2 + 1];
                finalData[0] = 0x69;
                finalData[1] = 0x69;
                System.arraycopy(byteData, 3, finalData, 2, body.toByteArray().length - 2 - 1);
                executorService.submit(new CoapMessageParser(macAddress, finalData, this));
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

    public String matchResponse(final Message response) {
        synchronized (this) {
            if (response.hasOption(OptionNumberRegistry.TOKEN)) {
                if (activeRequestsTOKEN.isEmpty()) {
                    return null;
                } else if (activeRequestsTOKEN.containsKey(response.getTokenString())) {
                    ActiveRequest activeRequest = activeRequestsTOKEN.get(response.getTokenString());
                    activeRequest.setTimestamp(System.currentTimeMillis());
                    activeRequest.incCount();
                    activeRequest.setMid(response.getMID());
                    activeRequestsTOKEN.put(response.getTokenString(), activeRequest);
                    return activeRequest.getHost() + "," + activeRequest.getUriPath();
                } else {
                    return null;
                }
            } else {
                if (activeRequestsMID.isEmpty()) {
                    return null;
                } else if (activeRequestsMID.containsKey(response.getMID())) {
                    ActiveRequest activeRequest = activeRequestsMID.get(response.getMID());
                    String retVal = activeRequest.getHost() + "," + activeRequest.getUriPath();
                    activeRequestsMID.remove(response.getMID());
                    return retVal;
                } else {
                    return null;
                }
            }
//            if (activeRequests.isEmpty()) {
//                LOGGER.info("no active request");
//                return null;
//            }
//            final byte[] payload = response.getPayload();
//            int mid = response.getMID();

//            LOGGER.info(response.getPayloadString());
//            LOGGER.info(response.hasOption(OptionNumberRegistry.TOKEN));
//            LOGGER.info(response.getOptionCount());
//            LOGGER.info(address);

//            for (int key : activeRequests.keySet()) {
//                ActiveRequest activeRequest = activeRequests.get(key);
//                if ((response.hasOption(OptionNumberRegistry.TOKEN))
//                        && (response.getTokenString().equals(activeRequest.getToken()))) {
//                    LOGGER.info("Found By Token " + response.getTokenString() + "==" + activeRequest.getToken());
////                    response.setPayload(payload);
////                    LOGGER.info(response.getPayloadString());
//                    activeRequest.setTimestamp(System.currentTimeMillis());
//                    activeRequest.incCount();
//
//                    if (activeRequest.getMid() == response.getMID()) {
//                        return activeRequest.getHost() + "," + activeRequest.getUriPath();
//                    }
//
//                    activeRequest.setMid(response.getMID());
//                    if (activeRequest.hasQuery()) {
//                        return null;
//                    } else {
//                        return activeRequest.getHost() + "," + activeRequest.getUriPath();
//                    }
//                }
////                LOGGER.info(activeRequest.getMid() + "--" + response.getMID());
//                if (response.getMID() == activeRequest.getMid()) {
//                    String retVal = activeRequest.getHost() + "," + activeRequest.getUriPath();
////                    if (activeRequest.hasQuery()) {
////                        retVal = null;
////                    }
//                    LOGGER.info("Found By MID" + retVal);
//                    try {
//                        activeRequests.remove(key);
//                    } catch (Exception e) {
//                        LOGGER.error(e, e);
//                    }
//                    return retVal;
//                }
//            }
//            return null;
        }
    }
}
