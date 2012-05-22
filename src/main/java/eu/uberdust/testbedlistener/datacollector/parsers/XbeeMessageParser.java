package eu.uberdust.testbedlistener.datacollector.parsers;

import com.rapplogic.xbee.api.XBeeAddress16;
import eu.uberdust.communication.protobuf.Message;
import eu.uberdust.testbedlistener.datacollector.DataCollector;
import eu.uberdust.testbedlistener.datacollector.commiter.WsCommiter;
import org.apache.log4j.Logger;

import java.util.Locale;

/**
 * Parses an XBee message received and adds data to a wisedb database.
 */
public class XbeeMessageParser implements Runnable {

    /**
     * LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(DataCollector.class);

    /**
     * Testbed Capability prefix.
     */
    private final String capabilityPrefix;

    /**
     * Index of capability.
     */
    private final static int INDEX = 7;

    /**
     * The testbed prefix.
     */
    private final String testbedPrefix;

    /**
     * The testbed id.
     */
    private final int testbedId;

    /**
     * The Mac Address of the remote node.
     */
    private final XBeeAddress16 remoteAddress;

    /**
     * The payload of the received message.
     */
    private final int[] payload;

    /**
     * Default Constructor.
     *
     * @param address       the address of the node
     * @param payload       the payload message to be parsed.
     * @param testbedPrefix the testbed prefix
     * @param testbedId     the testbed id
     */
    public XbeeMessageParser(final XBeeAddress16 address, final int[] payload,
                             final String testbedPrefix, final int testbedId, final String capabilityPrefix) {

        this.payload = payload;
        remoteAddress = address;
        this.testbedPrefix = testbedPrefix;
        this.capabilityPrefix = capabilityPrefix;
        this.testbedId = testbedId;
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
        parse();
    }

    /**
     * Parses the message and creates the event to report.
     */

    private void parse() {

        if (payload[0] != 103) {
            return;
        }
        //get the node id
        final String nodeId = extractNodeId();
        final String targetId = extractTargetId();

        //if there is a node id
        if ("".equals(nodeId)) {
            return;
        }


        final String capability = extractCapabilityName();
        final String capabilityStringValue = extractCapabilityValue();

        if (!targetId.equals("0xffff")) {
            try {
                commitLinkReading(nodeId, targetId, capability, Integer.parseInt(capabilityStringValue));
            } catch (final NumberFormatException e) {
                LOGGER.error(e);
                return;
            }
            return;
        }

        LOGGER.info("Node:" + nodeId + "," + targetId + " Capability:" + capability);

        Double capabilityValue;
        try {
            capabilityValue = Double.valueOf(capabilityStringValue);
        } catch (final NumberFormatException e) {
            LOGGER.error(e);
            commitNodeReading(nodeId, capability, capabilityStringValue);
            return;
        }
        LOGGER.debug(capability);
        LOGGER.debug(capabilityValue);

        commitNodeReading(nodeId, capability, capabilityValue);
    }

    private String extractCapabilityValue() {
        final int capabilityLength = payload[1];
        final int capabilityValueLength = payload[2];
        LOGGER.debug("Capability value length:" + capabilityValueLength);
        final StringBuilder capabilityValuebuilder = new StringBuilder();
        for (int i = INDEX + capabilityLength; i < INDEX + capabilityLength + capabilityValueLength - 1; i++) {
            capabilityValuebuilder.append((char) payload[i]);
        }
        return capabilityValuebuilder.toString();
    }

    private String extractCapabilityName() {
        final int capabilityLength = payload[1];
        LOGGER.debug("Capability length: " + capabilityLength);


        final StringBuilder capabilityNamebuilder = new StringBuilder();
        for (int i = INDEX; i < INDEX + capabilityLength - 1; i++) {
            capabilityNamebuilder.append((char) payload[i]);
        }
        return capabilityNamebuilder.toString();
    }


    /**
     * Converts the address to String.
     *
     * @return the node id in hex
     */
    private String extractNodeId() {
        return "0x"
                + Integer.toHexString(payload[3])
                + Integer.toHexString(payload[4]);

    }

    /**
     * Converts the address to String.
     *
     * @return the node id in hex
     */
    private String extractTargetId() {
        return "0x"
                + Integer.toHexString(payload[5])
                + Integer.toHexString(payload[6]);

    }

    /**
     * Commits a nodeReading to the database using the REST interface.
     *
     * @param nodeId     the id of the node reporting the reading
     * @param capability the name of the capability
     * @param value      the value of the reading
     */
    private void commitNodeReading(final String nodeId, final String capability, final Double value) {

        final String nodeUrn = testbedPrefix + nodeId;
        final String capabilityName = (capabilityPrefix + capability).toLowerCase(Locale.US);

        final Message.NodeReadings.Reading reading = Message.NodeReadings.Reading.newBuilder()
                .setNode(nodeUrn)
                .setCapability(capabilityName)
                .setTimestamp(System.currentTimeMillis())
                .setDoubleReading(value)
                .build();

        new WsCommiter(reading);
    }

    /**
     * Commits a nodeReading to the database using the REST interface.
     *
     * @param nodeId     the id of the node reporting the reading
     * @param capability the name of the capability
     * @param value      the value of the reading
     */
    private void commitNodeReading(final String nodeId, final String capability, final String value) {

        final String nodeUrn = testbedPrefix + nodeId;
        final String capabilityName = (capabilityPrefix + capability).toLowerCase(Locale.US);

        Message.NodeReadings.Reading reading = Message.NodeReadings.Reading.newBuilder()
                .setNode(nodeUrn)
                .setCapability(capabilityName)
                .setTimestamp(System.currentTimeMillis())
                .setStringReading(value)
                .build();

        new WsCommiter(reading);
    }

    /**
     * commits a nodeReading to the database using the Hibernate.
     *
     * @param source     the id of the source node of the link
     * @param target     the id of the target node of the link
     * @param testbedCap the capability describing the link reading
     * @param value      the status value of the link
     */
    private void commitLinkReading(final String source, final String target, final String testbedCap, final int value) {
        final String sourceUrn = testbedPrefix + source;
        final String targetUrn = testbedPrefix + target;

        LOGGER.debug("LinkReading" + sourceUrn + "<->" + targetUrn + " " + testbedCap + " " + value);

        Message.LinkReadings.Reading reading = Message.LinkReadings.Reading.newBuilder()
                .setSource(sourceUrn)
                .setTarget(targetUrn)
                .setCapability(testbedCap)
                .setTimestamp(System.currentTimeMillis())
                .setDoubleReading(value)
                .build();

        new WsCommiter(reading);
    }

    public static void main(String[] args) {
        XBeeAddress16 xBeeAddress16 = new XBeeAddress16(new int[]{0x99, 0x79});
        System.out.println("0x"
                + Integer.toHexString(xBeeAddress16.getAddress()[0])
                + Integer.toHexString(xBeeAddress16.getAddress()[1]));
    }
}
