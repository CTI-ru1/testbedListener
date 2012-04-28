package eu.uberdust.datacollector.parsers;

import com.rapplogic.xbee.api.XBeeAddress16;
import eu.uberdust.datacollector.DataCollector;
import org.apache.log4j.Logger;

import java.util.HashMap;

/**
 * Parses an XBee message received and adds data to a wisedb database.
 */
public class XbeeMessageParser implements Runnable {

    /**
     * LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(DataCollector.class);

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
     * The HashMap with the capabilities.
     */
    private final HashMap<String, String> capabilities;

    /**
     * Default Constructor.
     *
     * @param address       the address of the node
     * @param payload       the payload message to be parsed.
     * @param testbedPrefix the testbed prefix
     * @param testbedId     the testbed id
     * @param capabilities  the capabilities
     */
    public XbeeMessageParser(final XBeeAddress16 address, final int[] payload,
                             final String testbedPrefix, final int testbedId,
                             final HashMap<String, String> capabilities) {

        this.payload = payload;
        remoteAddress = address;
        this.testbedPrefix = testbedPrefix;
        this.testbedId = testbedId;
        this.capabilities = capabilities;
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

        //get the node id
        final String nodeId = extractNodeId(remoteAddress);

        //if there is a node id
        if ("".equals(nodeId)) {
            return;
        }
        LOGGER.debug("Node id is " + nodeId);


    }

    /**
     * Converts the address to String.
     *
     * @param xBeeAddress16 the 16 bit address
     * @return the node id in hex
     */
    private String extractNodeId(final XBeeAddress16 xBeeAddress16) {
        return "0x"
                + Integer.toHexString(xBeeAddress16.getAddress()[0])
                + Integer.toHexString(xBeeAddress16.getAddress()[1]);

    }

    public static void main(String[] args) {
        XBeeAddress16 xBeeAddress16 = new XBeeAddress16(new int[]{0x99, 0x79});
        System.out.println("0x"
                + Integer.toHexString(xBeeAddress16.getAddress()[0])
                + Integer.toHexString(xBeeAddress16.getAddress()[1]));
    }
}
