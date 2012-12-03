package eu.uberdust.testbedlistener.coap;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import com.rapplogic.xbee.api.XBeeAddress16;
import eu.mksense.XBeeRadio;
import eu.uberdust.testbedlistener.coap.udp.UDPhandler;
import eu.uberdust.testbedlistener.controller.TestbedController;
import eu.uberdust.testbedlistener.util.PropertyReader;
import eu.uberdust.testbedlistener.util.TokenManager;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.MalformedURLException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

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
    private transient final List<ActiveRequest> activeRequests;
    /**
     * COAP Server socket.
     */
    private transient DatagramSocket socket;

    /**
     * Random number generator.
     */
    //private transient final Random mid;
    private String testbedPrefix;
    private static final long MILLIS_TO_STALE = 1 * 60 * 1000;
    private Map<Integer, String> ownRequests;
    private ArrayList<TokenItem> ownObserves;
    private Map<String, String> blockWisePending;
    private Map<Integer, String> ethernetBlockWisePending;
    private Map<String, String> gateways;
    private Map<String, Long> duplicates;
    private int currentMID;
    private Timer cleanupTimer;

    /**
     * Constructor.
     */
    public CoapServer() {
        ownRequests = new HashMap<Integer, String>();
        ownObserves = new ArrayList<TokenItem>();
        this.endpoints = new HashMap<String, Map<String, Long>>();
        blockWisePending = new HashMap<String, String>();
        ethernetBlockWisePending = new HashMap<Integer, String>();
        this.activeRequests = new ArrayList<ActiveRequest>();
        this.testbedPrefix = PropertyReader.getInstance().getTestbedPrefix();
        this.duplicates = new HashMap<String, Long>();
        currentMID = (int) (Math.random() * 0x10000);
        this.cleanupTimer = new Timer();
//        Timer discoveryTimer = new Timer();
//        discoveryTimer.scheduleAtFixedRate(new BroadcastCoapRequest(), 20000, 60000);

        //Start the udp socket
        try {
            socket = new DatagramSocket(5683);
        } catch (SocketException e) {
            LOGGER.error(e.getMessage(), e);
        }

        loadGateways();
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

        Timer activeRequestCleanupTimer = new Timer();
        activeRequestCleanupTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                CoapServer.getInstance().cleanActiveRequests();
            }
        }, 60000, 60000);

        LOGGER.info("started CoapServer");
    }

    public void cleanActiveRequests() {
        LOGGER.info("Cleaning acrive Requests");
        for (ActiveRequest activeRequest : activeRequests) {
            if (System.currentTimeMillis() - activeRequest.getTimestamp() > 3 * 60 * 1000) {

//                try {
//                    TokenManager.getInstance().releaseToken(Hex.decodeHex(activeRequest.getToken().toCharArray()));
//                } catch (DecoderException e) {
//
//                }
                activeRequests.remove(activeRequest);
            }
        }


    }

    private void loadGateways() {
        gateways = new HashMap<String, String>();
        loadGatewaysFromUberdust();
        loadGatewaysFromFile();
    }

    private void loadGatewaysFromUberdust() {
        try {
            URL url = new URL(new StringBuilder()
                    .append("http://")
                    .append(PropertyReader.getInstance().getProperties().get("uberdust.server"))
                    .append(PropertyReader.getInstance().getProperties().get("uberdust.basepath"))
                    .append("/rest/testbed/")
                    .append(PropertyReader.getInstance().getProperties().get("wisedb.testbedid"))
                    .append("/capability/gateway/tabdelimited").toString());
            final URLConnection con = url.openConnection();
            final InputStream out = con.getInputStream();
            final StringBuilder response = new StringBuilder();
            int str = out.read();
            while (str != -1) {
                response.append((char) str);
                str = out.read();
            }

            out.close();
            final String[] splitted = response.toString().split("\n");
            for (final String row : splitted) {
                String urn = row.split("\t")[0];
                urn = urn.substring(urn.lastIndexOf(":0x") + 3);
                final String value = row.split("\t")[3];
                if ("1.0".equals(value)) {
                    gateways.put(urn, urn);
                }
            }
        } catch (MalformedURLException e) {
            LOGGER.error(e, e);
        } catch (IOException e) {
            LOGGER.error(e, e);
        }
    }

    private void loadGatewaysFromFile() {
        BufferedReader bin = null;
        try {
            bin = new BufferedReader(new FileReader("gateways"));
            String str = bin.readLine();
            while (str != null) {
                if (str.contains(",")) {
                    final String source = str.split(",")[0].substring(str.split(",")[0].lastIndexOf(":0x") + 3);
                    final String from = str.split(",")[1].substring(str.split(",")[1].lastIndexOf(":0x") + 3);
                    gateways.put(source, from);

                } else {
                    if (str.contains(":")) {
                        str = str.substring(str.lastIndexOf(":0x") + 3);
                    }
                    gateways.put(str, str);
                }
                LOGGER.info("adding " + str);
                str = bin.readLine();
            }
        } catch (FileNotFoundException e) {
            LOGGER.error(e, e);
        } catch (IOException e) {
            LOGGER.error(e, e);
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
        LOGGER.error("registerEndpoint-" + address + path);

        synchronized (CoapServer.class) {
            if (endpoints.containsKey(address)) {
                if (endpoints.get(address).containsKey(path)) {
                    if (System.currentTimeMillis() - endpoints.get(address).get(path) > MILLIS_TO_STALE) {
                        endpoints.get(address).put(path, System.currentTimeMillis());
                        LOGGER.info("address was stale " + address + " " + path);
                        return true;
                    } else {
//                        endpoints.get(address).put(path, System.currentTimeMillis());
                        return false;
                    }
                } else {
                    endpoints.get(address).put(path, System.currentTimeMillis());
                    return true;
                }
            } else {
                LOGGER.info("inserting address");
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
                    if (System.currentTimeMillis() - endpoints.get(address).get(path) > MILLIS_TO_STALE) {
                        LOGGER.info("address was stale " + address + " " + path);
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

    /**
     * Sends a reply to a packet using the socket.
     *
     * @param replyPacket the packet to use to reply.
     */

    public void sendReply(final DatagramPacket replyPacket) {
        synchronized (CoapServer.class) {
            try {
                socket.send(replyPacket);
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Sends a reply to a packet using the socket.
     *
     * @param buf           the bytes to send as a reply
     * @param socketAddress the address of the udp client
     */
    public void sendReply(final byte[] buf, final SocketAddress socketAddress) {
        byte[] buf1 = new byte[10124];
        LOGGER.info("sending reply to " + socketAddress + " len: " + buf.length);
        final DatagramPacket replyPacket;
        try {
            replyPacket = new DatagramPacket(buf, 0, buf.length, socketAddress);

            synchronized (CoapServer.class) {
                try {
                    CoapServer.getInstance().getSocket().send(replyPacket);
                    LOGGER.info("reply Sent");
                } catch (IOException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    /**
     * Adds the req to the list of active requests.
     *
     * @param address  the address from which the req originated.
     * @param req      the req message.
     * @param sAddress the socket address of the sender if any
     */
    public void addRequest(final String address, final Message req, final SocketAddress sAddress, final boolean hasQuery) {

        synchronized (CoapServer.class) {
            for (ActiveRequest activeRequest : activeRequests) {
                if ((activeRequest.getMid() == req.getMID()) || (activeRequest.getToken().equals(req.getToken()))) {
                    activeRequest.setUriPath(req.getUriPath());
                    activeRequest.setMid(req.getMID());
                    activeRequest.setSocketAddress(sAddress);
                    activeRequest.setQuery(hasQuery);
                    activeRequest.setTimestamp(System.currentTimeMillis());
                    return;
                }
                if (activeRequest.getUriPath().equals(req.getUriPath()) && activeRequest.getHost().equals(address)) {
                    if (activeRequest.getSocketAddress() == sAddress) {
                        activeRequest.setMid(req.getMID());
                        activeRequest.setTimestamp(System.currentTimeMillis());
                        activeRequest.setToken(req.getTokenString());
                        return;
                    }
                }
            }
            ActiveRequest mRequest = new ActiveRequest(req.getUriPath(), req.getMID(), req.getTokenString(), address, sAddress, hasQuery, System.currentTimeMillis());
            activeRequests.add(mRequest);
            LOGGER.info("Added Active Request For " + mRequest.getHost() + " with mid " + mRequest.getMid() + " path:" + mRequest.getUriPath());
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
            if (activeRequests.isEmpty()) {
                LOGGER.info("no active request");
                return null;
            }
//            final byte[] payload = response.getPayload();
            String responseTokenString = response.getTokenString();
//            LOGGER.info(response.getPayloadString());
//            LOGGER.info(response.hasOption(OptionNumberRegistry.TOKEN));
//            LOGGER.info(response.getOptionCount());
//            LOGGER.info(address);
            for (ActiveRequest activeRequest : activeRequests) {
                if ((response.hasOption(OptionNumberRegistry.TOKEN))
                        && (responseTokenString.equals(activeRequest.getToken()))) {
                    LOGGER.info("Found By Token " + responseTokenString + "==" + activeRequest.getToken());
//                    response.setPayload(payload);
//                    LOGGER.info(response.getPayloadString());

                    if (activeRequest.getMid() == response.getMID()) return "";

                    activeRequest.setMid(response.getMID());
                    if (activeRequest.hasQuery()) {
                        return null;
                    } else {
                        return activeRequest.getHost() + "," + activeRequest.getUriPath();
                    }
                }
//                LOGGER.info(activeRequest.getMid() + "--" + response.getMID());
                if (response.getMID() == activeRequest.getMid()) {
                    String retVal = activeRequest.getHost() + "," + activeRequest.getUriPath();
                    if (activeRequest.hasQuery()) {
                        retVal = null;
                    }
//                    LOGGER.info("Found By MID" + retVal + " " + activeRequest.getHost());
                    try {
                        activeRequests.remove(activeRequest);
                    } catch (Exception e) {
                        LOGGER.error(e, e);
                    }
                    return retVal;
                }
            }
            return "";
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
                socket.send(new DatagramPacket(response.toByteArray(), response.toByteArray().length, activeRequest.getSocketAddress()));
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
        TestbedController.getInstance().sendMessage(payload, nodeUrn);
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                TestbedController.getInstance().sendMessage(payload, nodeUrn);
            }
        }, 1000);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                TestbedController.getInstance().sendMessage(payload, nodeUrn);
            }
        }, 2500);
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

            addRequest(address, request, null, false);
            sendRequest(request.toByteArray(), address);
        }

    }

    public int nextMID() {
        currentMID = ++currentMID % 0x10000;

        return currentMID;
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
                request.setToken(TokenManager.getInstance().acquireToken());
//                request.setToken(TokenManager.getInstance().acquireToken());
            }
            request.prettyPrint();
//            ownRequests.put(request.getMID(), uri.toString());
            addRequest(address, request, null, false);
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
        if (gateways.containsKey(destination)) {
            return gateways.get(destination);
        } else {
            return "1ccd";
        }

    }

    public Map<String, Map<String, Long>> getEndpoints() {
        return endpoints;
    }

    public List<ActiveRequest> getActiveRequests() {
        return activeRequests;
    }

    public ArrayList<TokenItem> getObservers() {
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


    class TokenItem {
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

}
