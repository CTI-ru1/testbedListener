package eu.uberdust.testbedlistener.datacollector.notify;

import ch.ethz.inf.vs.californium.coap.Message;
import eu.uberdust.testbedlistener.util.commiter.WsCommiter;
import org.apache.log4j.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: amaxilatis
 * Date: 11/29/12
 * Time: 3:22 PM
 */
public class UberdustNotify implements Runnable {
    /**
     * LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(UberdustNotify.class);

    private transient final Message response;
    private transient final String uriPath;
    private transient final String mac;
    private transient final String testbedPrefix;
    private transient final String capabilityPrefix;

    public UberdustNotify(final String mac, final String uriPath, final Message response, final String testbedPrefix, final String capabilityPrefix) {
        this.response = response;
        this.uriPath = uriPath;
        this.mac = mac;
        this.testbedPrefix = testbedPrefix;
        this.capabilityPrefix = capabilityPrefix;
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
                commitNodeReading("0x" + mac, myuripath, capabilityValue);
            } catch (final NumberFormatException e) {
                String res = response.getPayloadString();
                commitNodeReading("0x" + mac, myuripath, res);
            }
        }
    }

    private void commitNodeReading(final String nodeId, String capability, final Double value) {
        if (capability.equals("temp")) {
            capability = "temperature";
        }
        final String nodeUrn = testbedPrefix + nodeId;
        final String capabilityName = (capabilityPrefix + capability).toLowerCase();

        final eu.uberdust.communication.protobuf.Message.NodeReadings.Reading reading = eu.uberdust.communication.protobuf.Message.NodeReadings.Reading.newBuilder()
                .setNode(nodeUrn)
                .setCapability(capabilityName)
                .setTimestamp(System.currentTimeMillis())
                .setDoubleReading(value)
                .build();

        new WsCommiter(reading);
    }

    private void commitNodeReading(final String nodeId, String capability, final String value) {

        if (capability.equals("temp")) {
            capability = "temperature";
        }
        final String nodeUrn = testbedPrefix + nodeId;
        final String capabilityName = (capabilityPrefix + capability).toLowerCase();
        LOGGER.info(value);
        final eu.uberdust.communication.protobuf.Message.NodeReadings.Reading reading = eu.uberdust.communication.protobuf.Message.NodeReadings.Reading.newBuilder()
                .setNode(nodeUrn)
                .setCapability(capabilityName)
                .setTimestamp(System.currentTimeMillis())
                .setStringReading(value)
                .build();

        new WsCommiter(reading);
    }
}
