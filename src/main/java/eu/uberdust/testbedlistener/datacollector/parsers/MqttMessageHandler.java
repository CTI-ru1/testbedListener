//package eu.uberdust.testbedlistener.datacollector.parsers;
//
//import eu.uberdust.communication.UberdustClient;
//import eu.uberdust.testbedlistener.util.HereIamMessage;
//import org.apache.log4j.Logger;
//import org.fusesource.hawtbuf.Buffer;
//import org.fusesource.hawtbuf.UTF8Buffer;
//import org.json.JSONArray;
//import org.json.JSONException;
//
//import java.io.IOException;
//import java.util.Arrays;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
///**
// * Created with IntelliJ IDEA.
// * User: amaxilatis
// * Date: 6/7/13
// * Time: 1:02 PM
// * To change this template use File | Settings | File Templates.
// */
//public class MqttMessageHandler implements org.fusesource.mqtt.client.Listener {
//    /**
//     * LOGGER.
//     */
//    private static final Logger LOGGER = Logger.getLogger(MqttMessageHandler.class);
//
//    public MqttMessageHandler(int testbedID) {
//
//
//    }
//
//    public void onConnected() {
//        LOGGER.trace("Listener onConnected");
//    }
//
//    public void onDisconnected() {
//        LOGGER.trace("Listener onDisconnected");
//    }
//
//
//    public void onFailure(Throwable value) {
//        LOGGER.error("Listener onFailure: ", value);
////        done.countDown();
//    }
//}
