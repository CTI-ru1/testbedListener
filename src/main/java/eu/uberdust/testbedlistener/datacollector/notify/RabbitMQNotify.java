package eu.uberdust.testbedlistener.datacollector.notify;

import ch.ethz.inf.vs.californium.coap.Message;
import com.sensorflare.mq.RabbitMQManager;
import eu.uberdust.testbedlistener.datacollector.collector.CollectorMqtt;
import org.apache.log4j.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: amaxilatis
 * Date: 11/29/12
 * Time: 3:22 PM
 */
public class RabbitMQNotify implements Runnable {
    /**
     * LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(RabbitMQNotify.class);

    private transient final Message response;
    private transient final String uriPath;
    private transient final String mac;
    private final CollectorMqtt collector;

    public RabbitMQNotify(final String mac, final String uriPath, final Message response, final CollectorMqtt aCollector) {
        this.response = response;
        this.uriPath = uriPath;
        this.mac = mac;
        this.collector = aCollector;
    }


    @Override
    public void run() {
//        LOGGER.info(uriPath);
        if (uriPath != null) {
            String myuripath = "";
            myuripath = uriPath.replaceAll("\\/", ":");
            if (':' == myuripath.charAt(0)) {
                myuripath = myuripath.substring(1);
            }

            if (myuripath.length() > 3) {

                LOGGER.info(myuripath);
                LOGGER.info(myuripath.length());
            }

            try {
                Double capabilityValue = Double.valueOf(response.getPayloadString());
                sendtoRabbitMQ("0x" + mac, myuripath, capabilityValue);
            } catch (final NumberFormatException e) {
                String res = response.getPayloadString();
                sendtoRabbitMQ("0x" + mac, myuripath, res);
            }
        }
    }


    private void sendtoRabbitMQ(final String nodeId, final String capabilityName, final Double value) {

        final String nodeUrn = collector.getDeviceID() + "/" + nodeId;
        LOGGER.info(nodeUrn);

        final eu.uberdust.communication.protobuf.Message.NodeReadings.Reading reading = eu.uberdust.communication.protobuf.Message.NodeReadings.Reading.newBuilder()
                .setNode(nodeUrn)
                .setCapability(capabilityName)
                .setTimestamp(System.currentTimeMillis())
                .setDoubleReading(value)
                .build();
        LOGGER.info("publishing");
        RabbitMQManager.getInstance().publish("readings", reading.toByteArray());
    }

    private void sendtoRabbitMQ(final String nodeId, final String capabilityName, final String value) {

        final String nodeUrn = collector.getDeviceID() + "/" + nodeId;
        LOGGER.info(nodeUrn);

        final eu.uberdust.communication.protobuf.Message.NodeReadings.Reading reading = eu.uberdust.communication.protobuf.Message.NodeReadings.Reading.newBuilder()
                .setNode(nodeUrn)
                .setCapability(capabilityName)
                .setTimestamp(System.currentTimeMillis())
                .setStringReading(value)
                .build();
        RabbitMQManager.getInstance().publish("readings", reading.toByteArray());
    }
}
