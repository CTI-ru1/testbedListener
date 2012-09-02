package eu.uberdust.testbedlistener.datacollector.parsers;

import eu.uberdust.communication.protobuf.Message;
import eu.uberdust.testbedlistener.util.commiter.WsCommiter;
import org.apache.log4j.Logger;

import java.util.Locale;

/**
 * Parses a message received and adds data to a wisedb database.
 */
public class CommandLineParser extends AbstractMessageParser{ //NOPMD

    /**
     * LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(CommandLineParser.class);

    /**
     * Text line of the message received.
     */
    private final transient String strLine;

    /**
     * @param msg the message received from the testbed
     */
    public CommandLineParser(final String msg) {
        this.strLine = msg;
    }


    /**
     * extracts the nodeid from a received testbed message.
     *
     * @param paramLine the message received from the testbed
     * @return the node id in hex
     */
    private String extractNodeId(final String paramLine) {
        try {
            return strLine.split(" ")[0];
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            return "";
        }
    }

    /**
     * Starts the parser thread.
     */
    public final void run() {
        try {
            parse();
        } catch (Exception e) {

        }
    }


    /**
     * Parses the message and creates the event to report.
     */
    public final void parse() {

        LOGGER.debug(strLine);

        //get the node id
        final String nodeId = extractNodeId(strLine);

        //if there is a node id
        if ("".equals(nodeId)) {
            return;
        }

        LOGGER.debug("Node id is " + nodeId);

        final String[] messageParts = strLine.split(" ");

        LOGGER.info("Length is :" + messageParts.length);

        if (nodeId.contains(",")) {
            final String source = nodeId.split(",")[0];
            final String target = nodeId.split(",")[1];
            commitLinkReading(source, target, messageParts[1], Integer.parseInt(messageParts[2]));
        } else {
            commitNodeReading(nodeId, messageParts[1], Integer.parseInt(messageParts[2]));
        }

    }


    /**
     * Commits a nodeReading to the database using the REST interface.
     *
     * @param nodeId     the id of the node reporting the reading
     * @param capability the name of the capability
     * @param value      the value of the reading
     */
    private void commitNodeReading(final String nodeId, final String capability, final int value) {

        final String nodeUrn = nodeId;
        final String capabilityName = (capability).toLowerCase(Locale.US);

        Message.NodeReadings.Reading reading = Message.NodeReadings.Reading.newBuilder()
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

        final String nodeUrn = nodeId;
        final String capabilityName = (capability).toLowerCase(Locale.US);

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
        final String sourceUrn = source;
        final String targetUrn = target;

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
}
