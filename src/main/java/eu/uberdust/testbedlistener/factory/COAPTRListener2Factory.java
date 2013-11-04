package eu.uberdust.testbedlistener.factory;

import eu.uberdust.communication.UberdustClient;
import eu.uberdust.testbedlistener.coap.CoapServer;
import eu.uberdust.testbedlistener.datacollector.collector.mqtt.listener.MqttConnectionManager;
import eu.uberdust.testbedlistener.datacollector.collector.mqtt.listener.MqttDeviceConnectionListener;
import eu.uberdust.testbedlistener.datacollector.collector.mqtt.listener.MqttStatsListener;
import eu.uberdust.testbedlistener.util.PropertyReader;
import org.json.JSONArray;

import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * The testbed Listener Connects to Uberdust and Testbed Runtime to forward command and readings Both Ways.
 */
public class COAPTRListener2Factory extends AbstractListenerFactory {

    /**
     * Logger.
     */
    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(COAPTRListener2Factory.class);

    /**
     * ENABLED backend identifier.
     */
    private static final String ENABLED = "1";

    /**
     * Starts the application.
     */
    @Override
    public void run() {

        final String server = PropertyReader.getInstance().getProperties().getProperty("uberdust.server");
        final String port = PropertyReader.getInstance().getProperties().getProperty("uberdust.port");
        final String testbedBasePath = PropertyReader.getInstance().getProperties().getProperty("uberdust.basepath");

        LOGGER.info("Starting Coap Server");
        CoapServer.getInstance();
        UberdustClient.setUberdustURL("http://" + server + ":" + port + testbedBasePath);


//        if (PropertyReader.getInstance().getProperties().get("use.controller").equals("1") ||
//                PropertyReader.getInstance().getProperties().get("use.datacollector").equals("1")) {
//            LOGGER.info("Connecting Network Manager");
//            JSONArray testbeds = null;
//            NetworkManager.getInstance().start(server + ":" + port + testbedBasePath, 5);
////            try {
////                testbeds = new JSONArray(UberdustRestClient.getInstance().callRestfulWebService("http://" + server + ":" + port + testbedBasePath + "rest/testbed/json"));
////                for (int i = 0; i < testbeds.length(); i++) {
////                    NetworkManager.getInstance().listenFor(i+1);
////                }
////            } catch (JSONException e) {
////                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
////            }
//
//        }

//        //Awaits for commands from Uberdust.
//        if (ENABLED.equals(PropertyReader.getInstance().getProperties().get("use.controller"))) {
//            LOGGER.info("addObserver");
//            NetworkManager.getInstance().addObserver(TestbedController.getInstance());
//        }


        //Listens to new Messages from the TestbedRuntime
        if (ENABLED.equals(PropertyReader.getInstance().getProperties().get("use.datacollector"))) {
            LOGGER.info("Starting Collectors...");

            try {
                final boolean hasMqttBroker = !PropertyReader.getInstance().getProperties().getProperty("mqtt.broker").isEmpty();
                if (hasMqttBroker) {
                    LOGGER.info("MqttConnector...");
                    final String mqttBroker = PropertyReader.getInstance().getProperties().getProperty("mqtt.broker");
                    MqttConnectionManager.getInstance().connect(mqttBroker);

                    JSONArray testbeds = null;
//                    try {
//                        testbeds = new JSONArray(UberdustRestClient.getInstance().callRestfulWebService("http://" + server + ":" + port + testbedBasePath + "rest/testbed/json"));
//                        for (int i = 0; i < testbeds.length(); i++) {
////                            MqttCollector mqList = new MqttCollector(mqttBroker, i + 1);
////                            new Thread(mqList).start();
//                        }
                    MqttConnectionManager.getInstance().listen("stats/#", new MqttStatsListener());
                    MqttConnectionManager.getInstance().listen("connect/#", new MqttDeviceConnectionListener());

//                    } catch (JSONException e) {
//                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//                    }
//
                }
            } catch (NullPointerException npe) {
                LOGGER.warn("mqtt.broker property is not defined");
                LOGGER.error(npe, npe);
            }

            DatagramSocket ds;
            try {
                ds = new DatagramSocket(6665);
            } catch (SocketException e) {
                LOGGER.info("exiting...");
                return;
            }

            PropertyReader.getInstance().setFile("listener.properties");

        }


        LOGGER.info("up and running");
    }

    public static void main(String[] args) {
        PropertyReader.getInstance().setFile("listener.properties");
        Thread thread = new Thread(new COAPTRListener2Factory());
        thread.start();
    }
}
