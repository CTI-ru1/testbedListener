package eu.uberdust.testbedlistener.datacollector.collector;

import ch.ethz.inf.vs.californium.coap.*;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import eu.uberdust.testbedlistener.coap.ActiveRequest;
import eu.uberdust.testbedlistener.coap.CacheEntry;
import eu.uberdust.testbedlistener.coap.CoapServer;
import eu.uberdust.testbedlistener.coap.ResourceCache;
import eu.uberdust.testbedlistener.datacollector.parsers.CoapMessageParser;
import eu.uberdust.testbedlistener.mqtt.listener.BaseMqttListener;
import eu.uberdust.testbedlistener.util.Converter;
import eu.uberdust.testbedlistener.util.HereIamMessage;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.Callback;
import org.fusesource.mqtt.client.QoS;

import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

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
    private static final int MILLIS_IN_SECOND = 1000;
    private static final int MILLIS_IN_MINUTE = 60 * MILLIS_IN_SECOND;
    private static final long MILLIS_TO_STALE = 2 * MILLIS_IN_MINUTE;

    private final Map<Integer, ActiveRequest> activeRequestsMID;
    private final Map<String, ActiveRequest> activeRequestsTOKEN;
    /**
     * Registered Endpoints.
     */
    private transient final Map<String, Map<String, Long>> endpoints;

    private final Map<String, String> blockWisePending;

    private final ExecutorService executorService;
    public int responseObserveCounter;
    private int requestObserveCounter;
    private int observeLostCounter;
    private int requestWellKnownCounter;

    private final String deviceID;
    private final Timer timer;
    private final Random rand;

    public CollectorMqtt(final String deviceID) {
        super(deviceID);
        this.deviceID = deviceID;

        this.activeRequestsMID = new HashMap<>();
        this.activeRequestsTOKEN = new HashMap<>();
        this.endpoints = new HashMap<>();
        this.blockWisePending = new HashMap<>();
        rand = new Random();


        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("CoapMessageParser:" + deviceID + "-Thread #%d").build();
        this.executorService = Executors.newFixedThreadPool(5, threadFactory);
        this.requestWellKnownCounter = 0;
        this.requestObserveCounter = 0;
        this.responseObserveCounter = 0;
        this.observeLostCounter = 0;

        this.timer = new Timer("send-request-" + deviceID);

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

    public String getDeviceID() {
        return deviceID;
    }

    @Override
    public void publish(final byte[] messageBytes) {
        LOGGER.info("Publish s" + topic);
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

    /**
     * Adds the req to the list of active requests.
     *
     * @param address the address from which the req originated.
     * @param req     the req message.
     */
    public void addRequest(final String address, final Message req, final boolean hasQuery) {

        synchronized (this) {
            int count = 0;
            if (req.hasOption(OptionNumberRegistry.TOKEN)) {
                if (!activeRequestsTOKEN.containsKey(req.getTokenString())) {
                    for (String key : activeRequestsTOKEN.keySet()) {
                        if (activeRequestsTOKEN.get(key).getHost().equals(address) && activeRequestsTOKEN.get(key).getUriPath().equals(req.getUriPath())) {
                            count = activeRequestsTOKEN.get(key).getCount();
                            activeRequestsTOKEN.remove(key);
                            break;
                        }
                    }
                    ActiveRequest mRequest = new ActiveRequest(req.getUriPath(), req.getMID(), req.getTokenString(), address, hasQuery, System.currentTimeMillis());
                    mRequest.setCount(count);
                    activeRequestsTOKEN.put(req.getTokenString(), mRequest);
                    LOGGER.info("Added Active Request For " + mRequest.getHost() + " with mid " + mRequest.getMid() + " path:" + mRequest.getUriPath());
                }
            } else {
                if (!activeRequestsMID.containsKey(req.getMID())) {
                    for (Integer key : activeRequestsMID.keySet()) {
                        if (activeRequestsMID.get(key).getHost().equals(address) && activeRequestsMID.get(key).getUriPath().equals(req.getUriPath())) {
                            count = activeRequestsMID.get(key).getCount();
                            activeRequestsMID.remove(key);
                            break;
                        }
                    }
                    ActiveRequest mRequest = new ActiveRequest(req.getUriPath(), req.getMID(), req.getTokenString(), address, hasQuery, System.currentTimeMillis());
                    mRequest.setCount(count);
                    activeRequestsMID.put(req.getMID(), mRequest);
                    LOGGER.info("Added Active Request For " + mRequest.getHost() + " with mid " + mRequest.getMid() + " path:" + mRequest.getUriPath());
                }
            }
        }
    }

    /**
     * Matches a message to a previously received request.
     *
     * @param response The response received.
     * @return The URI of the request or null.
     */

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
        }
    }

    public int nextMID() {
        return rand.nextInt() % 0x10000;
    }

    private boolean isReservedMID(int currentMID) {
        if (activeRequestsMID.containsKey(currentMID)) return true;
        return false;
    }

    public void requestForResource(final String capability, final String address, final boolean observe) {
        synchronized (this) {
            LOGGER.info("Register for " + address + "/" + capability);
//        if (!capability.equals("pir")) return;
            URI uri = null;
            try {
                uri = new URI(new StringBuilder().append("/").append(capability).toString());
            } catch (URISyntaxException e) {
                LOGGER.error(e, e);
            }
            final Request request = new Request(CodeRegistry.METHOD_GET, false);
            //int newmid = mid.nextInt() % 65535;
            request.setURI(uri);
            request.setMID(nextMID());
            //request.setMID(newmid > 0 ? newmid : -newmid);
            Option urihost = new Option(OptionNumberRegistry.URI_HOST);
            urihost.setStringValue(address);
            request.addOption(urihost);
            if (observe) {
                request.setOption(new Option(0, OptionNumberRegistry.OBSERVE));
                request.setToken(eu.uberdust.testbedlistener.util.TokenManager.getInstance().acquireToken(address));
                requestObserveCounter++;
//                request.setToken(TokenManager.getInstance().acquireToken());
            }
//            ownRequests.put(request.getMID(), uri.toString());
            addRequest(address, request, false);
            sendRequest(request.toByteArray(), address);


        }
    }


    public Map<Integer, ActiveRequest> getActiveRequestsMID() {
        return activeRequestsMID;
    }

    public Map<String, ActiveRequest> getActiveRequestsTOKEN() {
        return activeRequestsTOKEN;
    }

    public int getRequestObserveCounter() {
        return requestObserveCounter;
    }

    public void incRequestObserveCounter() {
        requestObserveCounter++;
    }

    /**
     * Sends a payload to a device and add the message id at the beginning of the message.
     *
     * @param data    the data to send.
     * @param nodeUrn the destination device.
     */
    public void sendRequest(final byte[] data, final String nodeUrn) {
        final byte[] payload = new byte[data.length + 1];
        payload[0] = 51;
        System.arraycopy(data, 0, payload, 1, data.length);
        mqttSengBytes(nodeUrn, payload);
    }

    /**
     * Publish the bytes to the MQTT for the specific destinationMAC.
     *
     * @param destinationMAC the destinationMAC device MAC address.
     * @param bytesToSend    the bytes to send.
     */
    public void mqttSengBytes(final String destinationMAC, final byte[] bytesToSend) {
        byte[] destinationBytes = Converter.getInstance().addressToByte(destinationMAC);
        byte[] payloadWithDestination = new byte[bytesToSend.length + 2];
        payloadWithDestination[0] = destinationBytes[1];
        payloadWithDestination[1] = destinationBytes[0];
        System.arraycopy(bytesToSend, 0, payloadWithDestination, 2, bytesToSend.length);

        publish(payloadWithDestination);
    }

    /**
     * Send a COAP ACK to a device containing a single mid.
     *
     * @param mid     the mid to ack.
     * @param nodeUrn the destination device.
     */
    public void sendAck(final int mid, final String nodeUrn) {
        final Message ack = new Message(Message.messageType.ACK, 0);
        ack.setMID(mid);
        sendRequest(ack.toByteArray(), nodeUrn);
    }

    public void incResponseObserveCounter() {
        responseObserveCounter++;
    }

    public int getResponseObserveCounter() {
        return responseObserveCounter;
    }

    /**
     * Adds an address to the list of endpoints provided by the server.
     *
     * @param address an address address.
     * @param path
     * @return if the endoint existed in the server.
     */
    public boolean registerEndpoint(final String path, final String address) {

        synchronized (CoapServer.class) {
            if (endpoints.containsKey(address)) {
                if (endpoints.get(address).containsKey(path)) {
                    final String resourceURIString = deviceID + "/" + address + "/" + path;
                    final CacheEntry pair = ResourceCache.getInstance().getValue(resourceURIString);
                    long millis;
                    if (pair == null) {
                        millis = MILLIS_TO_STALE;
                    } else {
                        millis = pair.getMaxAge() * 1000;
                    }
                    if (System.currentTimeMillis() - endpoints.get(address).get(path) > millis) {
                        endpoints.get(address).put(path, System.currentTimeMillis());
                        LOGGER.info("Stale " + address + "/" + path);
                        return true;
                    } else {
                        endpoints.get(address).put(path, System.currentTimeMillis());
                        return false;
                    }
                } else {
                    LOGGER.debug("Register " + address + "/" + path);
                    endpoints.get(address).put(path, System.currentTimeMillis());
                    return true;
                }
            } else {
                LOGGER.debug("Register " + address + "/" + path);
                final HashMap<String, Long> map = new HashMap<String, Long>();
                map.put(path, System.currentTimeMillis());
                endpoints.put(address, map);
                return true;
            }
        }
    }

    /**
     * Adds an address to the list of endpoints provided by the server.
     *
     * @param address an address address.
     * @param path
     * @return if the endoint existed in the server.
     */
    public boolean isAlive(final String path, final String address) {
        synchronized (CoapServer.class) {
            if (endpoints.containsKey(address)) {
                if (endpoints.get(address).containsKey(path)) {
                    final String resourceURIString = deviceID + "/" + address + "/" + path;
                    final CacheEntry pair = ResourceCache.getInstance().getValue(resourceURIString);
                    long millis;
                    if (pair == null) {
                        millis = MILLIS_TO_STALE;
                    } else {
                        millis = pair.getMaxAge() * 1000;
                    }
                    if (System.currentTimeMillis() - endpoints.get(address).get(path) > millis) {
                        LOGGER.warn("Stale " + address + "/" + path);
                        if (pair != null) {
                            pair.setObserveLostCounter(pair.getLostCounter() + 1);
                        }
                        observeLostCounter++;
                        return false;
                    } else {

                        return true;
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    public void updateEndpoint(final String address, final String path) {
        endpoints.get(address).put(path, System.currentTimeMillis());
    }

    public Map<String, Map<String, Long>> getEndpoints() {
        return endpoints;
    }

    public int getObserveLostCounter() {
        return observeLostCounter;
    }

    public void incRequestWellKnownCounter() {
        requestWellKnownCounter++;
    }

    public int getRequestWellKnownCounter() {
        return requestWellKnownCounter;
    }

    /**
     * Sends a reply to a packet using the socket.
     *
     * @param buf           the bytes to send as a reply
     * @param socketAddress the address of the udp client
     */
    public void sendReply(final byte[] buf, final SocketAddress socketAddress) {
        byte[] localBuf = buf.clone();

        LOGGER.debug("Reply " + socketAddress);
        final DatagramPacket replyPacket;
        try {
            replyPacket = new DatagramPacket(localBuf, 0, buf.length, socketAddress);
            CoapServer.getInstance().socketSend(replyPacket);
        } catch (SocketException e) {
            LOGGER.error(e, e);
        }
    }

    public void addPending(String remoteAddress, String remainder) {
        blockWisePending.put(remoteAddress, remainder);
    }

    public String getPending(String remoteAddress) {
        if (blockWisePending.containsKey(remoteAddress)) {
            String value = blockWisePending.get(remoteAddress);
            blockWisePending.remove(remoteAddress);
            return value;
        }
        return "";
    }

    public void postMessage(final String key, final String payloadString) {
        final String[] parts = key.split("/", 3);
        final String destinationMAC = parts[1].replaceAll("0x", "");
        final String capabilityString = "/" + parts[2];
        LOGGER.info("postToResource:" + Arrays.toString(parts) + "");

        final Request request = new Request(CodeRegistry.METHOD_POST, false);
        request.setMID(nextMID());

        final Option uriHostOption = new Option(OptionNumberRegistry.URI_HOST);
        uriHostOption.setStringValue(destinationMAC);
        request.addOption(uriHostOption);
        request.setURI(capabilityString);
        request.setPayload(payloadString);
        sendRequest(request.toByteArray(), destinationMAC);

    }
}
