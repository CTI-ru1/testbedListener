//package eu.uberdust.testbedlistener.factory;
//
//import eu.mksense.XBeeRadio;
//import eu.uberdust.communication.UberdustClient;
//import eu.uberdust.communication.rest.UberdustRestClient;
//import eu.uberdust.testbedlistener.coap.CoapServer;
//import eu.uberdust.testbedlistener.coap.udp.EthernetUDPhandler;
//import eu.uberdust.testbedlistener.datacollector.collector.XbeeCollector;
//import eu.uberdust.testbedlistener.datacollector.collector.mqtt.listener.MqttDeviceConnectionListener;
//import eu.uberdust.testbedlistener.datacollector.collector.mqtt.listener.MqttStatsListener;
//import eu.uberdust.testbedlistener.util.PropertyReader;
//import org.json.JSONArray;
//import org.json.JSONException;
//
//import java.net.DatagramSocket;
//import java.net.SocketException;
//
///**
// * The testbed Listener Connects to Uberdust and Testbed Runtime to forward command and readings Both Ways.
// */
//public class COAPTRListenerFactory extends AbstractListenerFactory {
//
//    /**
//     * Logger.
//     */
//    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(COAPTRListenerFactory.class);
//
//    /**
//     * ENABLED backend identifier.
//     */
//    private static final String ENABLED = "1";
//
//    /**
//     * Starts the application.
//     */
//    @Override
//    public void run() {
//
//        final String server = PropertyReader.getInstance().getProperties().getProperty("uberdust.server");
//        final String port = PropertyReader.getInstance().getProperties().getProperty("uberdust.port");
//        final String testbedBasePath = PropertyReader.getInstance().getProperties().getProperty("uberdust.basepath");
//
//        LOGGER.info("Starting Coap Server");
//        CoapServer.getInstance();
//        UberdustClient.setUberdustURL("http://" + server + ":" + port + testbedBasePath);
//
//
////        if (PropertyReader.getInstance().getProperties().get("use.controller").equals("1") ||
////                PropertyReader.getInstance().getProperties().get("use.datacollector").equals("1")) {
////            LOGGER.info("Connecting Network Manager");
////            JSONArray testbeds = null;
////            NetworkManager.getInstance().start(server + ":" + port + testbedBasePath, 5);
//////            try {
//////                testbeds = new JSONArray(UberdustRestClient.getInstance().callRestfulWebService("http://" + server + ":" + port + testbedBasePath + "rest/testbed/json"));
//////                for (int i = 0; i < testbeds.length(); i++) {
//////                    NetworkManager.getInstance().listenFor(i+1);
//////                }
//////            } catch (JSONException e) {
//////                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//////            }
////
////        }
//
////        //Awaits for commands from Uberdust.
////        if (ENABLED.equals(PropertyReader.getInstance().getProperties().get("use.controller"))) {
////            LOGGER.info("addObserver");
////            NetworkManager.getInstance().addObserver(TestbedController.getInstance());
////        }
//
//
//        //Listens to new Messages from the TestbedRuntime
//        if (ENABLED.equals(PropertyReader.getInstance().getProperties().get("use.datacollector"))) {
//            LOGGER.info("Starting Collectors...");
//
////            try {
////                final boolean hasTestbedRuntime = !PropertyReader.getInstance().getProperties().getProperty("testbed.hostname").isEmpty();
////                if (hasTestbedRuntime) {
////                    LOGGER.info("TestbedRuntimeCollector...");
////                    final Thread dataCollector = new Thread(new TestbedRuntimeCollector());
////                    dataCollector.start();
////                }
////            } catch (NullPointerException npe) {
////                LOGGER.warn("testbed.hostname property is not defined");
////            }
//
//            try {
//                final boolean hasMqttBroker = !PropertyReader.getInstance().getProperties().getProperty("mqtt.broker").isEmpty();
//                if (hasMqttBroker) {
//                    LOGGER.info("MqttCollector...");
//                    final String mqttBroker = PropertyReader.getInstance().getProperties().getProperty("mqtt.broker");
//
//                    JSONArray testbeds = null;
//                    try {
//                        testbeds = new JSONArray(UberdustRestClient.getInstance().callRestfulWebService("http://" + server + ":" + port + testbedBasePath + "rest/testbed/json"));
//                        for (int i = 0; i < testbeds.length(); i++) {
////                            MqttCollector mqList = new MqttCollector(mqttBroker, i + 1);
////                            new Thread(mqList).start();
//                        }
//                        MqttStatsListener statsListener = new MqttStatsListener(mqttBroker);
//                        new Thread(statsListener).start();
//                        MqttDeviceConnectionListener connectListener = new MqttDeviceConnectionListener(mqttBroker);
//                        new Thread(connectListener).start();
//
//                    } catch (JSONException e) {
//                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//                    }
//
//                }
//            } catch (NullPointerException npe) {
//                LOGGER.warn("mqtt.broker property is not defined");
//            }
//
//            try {
//                final boolean hasXbee = !PropertyReader.getInstance().getProperties().getProperty("xbee.port").isEmpty();
//                if (hasXbee) {
//                    final String xbeePort = PropertyReader.getInstance().getProperties().getProperty("xbee.port");
//                    final String xbeeBaudrate = PropertyReader.getInstance().getProperties().getProperty("xbee.baudrate");
//                    try {
//                        XBeeRadio.getInstance().open(xbeePort, Integer.parseInt(xbeeBaudrate));
//                        XBeeRadio.getInstance().setChannel(12);
//                        final Thread xbeeCollector = new Thread(new XbeeCollector());
//                        xbeeCollector.start();
//                    } catch (Exception e) {
//                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//                    }
//                }
//            } catch (NullPointerException npe) {
//                LOGGER.warn("xbee.port property is not defined");
//            }
//
//            DatagramSocket ds;
//            try {
//                ds = new DatagramSocket(6665);
//            } catch (SocketException e) {
//                LOGGER.info("exiting...");
//                return;
//            }
//
//            PropertyReader.getInstance().setFile("listener.properties");
//            String devices = (String) PropertyReader.getInstance().getProperties().get("polldevices");
//            if (devices != null) {
//                EthernetUDPhandler udphandler = new EthernetUDPhandler(ds);
//                udphandler.start();
//                CoapServer.getInstance().setEthernetUDPHandler(udphandler);
//
//                for (String device : devices.split(",")) {
////                    (new EthernetSupport(udphandler, device)).start();
//                }
//            }
//
//        }
//
//
//        LOGGER.info("up and running");
//    }
//
//    public static void main(String[] args) {
//        PropertyReader.getInstance().setFile("listener.properties");
//        Thread thread = new Thread(new COAPTRListenerFactory());
//        thread.start();
//    }
//}
