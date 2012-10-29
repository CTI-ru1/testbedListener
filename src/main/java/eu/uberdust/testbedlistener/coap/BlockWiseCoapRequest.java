package eu.uberdust.testbedlistener.coap;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import com.rapplogic.xbee.api.XBeeAddress16;
import eu.mksense.XBeeRadio;
import eu.uberdust.testbedlistener.util.Converter;
import org.apache.log4j.Logger;

import java.util.TimerTask;

/**
 * Created by IntelliJ IDEA.
 * User: amaxilatis
 * Date: 7/11/12
 * Time: 11:52 AM
 * To change this template use File | Settings | File Templates.
 */
public class BlockWiseCoapRequest extends TimerTask {
    /**
     * LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(BlockWiseCoapRequest.class);
    private XBeeAddress16 remoteAddress;
    private byte[] blockIdx;

    public BlockWiseCoapRequest(XBeeAddress16 address16, byte[] blockIdx) {
        remoteAddress = address16;
        this.blockIdx= blockIdx;
    }

    @Override
    public void run() {
        LOGGER.debug("Broadcast Message from server");
        Request request = new Request(CodeRegistry.METHOD_GET, false);
        request.setURI("/.well-known/core");
        Option blockOpt = new Option(OptionNumberRegistry.BLOCK2);
        blockOpt.setIntValue(18);
        request.addOption(blockOpt);
//            LOGGER.info(Converter.getInstance().payloadToString(request.toByteArray()));
        try {
            int[] zpayloap = new int[1 + request.toByteArray().length];
            zpayloap[0] = 51;
            System.arraycopy(Converter.getInstance().ByteToInt(request.toByteArray()), 0, zpayloap, 1, Converter.getInstance().ByteToInt(request.toByteArray()).length);
            LOGGER.info(Converter.getInstance().payloadToString(zpayloap));
            XBeeRadio.getInstance().send(remoteAddress, 112, zpayloap);
        } catch (Exception e) {     //NOPMD
            LOGGER.error(e.getMessage(), e);
        }
//            }
    }

}
