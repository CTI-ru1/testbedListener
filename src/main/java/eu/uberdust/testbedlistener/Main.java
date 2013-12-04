package eu.uberdust.testbedlistener;

import com.sensorflare.mq.RabbitMQManager;
import eu.uberdust.testbedlistener.coap.CoapServer;
import eu.uberdust.testbedlistener.mq.listener.RabbitMQCommandsListener;
import eu.uberdust.testbedlistener.mqtt.MqttConnectionManager;
import eu.uberdust.testbedlistener.mqtt.listener.DeviceConnectionMqttListener;
import eu.uberdust.testbedlistener.mqtt.listener.StatsMqttListener;
import eu.uberdust.testbedlistener.util.PropertyReader;
import org.apache.log4j.PropertyConfigurator;

import java.io.IOException;

/**
 * Main class for the Testbedlistener application.
 * Starts a new Listener.
 */
public class Main {

    /**
     * Logger.
     */
    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(Main.class);
    /**
     * ENABLED backend identifier.
     */
    private static final String ENABLED = "1";
    /**
     * The porperty file to use.
     */
    private static final String PROPERTY_FILE = "listener.properties";

    /**
     * Starts the application.
     * Launches different listeners based on the type property provided.
     *
     * @param args not used.
     * @see Main#PROPERTY_FILE
     */
    public static void main(final String[] args) {
        PropertyReader.getInstance().setFile(PROPERTY_FILE);
//        BasicConfigurator.configure();
        PropertyConfigurator.configure("log4j.properties");

        final String uberdustServer = PropertyReader.getInstance().getProperties().getProperty("uberdust.uberdustServer");
        final String uberdustPort = PropertyReader.getInstance().getProperties().getProperty("uberdust.uberdustPort");

        final String rabbitMQServer = PropertyReader.getInstance().getProperties().getProperty("rabbitmq.server");
        final String rabbitMQPort = PropertyReader.getInstance().getProperties().getProperty("rabbitmq.port");
        final String rabbitMQuser = PropertyReader.getInstance().getProperties().getProperty("rabbitmq.user", "guest");
        final String rabbitMQpassword = PropertyReader.getInstance().getProperties().getProperty("rabbitmq.password", "guest");

        //Connect the RabbitMQ Broker
        RabbitMQManager.getInstance().connect(rabbitMQServer, rabbitMQPort, rabbitMQuser, rabbitMQpassword);
        LOGGER.info("RabbitMQ connected");
        try {
            RabbitMQManager.getInstance().registerObserver("commands", new RabbitMQCommandsListener());
        } catch (IOException e) {
            LOGGER.error(e, e);
        }

        //Start the CoAPServer
        CoapServer.getInstance();
        LOGGER.info("CoAPServer Started");

        //Connect the MQTT Broker
        final String myMQTTServer = PropertyReader.getInstance().getProperties().getProperty("mqtt.server");
        final String myMQTTPort = PropertyReader.getInstance().getProperties().getProperty("mqtt.port", "1883");
        final String myMQTTBroker = "tcp://" + myMQTTServer + ":" + myMQTTPort;

        LOGGER.info("Connecting to MQTT Broker {" + myMQTTBroker + "}...");

        MqttConnectionManager.getInstance().connect(myMQTTBroker);

        MqttConnectionManager.getInstance().listen("stats/#", new StatsMqttListener());
        MqttConnectionManager.getInstance().listen("connect/#", new DeviceConnectionMqttListener());
        LOGGER.info("MQTT Broker Connected!");


        LOGGER.info("All Systems up and running!");

    }
}
