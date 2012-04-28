package eu.uberdust.datacollector.parsers;

import eu.uberdust.communication.protobuf.Message;
import eu.uberdust.datacollector.DataCollector;
import org.apache.log4j.Logger;

import java.util.Locale;
import java.util.Map;

/**
 * Parses a message received and adds data to a wisedb database.
 */
public class MessageParser implements Runnable { //NOPMD

    /**
     * LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(DataCollector.class);

    /**
     * Text line of the message received.
     */
    private final transient String strLine;

    /**
     * Map of all the codeNames-capabilities.
     */
    private final transient Map<String, String> sensors;

    /**
     * Testbed Capability prefix.
     */
    private static final String CAPABILITY_PREFIX = "urn:wisebed:node:capability:";

    /**
     * Position of the timestamp in a node Reading .
     */
    private static final int TIMESTAMP_POS = 4;
    private String testbedPrefix;
    private int testbedId;

    private String[] capabilities = {"temperature", "light", "ir", "humidity", "co", "co2", "ch4", "pir"
            , "batterycharge", "accelerometer", "link_up", "barometricpressure", "link_down", "pressure", "light", "", "temperature"};


    /**
     * @param msg    the message received from the testbed
     * @param senses the Map containing the sensor codenames on testbed , capability names
     */
    public MessageParser(final String msg, final Map<String, String> senses, String testbedPrefix, int testbedId) {
        this.testbedPrefix = testbedPrefix;
        this.testbedId = testbedId;
        strLine = msg.substring(msg.indexOf("binaryData:") + "binaryData:".length());
        sensors = senses;
    }


    /**
     * extracts the nodeid from a received testbed message.
     *
     * @param paramLine the message received from the testbed
     * @return the node id in hex
     */
    private String extractNodeId(final String paramLine) {
        final String line = paramLine.substring(7);
        final int start = line.indexOf("0x");
        if (start > 0) {
            final int end = line.indexOf(' ', start);
            if (end > 0) {
                return line.substring(start, end);
            }
        }
        return "";
    }

    /**
     * Starts the parser thread.
     */
    public final void run() {
        parse();
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

        if (checkSelfDescription(nodeId)) {
            return;
        }


        //check for Link Readings
        if (checkLinkReading(nodeId)) {
            return;
        }

        //check for all given capabilities
        for (String sensor : sensors.keySet()) {
            if (checkSensor(sensor, nodeId)) {
                return;
            }
        }

    }

    private boolean checkSelfDescription(final String nodeId) {
        if (strLine.contains("SELF")) {
            LOGGER.info(strLine);

            final int start = strLine.indexOf("SELF") + "SELF".length() + 1;
            try {
                final String[] sensors = strLine.substring(start).split(",");
                LOGGER.info("Num of sensors:" + sensors.length);
                int relayCount = 1;
                long millis = System.currentTimeMillis();
                for (int i = 0; i < sensors.length - 1; i++) {
                    final int capIndex = Integer.parseInt(sensors[i]);
                    if (capIndex == 14) {
                        final String capName = CAPABILITY_PREFIX + capabilities[capIndex] + relayCount;
                        LOGGER.info("NOde:" + nodeId + " " + capName);
                        commitNodeReading(nodeId, "report", capName, millis);
                        relayCount++;
                    } else {
                        final String capName = CAPABILITY_PREFIX + capabilities[capIndex];
                        LOGGER.info("NOde:" + nodeId + " " + CAPABILITY_PREFIX + capabilities[capIndex]);
                        commitNodeReading(nodeId, "report", capName, millis);
                    }
                }
            } catch (Exception e) {
                LOGGER.info(e);
                e.printStackTrace();
            }
            return true;
        } else {
            return false;
        }

    }

