package eu.uberdust.datacollector.parsers;

import com.rapplogic.xbee.api.XBeeAddress16;
import eu.uberdust.communication.protobuf.Message;
import eu.uberdust.datacollector.DataCollector;
import eu.uberdust.datacollector.TestbedMessageHandler;
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
    private static final String CAPABILITY_PREFIX = "urn:node:capability:";

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
                             final String testbedPrefix, final int testbedId) {

        this.payload = payload;
        remoteAddress = address;
        this.testbedPrefix = testbedPrefix;
        this.testbedId = testbedId;
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p/>
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

        //if there is a node id
        if ("".equals(nodeId)) {
            return;
        }
        LOGGER.debug("Node id is " + nodeId);

        final int capabilityLength = payload[1];
        LOGGER.debug("Capability length: " + capabilityLength);

        final int capabilityValueLength = payload[2];
        LOGGER.debug("Capability value length:" + capabilityValueLength);

        final StringBuilder capabilityNamebuilder = new StringBuilder();
        for (int i = INDEX; i < INDEX + capabilityLength - 1; i++) {
            capabilityNamebuilder.append((char) payload[i]);
        }
        final String capability = capabilityNamebuilder.toString();

        final StringBuilder capabilityValuebuilder = new StringBuilder();
        for (int i = INDEX + capabilityLength; i < INDEX + capabilityLength + capabilityValueLength - 1; i++) {
            capabilityValuebuilder.append((char) payload[i]);
        }
        Double capabilityValue;
        try {
            capabilityValue = Double.valueOf(capabilityValuebuilder.toString());
        } catch (final NumberFormatException e) {
            LOGGER.error(e);
            commitNodeReading(nodeId, capability, capabilityValuebuilder.toString());
            return;
        }
        LOGGER.debug(capability);
        LOGGER.debug(capabilityValue);

        commitNodeReading(nodeId, capability, capabilityValue);
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
     * Commits a nodeReading to the database using the REST interface.
     *
     * @param nodeId     the id of the node reporting the reading
     * @param capability the name of the capability
     * @param value      the value of the reading
     */
    private void commitNodeReading(final String nodeId, final String capability, final Double value) {

        final String nodeUrn = testbedPrefix + nodeId;
        final String capabilityName = (CAPABILITY_PREFIX + capability).toLowerCase(Locale.US);

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

        final String nodeUrn = TestbedMessageHandler.getInstance().getTestbedPrefix() + nodeId;
        final String capabilityName = (CAPABILITY_PREFIX + capability).toLowerCase(Locale.US);

        Message.NodeReadings.Reading reading = Message.NodeReadings.Reading.newBuilder()
                .setNode(nodeUrn)
                .setCapability(capabilityName)
                .setTimestamp(System.currentTimeMillis())
                .setStringReading(value)
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
