package eu.uberdust.testbedlistener.datacollector;

import de.uniluebeck.itm.tr.iwsn.overlay.messaging.Messages;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.LengthFieldPrepender;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;

import static org.jboss.netty.channel.Channels.pipeline;

/**
 * Created by IntelliJ IDEA.
 * User: amaxilatis
 * Date: 12/1/11
 * Time: 5:14 PM
 */
public class TRPipelineFactory implements ChannelPipelineFactory {

    /**
     * Chanel handler that receives the messages and Generates parser threads.
     */
    private final transient TRChannelUpstreamHandler upstreamHandler;

    /**
     * @param testbedRuntimeCollector a datacollector object
     */
    public TRPipelineFactory(final TestbedRuntimeCollector testbedRuntimeCollector) {
        upstreamHandler = new TRChannelUpstreamHandler(testbedRuntimeCollector);
    }

    @Override
    public final ChannelPipeline getPipeline() {

        ChannelPipeline p = pipeline();

        p.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4));
        p.addLast("protobufEnvelopeMessageDecoder", new ProtobufDecoder(Messages.Msg.getDefaultInstance()));

        p.addLast("frameEncoder", new LengthFieldPrepender(4));
        p.addLast("protobufEncoder", new ProtobufEncoder());

        p.addLast("handler", upstreamHandler);

        return p;

    }

}
