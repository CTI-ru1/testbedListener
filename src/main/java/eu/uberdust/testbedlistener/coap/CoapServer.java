package eu.uberdust.testbedlistener.coap;

import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.Response;
import eu.uberdust.DeviceCommand;
import eu.uberdust.testbedlistener.HeartBeatJob;
import eu.uberdust.testbedlistener.coap.udp.EthernetUDPhandler;
import eu.uberdust.testbedlistener.coap.udp.UDPhandler;
import eu.uberdust.testbedlistener.datacollector.collector.CollectorMqtt;
import eu.uberdust.testbedlistener.mqtt.MqttConnectionManager;
import eu.uberdust.testbedlistener.util.Converter;
import eu.uberdust.testbedlistener.util.PropertyReader;
import org.apache.log4j.Logger;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CoAP Server Class.
 * Used to Manage Connections and control most operations between diffrent {@see CollectorMqtt} threads and external Request.
 *
 * @author Dimitrios Amaxilatis,Dimitrios Giannakopoulos
 * @Date 5/19/12
 */
public class CoapServer {

    /**
     * LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(CoapServer.class);
    protected final static String MQTT_SEPARATOR = "-";

    /**
     * Singleton instance.
     */
    private static CoapServer instance = null;

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
     * All the connected collectors for every different Gateway Device.
     */
    Map<String, CollectorMqtt> collectors;

    /**
     * CoAP Server socket.
     */
    private transient DatagramSocket socket;

    private EthernetUDPhandler ethernetUDPHandler;

    private final Map<Integer, String> ethernetBlockWisePending;
    private final Map<Integer, String> ownRequests;

    private final List<TokenItem> ownObserves;

    private final Map<String, Long> duplicates;

    /**
     * Boot tile for status updates.
     */
    private final long startTime;
    /**
     * Quartz {@see SchedulerFactory} for heartbeats.
     */
    private final StdSchedulerFactory schFactory;

    /**
     * Quartz {@see Scheduler} for heartbeats.
     */
    private Scheduler sch;
    /**
     * All the Gateway Devices Connected.
     */
    private Map<String, Long> arduinoGateways;

    /**
     * Key-Value store for Statistics received from Gateway Devices.
     */
    private Map<String, Map<String, String>> arduinoGatewayStats;

    private Map<String, String> xCount, yCount;

    public long getStartTime() {
        return startTime;
    }

    /**
     * Constructor.
     */
    public CoapServer() {

        this.startTime = System.currentTimeMillis();

        ownRequests = new HashMap<>();
        ownObserves = new ArrayList<>();

        this.collectors = new HashMap<>();
        this.ethernetBlockWisePending = new HashMap<>();
        this.arduinoGateways = new HashMap<>();
        this.arduinoGatewayStats = new HashMap<>();

        xCount = new HashMap<>();
        yCount = new HashMap<>();

        duplicates = new HashMap<String, Long>();

        //Create and Schedule a Job for the HeartBeats
        this.schFactory = new StdSchedulerFactory();
        try {
            sch = schFactory.getScheduler();
            sch.start();
        } catch (SchedulerException e) {
            LOGGER.error(e, e);
        }
        JobDetail heartbeatToGatewaysJob = JobBuilder.newJob(HeartBeatJob.class).withIdentity("heartbeatToGatewaysJob").build();
        try {
            //Trigger the job to run on the next round minute
            Trigger trigger = TriggerBuilder.newTrigger().withSchedule(
                    SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(1).repeatForever())
                    .build();
            sch.scheduleJob(heartbeatToGatewaysJob, trigger);

        } catch (SchedulerException e) {
            LOGGER.error(e, e);
        }

        //Start the CoAP udp socket
        try {
            socket = new DatagramSocket(5683);
        } catch (SocketException e) {
            LOGGER.error(e.getMessage(), e);
        }

        //TODO: needed ?
        //GatewayManager.getInstance();

        //Start listening for incoming CoAP requests!
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

        LOGGER.info("CoapServer Booted up!");
    }

    public void socketSend(DatagramPacket replyPacket) {
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
     * Responds to a UDP request using the packet received from the device.
     *
     * @param response      the response received.
     * @param activeRequest the request for the response.
     * @param address
     */
    private void respondToUDP(final Message response, final ActiveRequest activeRequest, String address) {
        if (activeRequest.getSocketAddress() != null) {
            try {
                response.setURI("/" + address + activeRequest.getUriPath());
                LOGGER.info("/" + address + activeRequest.getUriPath());
                LOGGER.info("Sending Response to: " + activeRequest.getSocketAddress());
                socketSend(new DatagramPacket(response.toByteArray(), response.toByteArray().length, activeRequest.getSocketAddress()));
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
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
            LOGGER.error(e, e);
        }
    }

    public void ackEthernet(UDPhandler udPhandler, Response response, String address) {
        Message ack = new Message(Message.messageType.ACK, 0);
        ack.setMID(response.getMID());
        try {
            udPhandler.send(ack, address);
        } catch (IOException e) {
            LOGGER.error(e, e);
        }
    }

    public DatagramSocket getSocket() {
        return socket;
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

    public void sendEthernetRequest(DeviceCommand command) {

        InetAddress inetAddr;
        try {
            inetAddr = InetAddress.getByName(command.getDestination().substring(command.getDestination().lastIndexOf(":") + 1));

            final String payloadIn = command.getPayload().substring(3);
            final byte[] payload = Converter.getInstance().commaPayloadtoBytes(payloadIn);
            //payload[payload.length-2]-=32;
//            System.out.println("sending dgram with" + Arrays.toString(payload));
            final DatagramPacket packet = new DatagramPacket(payload, payload.length, inetAddr, 5683);
            socketSend(packet);
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

    public void registerGateway(boolean isNew, String deviceId) {
        LOGGER.info("put " + deviceId);

        arduinoGateways.put(deviceId, System.currentTimeMillis());

        final String key = deviceId;


        if (!collectors.containsKey(key)) {
            final CollectorMqtt aCollector = new CollectorMqtt(deviceId);
            MqttConnectionManager.getInstance().listen(key + "/#", aCollector);
            collectors.put(key, aCollector);
        }

    }

    public Map<String, Map<String, String>> getArduinoGatewayStats() {
        return arduinoGatewayStats;
    }

    public void appendGatewayStat(final String deviceId, final String key, final String value) {
        LOGGER.info("put " + deviceId + " key: " + key + " value: " + value);
        if (!arduinoGatewayStats.containsKey(deviceId)) {
            arduinoGatewayStats.put(deviceId, new HashMap<String, String>());
        }
        arduinoGatewayStats.get(deviceId).put(key, value);
    }

    public Map<String, Long> getArduinoGateways() {
        return arduinoGateways;
    }

    public Map<String, CollectorMqtt> getCollectors() {
        return collectors;
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
}