    /**
     * checks for the a node reading.
     *
     * @param sensor the sensor to check for
     * @param nodeId the id of the reporting node
     * @return true if contains a NodeReading
     */
    private boolean checkSensor(final String sensor, final String nodeId) {
        boolean retVal = false;
        if (strLine.contains(sensor)) {
            retVal = true;
            final int start = strLine.indexOf(sensor) + sensor.length() + 1;
            int end = strLine.indexOf(' ', start);
            if (end == -1) {
                end = strLine.length() - 2;
            }
            try {
                final int value = Integer.parseInt(strLine.substring(start, end));
                LOGGER.debug(sensors.get(sensor) + " value " + value + " node " + nodeId);
//                final String milliseconds = String.valueOf(System.currentTimeMillis());

//                if ((nodeId.contains("1ccd")) && (sensor.contains("EM_E"))) {
//                    milliseconds = strLine.split(" ")[TIMESTAMP_POS];
//                    LOGGER.info("setting eventt to " + milliseconds);
//
//                }
                commitNodeReading(nodeId, sensors.get(sensor), value, System.currentTimeMillis());
            } catch (Exception e) {
                LOGGER.error("Parse Error" + sensor + "'" + strLine.substring(start, end) + "'");
            }
        }
        return retVal;
    }

    /**
     * Checks the message received for a new node reading.
     *
     * @param nodeId nodeId the id of the node reporting the reading
     * @return true of a reading was found
     */
    private boolean checkLinkReading(final String nodeId) {
        if (strLine.contains("LINK_DOWN")) {
            //get the target id
            final int start = strLine.indexOf("LINK_DOWN") + "LINK_DOWN".length() + 1;
            final int end = strLine.indexOf(' ', start);
            commitLinkReading(nodeId, strLine.substring(start, end), "status", 0);
        } else if (strLine.contains("LINK_UP")) {
            //get the target id
            final int start = strLine.indexOf("LINK_UP") + "LINK_UP".length() + 1;
            final int end = strLine.indexOf(' ', start);
            commitLinkReading(nodeId, strLine.substring(start, end), "status", 1);
        } else if (strLine.contains("command=")) {
            LOGGER.info(strLine);
            final int start = strLine.indexOf("dest::") + "dest::".length();
            final int end = strLine.indexOf(' ', start);
            LOGGER.info("nodid:" + strLine.substring(start, end));

            final int commandStart = strLine.indexOf("command=");
            final int commandStop = strLine.indexOf(' ', commandStart);
            final iSenseArduinoCmd command = new iSenseArduinoCmd(strLine.substring(commandStart, commandStop));
            LOGGER.info("commandString:" + command.toString());
            commitLinkReading(nodeId, strLine.substring(start, end), "command", command.toInt());
            LOGGER.info("COMMAND " + nodeId + " " + strLine.substring(start, end) + " " + command.toString());
        }
        return false;
    }

    /**
     * Commits a nodeReading to the database using the REST interface.
     *
     * @param nodeId     the id of the node reporting the reading
     * @param capability the name of the capability
     * @param value      the value of the reading
     * @param msec       timestamp in milliseconds
     */
    private void commitNodeReading(final String nodeId, final String capability, final int value, final long msec) {

        final String nodeUrn = testbedPrefix + nodeId;
        final String capabilityName = (CAPABILITY_PREFIX + capability).toLowerCase(Locale.US);

        Message.NodeReadings.Reading reading = Message.NodeReadings.Reading.newBuilder()
                .setNode(nodeUrn)
                .setCapability(capabilityName)
                .setTimestamp(msec)
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
     * @param msec       timestamp in milliseconds
     */
    private void commitNodeReading(final String nodeId, final String capability, final String value, final long msec) {

        final String nodeUrn = testbedPrefix + nodeId;
        final String capabilityName = (CAPABILITY_PREFIX + capability).toLowerCase(Locale.US);

        Message.NodeReadings.Reading reading = Message.NodeReadings.Reading.newBuilder()
                .setNode(nodeUrn)
                .setCapability(capabilityName)
                .setTimestamp(msec)
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
}
