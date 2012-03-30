package eu.uberdust.datacollector;

import com.google.protobuf.InvalidProtocolBufferException;
import de.uniluebeck.itm.gtr.messaging.Messages;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNApp;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNAppMessages;
import eu.uberdust.datacollector.parsers.MessageParser;
import eu.uberdust.testbedlistener.util.PropertyReader;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

import java.net.ConnectException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by IntelliJ IDEA.
 * User: amaxilatis
 * Date: 12/1/11
 * Time: 5:04 PM
 */
public class DataCollectorChannelUpstreamHandler extends SimpleChannelUpstreamHandler {
    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(DataCollectorChannelUpstreamHandler.class);

    /**
     * counts the messages received - stats.
     */
    private transient int messageCounter;

    /**
     * Stats counter.
     */
    private static final int REPORT_LIMIT = 1000;

    /**
     * saves the last time 1000 messages were received - stats.
     */
    private transient long lastTime;

    /**
     * executors for handling incoming messages.
     */
    private final transient ExecutorService executorService;

    /**
     * map that contains the sensors monitored.
     */
    private transient Map<String, String> sensors = new HashMap<String, String>();

    /**
     * reference to the class that created the handler.
     */
    private final transient DataCollector dataCollector;
    private String testbedPrefix;
    private int testbedId;

    /**
     * @param dataCollector a datacollector object
     */
    public DataCollectorChannelUpstreamHandler(final DataCollector dataCollector) {
        this.dataCollector = dataCollector;
        messageCounter = 0;
        lastTime = System.currentTimeMillis();


        final String sensorNamesString = PropertyReader.getInstance().getProperties().getProperty("sensors.names");
        final String sensorPrefixesString = PropertyReader.getInstance().getProperties().getProperty("sensors.prefixes");

        final String[] sensorsNamesList = sensorNamesString.split(",");
        final String[] sensorsPrefixesList = sensorPrefixesString.split(",");

        final StringBuilder sensBuilder = new StringBuilder("Sensors Checked: \n");
        for (int i = 0; i < sensorsNamesList.length; i++) {
            sensBuilder.append(sensorsNamesList[i]).append("[").append(sensorsPrefixesList[i]).append("]").append("\n");
            sensors.put(sensorsPrefixesList[i], sensorsNamesList[i]);
        }
        LOGGER.info(sensBuilder);

        testbedPrefix = PropertyReader.getInstance().getProperties().getProperty("testbed.prefix");
        LOGGER.info(testbedPrefix);
        testbedId = Integer.parseInt(PropertyReader.getInstance().getProperties().getProperty("wisedb.testbedid"));
        LOGGER.info(testbedId);

        executorService = Executors.newCachedThreadPool();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        if (e instanceof ConnectException) {
            LOGGER.warn("ConnectException");
        }
    }

    @Override
    public final void messageReceived(final ChannelHandlerContext ctx, final MessageEvent messageEvent)
            throws InvalidProtocolBufferException {
        LOGGER.debug("message received");
        final Messages.Msg message = (Messages.Msg) messageEvent.getMessage();
        if (WSNApp.MSG_TYPE_LISTENER_MESSAGE.equals(message.getMsgType())) {
            final WSNAppMessages.Message wsnAppMessage = WSNAppMessages.Message.parseFrom(message.getPayload());
            parse(wsnAppMessage.toString());
//            messageCounter++;
//            if (messageCounter == REPORT_LIMIT) {
//                final long milliseconds = System.currentTimeMillis() - lastTime;
//                final double stat = messageCounter / (milliseconds / (double) REPORT_LIMIT);
//                LOGGER.info("MessageRate : " + stat + " messages/sec");
//                final ThreadPoolExecutor pool = (ThreadPoolExecutor) executorService;
//                LOGGER.info("PoolSize : " + pool.getPoolSize() + " Active :" + pool.getActiveCount());
//                LOGGER.info("Peak : " + pool.getLargestPoolSize());
//                final NodeReading nodeReading = new NodeReading();
//                nodeReading.setTestbedId("3");
//                nodeReading.setNodeId("urn:ctinetwork:uberdust");
//                nodeReading.setCapabilityName("urn:ctinetwork:testbedlistener:poolSize");
//                nodeReading.setReading(String.valueOf(pool.getPoolSize()));
//                nodeReading.setTimestamp(String.valueOf((new Date()).getTime()));
//                new WsCommiter(nodeReading);
//                nodeReading.setCapabilityName("urn:ctinetwork:testbedlistener:activeThreads");
//                nodeReading.setReading(String.valueOf(pool.getActiveCount()));
//                new WsCommiter(nodeReading);
//                nodeReading.setCapabilityName("urn:ctinetwork:testbedlistener:messageRate");
//                nodeReading.setReading(String.valueOf(stat));
//                new WsCommiter(nodeReading);
//
//                lastTime = System.currentTimeMillis();
//                messageCounter = 0;
//            }

        } else {
            LOGGER.error("got a message of type " + message.getMsgType());
        }
    }

    /**
     * called upon disconnect from the server.
     *
     * @param ctx               the channel context
     * @param channelStateEvent the channel disconnect event
     * @throws Exception an exception
     */
    @Override
    public final void channelDisconnected(final ChannelHandlerContext ctx, final ChannelStateEvent channelStateEvent)
            throws Exception {     //NOPMD
        super.channelDisconnected(ctx, channelStateEvent);
        shutdown();

    }

    /**
     * Shuts down the executorService.
     */
    private void shutdown() {
        LOGGER.error("Shutting down!!!");
        dataCollector.restart();
    }

    /**
     * Submits a new thread to the executor to parse the new string message.
     *
     * @param toString the string to parse
     */
    private void parse(final String toString) {
        executorService.submit(new MessageParser(toString, sensors, testbedPrefix, testbedId));
    }

}
