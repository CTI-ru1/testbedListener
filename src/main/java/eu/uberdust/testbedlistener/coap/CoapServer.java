package eu.uberdust.testbedlistener.coap;

import ch.ethz.inf.vs.californium.coap.*;
import com.rapplogic.xbee.api.XBeeAddress16;
import eu.mksense.XBeeRadio;
import eu.uberdust.DeviceCommand;
import eu.uberdust.communication.UberdustClient;
import eu.uberdust.testbedlistener.HeartBeatJob;
import eu.uberdust.testbedlistener.coap.udp.EthernetUDPhandler;
import eu.uberdust.testbedlistener.coap.udp.UDPhandler;
import eu.uberdust.testbedlistener.util.Converter;
import eu.uberdust.testbedlistener.util.PropertyReader;
import eu.uberdust.testbedlistener.util.TokenManager;
import eu.uberdust.testbedlistener.util.commiter.WsCommiter;
import org.apache.log4j.Logger;
import org.fusesource.mqtt.client.Callback;
import org.fusesource.mqtt.client.CallbackConnection;
import org.fusesource.mqtt.client.QoS;
import org.json.JSONException;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: amaxilatis
 * Date: 5/19/12
 * Time: 6:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class CoapServer {
    /**
     * LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(CoapServer.class);
    private static final String CONNECTED_ARDUINO_GATEWAY_STRING = "reconnectarduinoGateway";
    /**
     * Singleton instance.
     */
    private static CoapServer instance = null;
    /**
     * Registered Endpoints.
     */
    private transient final Map<String, Map<String, Long>> endpoints;
    /**
     * Active Requests.
     */
    private transient final Map<Integer, ActiveRequest> activeRequestsMID;
    private transient final Map<String, ActiveRequest> activeRequestsTOKEN;
    /**
     * COAP Server socket.
     */
    private transient DatagramSocket socket;

    /**
     * Random number generator.
     */
    //private transient final Random mid;
    private final String testbedPrefix;
    private static final int MILLIS_IN_SECOND = 1000;
    private static final int MILLIS_IN_MINUTE = 60 * MILLIS_IN_SECOND;
    private static final long MILLIS_TO_STALE = 2 * MILLIS_IN_MINUTE;
    private final Map<Integer, String> ownRequests;
    private final List<TokenItem> ownObserves;
    private final Map<String, String> blockWisePending;
    private final Map<Integer, String> ethernetBlockWisePending;
    private final Map<String, Long> duplicates;
    private int currentMID;
    private final long startTime;
    private int requestWellKnownCounter;
    private int requestObserveCounter;
    public int responseObserveCounter;
    private int observeLostCounter;
    private EthernetUDPhandler ethernetUDPHandler;
    private CallbackConnection mqtt;
    private final StdSchedulerFactory schFactory;
    private Scheduler sch;
    private Map<String, Long> arduinoGateways;
    private Map<String, String> xCount, yCount;

    public long getStartTime() {
        return startTime;
    }

    /**
     * Constructor.
     */
    public CoapServer() {
        ownRequests = new HashMap<Integer, String>();
        ownObserves = new ArrayList<TokenItem>();
        this.endpoints = new HashMap<String, Map<String, Long>>();
        blockWisePending = new HashMap<String, String>();
        ethernetBlockWisePending = new HashMap<Integer, String>();
        this.activeRequestsMID = new HashMap<Integer, ActiveRequest>();
        this.activeRequestsTOKEN = new HashMap<String, ActiveRequest>();
        this.arduinoGateways = new HashMap<String, Long>();
        this.xCount = new HashMap<String, String>();
        this.yCount = new HashMap<String, String>();
        this.testbedPrefix = PropertyReader.getInstance().getTestbedPrefix();
        this.duplicates = new HashMap<String, Long>();
        currentMID = (int) (Math.random() * 0x10000);
        this.startTime = System.currentTimeMillis();
        this.requestWellKnownCounter = 0;
        this.requestObserveCounter = 0;
        this.responseObserveCounter = 0;
        this.observeLostCounter = 0;


        schFactory = new StdSchedulerFactory();

        try {
            sch = schFactory.getScheduler();
            sch.start();
        } catch (SchedulerException e) {
            LOGGER.error(e, e);
        }
        JobDetail heartbeatToGatewaysJob = JobBuilder.newJob(HeartBeatJob.class).withIdentity("heartbeatToGatewaysJob").build();
        try {
            this.addJob(heartbeatToGatewaysJob, 3);
        } catch (SchedulerException e) {
            LOGGER.error(e, e);
        }


//        Timer discoveryTimer = new Timer();
//        discoveryTimer.scheduleAtFixedRate(new BroadcastCoapRequest(), 20000, 60000);


        //Start the udp socket
        try {
            socket = new DatagramSocket(5683);
        } catch (SocketException e) {
            LOGGER.error(e.getMessage(), e);
        }

        GatewayManager.getInstance();

//        //Start the handler
        final UDPhandler thread = new UDPhandler(socket);
        thread.start();
//
//        final Thread threadEthernet = new Thread(new EthernetSupport(thread));
//        threadEthernet.start();
//        cleanupTimer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                cleanActiveRequests();
//            }
//        },5*60*1000);

//        Timer activeRequestCleanupTimer = new Timer();
//        activeRequestCleanupTimer.scheduleAtFixedRate(new TimerTask() {
//            @Override
//            public void run() {
//                CoapServer.getInstance().cleanActiveRequests();
//            }
//        }, 60000, 60000);

        LOGGER.info("started CoapServer");
    }


    public void addJob(JobDetail jobDetail, int interval) throws SchedulerException {
        //Trigger the job to run on the next round minute
        Trigger trigger = TriggerBuilder.newTrigger().withSchedule(
                SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(interval).repeatForever())
                .build();
        sch.scheduleJob(jobDetail, trigger);
    }

    public void cleanActiveRequests() {
        LOGGER.info("Cleaning active Requests");
        for (int key : activeRequestsMID.keySet()) {
            if (System.currentTimeMillis() - activeRequestsMID.get(key).getTimestamp() > 2 * MILLIS_IN_MINUTE + 20 * MILLIS_IN_SECOND) {

//                try {
//                    TokenManager.getInstance().releaseToken(Hex.decodeHex(activeRequest.getToken().toCharArray()));
//                } catch (DecoderException e) {
//
//                }
                activeRequestsMID.remove(key);
            }
        }


    }


    /**
     * Singleton Class.
     *
     * @return The unique instance of CoapServer.
     */
    public static CoapServer getInstance() {
        synchronized (CoapServer.class) {
            if (instance == null) {
                instance = new CoapServer();
            }
        }
        return instance;
    }

    /**
     * Adds an address to the list of endpoints provided by the server.
     *
     * @param address an address address.
     * @param path
     * @return if the endoint existed in the server.
     */
    public boolean registerEndpoint(final String path, final String address) {
        LOGGER.error("Register resource" + path + " from device " + address);

        synchronized (CoapServer.class) {
            if (endpoints.containsKey(address)) {
                if (endpoints.get(address).containsKey(path)) {
                    Cache pair = CacheHandler.getInstance().getValue(address, path);
                    long millis;
                    if (pair == null) {
                        millis = MILLIS_TO_STALE;
                    } else {
                        millis = pair.getMaxAge() * 1000;
                    }
                    if (System.currentTimeMillis() - endpoints.get(address).get(path) > millis) {
                        endpoints.get(address).put(path, System.currentTimeMillis());
                        LOGGER.info("Resource was out of date " + address + "/" + path);
                        return true;
                    } else {
                        endpoints.get(address).put(path, System.currentTimeMillis());
                        return false;
                    }
                } else {
                    endpoints.get(address).put(path, System.currentTimeMillis());
                    return true;
                }
            } else {
                LOGGER.info("Adding new device: " + address);
                HashMap<String, Long> map = new HashMap<String, Long>();
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
        LOGGER.error("isAlive-" + address + path);
        synchronized (CoapServer.class) {
            if (endpoints.containsKey(address)) {
                if (endpoints.get(address).containsKey(path)) {
                    Cache pair = CacheHandler.getInstance().getValue(address, "/" + path);
                    long millis;
                    if (pair == null) {
                        millis = MILLIS_TO_STALE;
                    } else {
                        millis = pair.getMaxAge() * 1000;
                    }
                    if (System.currentTimeMillis() - endpoints.get(address).get(path) > millis) {
                        LOGGER.info("address was stale " + address + " " + path);
                        if (pair != null) {
                            pair.incLostCounter();
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


//    public void endpointIsAlive(final String endpoint, final String path) {
//        LOGGER.info("endpointIsAlive");
//        synchronized (CoapServer.class) {
//            if (endpoints.containsKey(endpoint)) {
//                endpoints.put(endpoint, System.currentTimeMillis());
//            }
//        }
//    }


    void socketSend(DatagramPacket replyPacket) {
        try {
            //garbage-check "Coap Version should be 01 nothing more nothing less"
            byte fbyte = (byte) (replyPacket.getData()[0] & 0xc0);
            if (fbyte == 0x40) {
                socket.send(replyPacket);
                LOGGER.info("socketSend(" + replyPacket.getAddress().getHostAddress() + ":" + replyPacket.getPort());
            }
        } catch (IOException e) {
            LOGGER.error("socketSend(", e);
        }
    }


    /**
     * Sends a reply to a packet using the socket.
     *
     * @param buf           the bytes to send as a reply
     * @param socketAddress the address of the udp client
     */
    public void sendReply(final byte[] buf, final SocketAddress socketAddress) {
        byte[] localBuf = buf.clone();

        LOGGER.info("sending reply to " + socketAddress + " len: " + buf.length);
        final DatagramPacket replyPacket;
        try {
            replyPacket = new DatagramPacket(localBuf, 0, buf.length, socketAddress);
            socketSend(replyPacket);
        } catch (SocketException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    /**
     * Adds the req to the list of active requests.
     *
     * @param address the address from which the req originated.
     * @param req     the req message.
     */
    public void addRequest(final String address, final Message req, final boolean hasQuery) {

        synchronized (CoapServer.class) {
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
//            for (int key : activeRequests.keySet()) {
//
//                if ((activeRequests.get(key).getMid() == req.getMID()) || (req.hasOption(OptionNumberRegistry.TOKEN) && (activeRequests.get(key).getToken().equals(req.getTokenString())))) {
//                    activeRequests.get(key).setUriPath(req.getUriPath());
//                    activeRequests.get(key).setMid(req.getMID());
//                    activeRequests.get(key).setQuery(hasQuery);
//                    activeRequests.get(key).setTimestamp(System.currentTimeMillis());
//                    return;
//                }
//                if (activeRequests.get(key).getUriPath().equals(req.getUriPath()) && activeRequests.get(key).getHost().equals(address)) {
//                    activeRequests.get(key).setMid(req.getMID());
//                    activeRequests.get(key).setTimestamp(System.currentTimeMillis());
//                    activeRequests.get(key).setToken(req.getTokenString());
//                    return;
//                }
//            }
//            ActiveRequest mRequest = new ActiveRequest(req.getUriPath(), req.getMID(), req.getTokenString(), address, hasQuery, System.currentTimeMillis());
//            activeRequests.put(req.getMID(), mRequest);
//            LOGGER.info("Added Active Request For " + mRequest.getHost() + " with mid " + mRequest.getMid() + " path:" + mRequest.getUriPath());
        }
    }

    /**
     * Matches a message to a previously received request.
     *
     * @param response The response received.
     * @return The URI of the request or null.
     */

    public String matchResponse(final Message response) {
        synchronized (CoapServer.class) {
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

    /**
     * Responds to a UDP request using the packet received from the device.
     *
     * @param response      the response received.
     * @param activeRequest the request for the response.
     * @param address
     */
    private void respondToUDP(final Message response, final ActiveRequest activeRequest, String address) {
        if (activeRequest.getSocketAddress() != null) {
            try {
                response.setURI("/" + testbedPrefix + address + activeRequest.getUriPath());
                LOGGER.info("/" + testbedPrefix + address + activeRequest.getUriPath());
                LOGGER.info("Sending Response to: " + activeRequest.getSocketAddress());
                socketSend(new DatagramPacket(response.toByteArray(), response.toByteArray().length, activeRequest.getSocketAddress()));
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
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
        LOGGER.info("sending request");
//        TestbedController.getInstance().sendMessage(payload, nodeUrn);
//        XbeeController.getInstance().sendPayload(nodeUrn,payload);
        mqttSendPayload(nodeUrn, payload);
//        Timer timer = new Timer();
//        timer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                mqtt.sendPayload(nodeUrn, payload);
//            }
//        }, 100);
//        timer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                mqtt.sendPayload(nodeUrn, payload);
//            }
//        }, 250);
    }

    private void sendXbee(XBeeAddress16 address16, int i, int[] bytes, int counter) {

        try {
            XBeeRadio.getInstance().send(address16, i, bytes);
        } catch (Exception e1) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e2) {
                return;
            }
            if (counter > 4) {
                return;
            }
            sendXbee(address16, i, bytes, ++counter);
            LOGGER.error(e1.getMessage(), e1);
        }

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
//        final byte[] data = ack.toByteArray();
//        final int[] bytes = new int[data.length + 1];
//        bytes[0] = 51;
//        for (int i = 0; i < data.length; i++) {
//            final short read = (short) ((short) data[i] & 0xff);
//            bytes[i + 1] = read;
//        }
//        final int[] macAddress = Converter.getInstance().addressToInteger(nodeUrn);
//        final XBeeAddress16 address16 = new XBeeAddress16(macAddress[0], macAddress[1]);
//        LOGGER.info("Sending Ack to " + nodeUrn);
//        try {
//            XBeeRadio.getInstance().send(address16, 112, bytes);
//        } catch (Exception e) {//NOPMD
//            LOGGER.error(e.getMessage(), e);
//        }
    }

    public void registerForResource(final String capability, final String address) {
        {
            URI uri = null;
            try {
                uri = new URI(new StringBuilder().append("/").append(capability).toString());
            } catch (URISyntaxException e) {
                LOGGER.error(e.getLocalizedMessage(), e);
            }
            final Request request = new Request(CodeRegistry.METHOD_GET, false);
            request.setMID(nextMID());
            request.setURI(uri);
//            List<Option> uriPath = Option.split(OptionNumberRegistry.URI_PATH, uri.getPath(), "/");
//            request.setOptions(OptionNumberRegistry.URI_PATH, uriPath);
//            request.setOption(new Option(0, OptionNumberRegistry.OBSERVE));
//            request.setToken(TokenManager.getInstance().acquireToken());

            addRequest(address, request, false);
            sendRequest(request.toByteArray(), address);
        }

    }

    public int nextMID() {
        do {
            currentMID = ++currentMID % 0x10000;
        } while (isReservedMID(currentMID));
        return currentMID;
    }

    private boolean isReservedMID(int currentMID) {
        if (activeRequestsMID.containsKey(currentMID)) return true;
        return false;
    }

//    }

    public void requestForResource(String capability, String address, boolean observe) {
        synchronized (CoapServer.class) {
            LOGGER.info("requestForResource:" + address);
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
                request.setToken(TokenManager.getInstance().acquireToken(address));
                requestObserveCounter++;
//                request.setToken(TokenManager.getInstance().acquireToken());
            }
            request.prettyPrint();
//            ownRequests.put(request.getMID(), uri.toString());
            addRequest(address, request, false);
            LOGGER.info(request.getMID());
            sendRequest(request.toByteArray(), address);


        }
    }

    public String matchMID(int mid) {
        if (ownRequests.containsKey(mid)) {
            String uri = ownRequests.get(mid);
            ownRequests.remove(uri);
            return uri;
        }
        return "";
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

    public void addEthernet(String payload, int mid) {
        LOGGER.info("Adding by mid " + mid);
        ownRequests.put(mid, payload);
    }

    public String checkEthernet(int mid) {
        LOGGER.info("Checking by mid " + mid);
        if (ownRequests.containsKey(mid)) {
            String eth = ownRequests.get(mid);
            ownRequests.remove(mid);
            return eth;
        }
        return "";
    }

    public void addEthernet(String payload, String token) {
        LOGGER.info("Adding by token " + token + " \"" + payload + "\"");
        ownObserves.add(new TokenItem(token, payload));
    }

    //    public boolean isOutside(String address, Message response) {
////        LOGGER.info("Looking for " + address + " with mid " + response.getMID());
////        synchronized (CoapServer.class) {
////            if (activeRequests.isEmpty()) {
////                return false;
////            }
//////            if (!activeRequests.containsKey(address)) return false;
//////
//////            for (ActiveRequest activeRequest : activeRequests.get(destination)) {
//////                if ((response.hasOption(OptionNumberRegistry.TOKEN))
//////                        && (response.getTokenString().equals(activeRequest.getToken()))) {
//////                    if (activeRequest.hasQuery()) {
//////                        return false;
//////                    } else {
//////                        return true;
//////                    }
//////                }
//////                if (response.getMID() == activeRequest.getMid()) {
//////                    String retVal = activeRequest.getUriPath();
//////                    return true;
//////                }
//////            }
////
////        }
////
////        return false;


    public String checkEthernet(String token) {
        LOGGER.info("Checking by token " + token);
        for (TokenItem tokenItem : ownObserves) {
            if (tokenItem.getBytes().equals(token)) {
                return tokenItem.getPath();
            }
        }
        return "";
    }


    public static void main(String[] args) {
        PropertyReader.getInstance().setFile("listener.properties");
        CoapServer.getInstance();
    }

    public void addPending(Integer mid, String remainder) {
        ethernetBlockWisePending.put(mid, remainder);
    }

    public String getPending(int mid) {
        if (ethernetBlockWisePending.containsKey(mid)) {
            String value = ethernetBlockWisePending.get(mid);
            ethernetBlockWisePending.remove(mid);
            return value;
        }
        return "";
    }

    public void ackEthernet(EthernetUDPhandler udPhandler, Response response, SocketAddress address) {
        Message ack = new Message(Message.messageType.ACK, 0);
        ack.setMID(response.getMID());
        try {
            udPhandler.send(ack, address);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void ackEthernet(UDPhandler udPhandler, Response response, String address) {
        Message ack = new Message(Message.messageType.ACK, 0);
        ack.setMID(response.getMID());
        try {
            udPhandler.send(ack, address);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public DatagramSocket getSocket() {
        return socket;
    }

    public String findGateway(final String destination) {
        if (GatewayManager.getInstance().hasGateway(destination)) {
            return GatewayManager.getInstance().getGateway(destination);
        } else {
            return "1ccd";
        }

    }

    public Map<String, Map<String, Long>> getEndpoints() {
        return endpoints;
    }

    public Map<Integer, ActiveRequest> getActiveRequestsMID() {
        return activeRequestsMID;
    }

    public Map<String, ActiveRequest> getActiveRequestsTOKEN() {
        return activeRequestsTOKEN;
    }

    public List<TokenItem> getObservers() {
        return ownObserves;
    }

    public boolean rejectDuplicate(String response) {
        if (duplicates.containsKey(response)) {
            if (System.currentTimeMillis() - duplicates.get(response) > 10 * 1000) {
                duplicates.put(response, System.currentTimeMillis());
                return false;
            } else {
                return true;
            }
        } else {
            duplicates.put(response, System.currentTimeMillis());
            return false;
        }
    }

    public void incRequestWellKnownCounter() {
        requestWellKnownCounter++;
    }

    public int getRequestWellKnownCounter() {
        return requestWellKnownCounter;
    }

    public int getRequestObserveCounter() {
        return requestObserveCounter;
    }

    public void incRequestObserveCounter() {
        requestObserveCounter++;
    }

    public void incResponseObserveCounter() {
        responseObserveCounter++;
    }

    public int getResponseObserveCounter() {
        return responseObserveCounter;
    }

    public int getObserveLostCounter() {
        return observeLostCounter;
    }

    public void sendEthernetRequest(DeviceCommand command) {
//        System.out.println("sending dgram to ");

        InetAddress inetAddr;
        try {
            inetAddr = InetAddress.getByName(command.getDestination().substring(command.getDestination().lastIndexOf(":") + 1));

            String payloadIn = command.getPayload().substring(3);
            final byte[] payload = Converter.getInstance().commaPayloadtoBytes(payloadIn);
            //payload[payload.length-2]-=32;
//            System.out.println("sending dgram with" + Arrays.toString(payload));
            DatagramPacket packet = new DatagramPacket(payload, payload.length, inetAddr, 5683);
            socketSend(packet);
        } catch (UnknownHostException e) {
            LOGGER.error(e, e);
            return;
        } catch (IOException e) {
            LOGGER.error(e, e);
            return;
        }
    }

    public void setEthernetUDPHandler(EthernetUDPhandler ethernetUDPHandler) {
        this.ethernetUDPHandler = ethernetUDPHandler;
    }

    public EthernetUDPhandler getEthernetUDPHandler() {
        return ethernetUDPHandler;
    }

    public void setMqtt(CallbackConnection mqtt) {
        this.mqtt = mqtt;
    }

    public void publish(final String topic, final String message) {
        mqttPublish(topic, message);
    }

    public void registerGateway(String arduinoGateway) {
        if (arduinoGateway.startsWith(CONNECTED_ARDUINO_GATEWAY_STRING)) {

            String urnPrefix = null;
            try {
                String mac = (arduinoGateway.substring(arduinoGateway.indexOf(CONNECTED_ARDUINO_GATEWAY_STRING) + CONNECTED_ARDUINO_GATEWAY_STRING.length())).split(":")[0];
                String testbedID = (arduinoGateway.substring(arduinoGateway.indexOf(CONNECTED_ARDUINO_GATEWAY_STRING) + CONNECTED_ARDUINO_GATEWAY_STRING.length())).split(":")[1];

                urnPrefix = UberdustClient.getInstance().getUrnPrefix(Integer.parseInt(testbedID));
                final String nodeUrn = urnPrefix + "0x" + mac;
                final String capabilityName = "connect";
                final eu.uberdust.communication.protobuf.Message.NodeReadings.Reading reading = eu.uberdust.communication.protobuf.Message.NodeReadings.Reading.newBuilder()
                        .setNode(nodeUrn)
                        .setCapability(capabilityName)
                        .setTimestamp(System.currentTimeMillis())
                        .setDoubleReading(Double.parseDouble(testbedID))
                        .build();
                new WsCommiter(reading);
            } catch (JSONException e) {
                LOGGER.error(e, e);
            }
        } else if (arduinoGateway.startsWith("xmess")) {
            arduinoGateway = arduinoGateway.replaceAll("xmess:", "");
            xCount.put(arduinoGateway.split(":")[0], arduinoGateway.split(":")[1]);
        } else if (arduinoGateway.startsWith("ymess")) {
            arduinoGateway = arduinoGateway.replaceAll("ymess:", "");
            xCount.put(arduinoGateway.split(":")[0], arduinoGateway.split(":")[1]);
        } else {
            arduinoGateways.put(arduinoGateway, System.currentTimeMillis());
        }
    }

    public Map<String, Long> getArduinoGateways() {
        return arduinoGateways;
    }

    public Map<String, String> getXCount() {
        return xCount;
    }
    public Map<String, String> getYCount() {
        return yCount;
    }

    public class TokenItem {
        String bytes;
        String path;

        TokenItem(String bytes, String path) {
            this.bytes = bytes;
            this.path = path;
        }

        public String getBytes() {
            return bytes;
        }

        public String getPath() {
            return path;
        }
    }


    public void mqttSendPayload(final String destination, final byte[] payloadIn) {
        byte[] destinationBytes = Converter.getInstance().addressToByte(destination);
        byte[] payloadWithDestination = new byte[payloadIn.length + 2];
        LOGGER.debug("mqttSendPayload");
        payloadWithDestination[0] = destinationBytes[1];
        payloadWithDestination[1] = destinationBytes[0];
        System.arraycopy(payloadIn, 0, payloadWithDestination, 2, payloadIn.length);

        mqtt.publish("arduinoGateway", payloadWithDestination, QoS.AT_MOST_ONCE, false, new Callback() {
            @Override
            public void onSuccess(Object o) {
                LOGGER.info("onSuccess");
            }

            @Override
            public void onFailure(Throwable throwable) {
                LOGGER.info("onFailure");
            }
        });
    }


    public void mqttPublish(final String topic, final String message) {
        LOGGER.debug("mqttPublish");
        mqtt.publish(topic, message.getBytes(), QoS.AT_MOST_ONCE, false, new Callback() {

            @Override
            public void onSuccess(Object value) {
                LOGGER.info("onSuccess");
            }

            @Override
            public void onFailure(Throwable value) {
                LOGGER.error("onFailure", value);
            }
        });
    }

}
