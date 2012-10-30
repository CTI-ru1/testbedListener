package eu.uberdust.testbedlistener.datacollector;

import com.google.protobuf.ByteString;
import de.uniluebeck.itm.tr.iwsn.overlay.messaging.Messages;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNApp;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNAppMessages;
import eu.uberdust.testbedlistener.controller.TestbedController;
import eu.uberdust.testbedlistener.util.PropertyReader;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;


/**
 * Opens a connection to a TestbedRuntime server and received debug messages from all nodes to collect data.
 */
public class TestbedRuntimeCollector extends AbstractCollector implements Runnable {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(TestbedRuntimeCollector.class);

    /**
     * testbed hostname.
     */
    private transient String host;

    /**
     * testbed port to connect to.
     */
    private transient int port;

    /**
     * pipeline factory.
     */
    private transient NioClientSocketChannelFactory factory;
    private transient ClientBootstrap bootstrap;

    /**
     * Default Constructor.
     */
    public TestbedRuntimeCollector() {
        PropertyConfigurator.configure(Thread.currentThread().getContextClassLoader().getResource("log4j.properties"));
        readProperties();

    }

    /**
     * Reads the property file.
     */
    private void readProperties() {

        host = PropertyReader.getInstance().getProperties().getProperty("testbed.hostname");
        port = Integer.parseInt(PropertyReader.getInstance().getProperties().getProperty("testbed.overlay"));

    }


    /**
     * Channel factory with custom channelPipeline to parse the received messages.
     */
    private final transient TRPipelineFactory chPipelineFactory = new TRPipelineFactory(this);


    /**
     * Connects to testbedruntime overlay port to receive all incoming debug messages.
     *
     * @return true when connection was success
     */
    public final boolean start() {

        ChannelFuture connectFuture = null;
        // Make a new connection.
        connectFuture = bootstrap.connect(new InetSocketAddress(host, port));
        final Channel channel = connectFuture.getChannel();
        LOGGER.info(channel.getId());

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            LOGGER.error(e);
        }
        LOGGER.info("connecting");
        // Wait until the connection is made successfully.
        if (!connectFuture.isSuccess()) {
            LOGGER.warn("Client Connect Failed!", connectFuture.getCause());
            return false;
        }
        return true;
    }

    /**
     * Reconnects to testbedruntime when connection was lost.
     */
    public final void restart() {
        LOGGER.info("Waiting for Testbed Restart...");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            LOGGER.error(e);
        }
        LOGGER.info("Reconnecting...");
        if (!start()) {
            restart();
        }
    }

    @Override
    public void run() {
        factory = new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
        bootstrap = new ClientBootstrap(factory);
        bootstrap.setPipelineFactory(chPipelineFactory);

        if (!start()) {
            restart();
        }
//        else {
//            TestbedController.getInstance().connectToRuntime();
//        }

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


        TestbedController.getInstance().setTestbed(this);


    }

    public void sendMessage(byte[] newPayload, String destination) {
        destination = PropertyReader.getInstance().getTestbedPrefix()+"0x" + destination;
        LOGGER.info("Sending to " + destination);
        byte[] framedPayload = new byte[newPayload.length + 1];
        System.arraycopy(newPayload, 0, framedPayload, 1, newPayload.length);
        framedPayload[0] = 0xa;
        ByteString bs = ByteString.copyFrom(framedPayload);
        WSNAppMessages.Message.Builder message = WSNAppMessages.Message.newBuilder()
                .setBinaryData(bs)
                .setSourceNodeId("gateway")
                .setTimestamp("Nobody cares for this demo purpose");

        WSNAppMessages.OperationInvocation.Builder oiBuilder = WSNAppMessages.OperationInvocation.newBuilder()
                .setOperation(WSNAppMessages.OperationInvocation.Operation.SEND)
                .setArguments(message.build().toByteString());


        Messages.Msg msg = Messages.Msg.newBuilder()
                .setMsgType(WSNApp.MSG_TYPE_OPERATION_INVOCATION_REQUEST)
                .setFrom("gateway")
                .setTo(destination)
                .setPayload(oiBuilder.build().toByteString())
                .setPriority(1)
                .build();

        // Make a new connection.
        ChannelFuture connectFuture = bootstrap.connect(new InetSocketAddress(host, port));

        // Wait until the connection is made successfully.
        Channel channel = connectFuture.awaitUninterruptibly().getChannel();
        if (!connectFuture.isSuccess()) {
            LOGGER.error("Client connect failed", connectFuture.getCause());
        }

        LOGGER.info("WILL WRITE MESSAGE");
        channel.write(msg);
        LOGGER.info("WROTE MESSAGE");
    }


    public static void main(String[] args) {
        PropertyReader.getInstance().setFile("listener.properties");
        Thread thread = new Thread(new TestbedRuntimeCollector());
        thread.start();


    }
}
