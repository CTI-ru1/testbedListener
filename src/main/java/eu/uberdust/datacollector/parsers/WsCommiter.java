package eu.uberdust.datacollector.parsers;

import eu.uberdust.communication.protobuf.Message;
import eu.uberdust.network.NetworkManager;
import org.apache.log4j.Logger;


/**
 * Created by IntelliJ IDEA.
 * User: amaxilatis
 * Date: 12/4/11
 * Time: 2:22 PM
 */
public class WsCommiter {
    /**
     * LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(WsCommiter.class);

    /**
     * Constructor using a NodeReading.
     *
     * @param nodeReading the NodeReading to commit
     */
    public WsCommiter(final Message.NodeReadings.Reading nodeReading) {
        try {

            final Message.NodeReadings readings = Message.NodeReadings.newBuilder().addReading(nodeReading).build();

            NetworkManager.getInstance().sendNodeReading(readings);
            LOGGER.info(readings);
        } catch (Exception e) {
            LOGGER.error("InsertReadingWebSocketClient -node-" + e);
        }
    }

    /**
     * Constructor using a LinkReading.
     *
     * @param linkReading the LinkReading to commit
     */
    public WsCommiter(final Message.LinkReadings.Reading linkReading) {
        try {
            Message.LinkReadings readings = Message.LinkReadings.newBuilder().addReading(linkReading).build();

            NetworkManager.getInstance().sendLinkReading(readings);
            LOGGER.debug(readings);
        } catch (Exception e) {
            LOGGER.error("InsertReadingWebSocketClient -link- " + e);
        }
    }
}
