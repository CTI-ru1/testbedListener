package eu.uberdust.testbedlistener;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import eu.uberdust.testbedlistener.coap.CoapServer;

import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: amaxilatis
 * Date: 11/2/12
 * Time: 5:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class CoapHelper {

    public static Request getWellKnown(String macStr, int i) {
        Request request = getWellKnown(macStr);
        Option blockwiseOption = new Option(OptionNumberRegistry.BLOCK2);
        byte[] block = new byte[1];
        block[0] = (byte) ((byte) i << 4);
        block[0] = (byte) (block[0] | 0x2);
        blockwiseOption.setValue(block);
        request.setOption(blockwiseOption);
        return request;
    }

    public static Request getWellKnown(String macStr) {
        synchronized (CoapHelper.class) {
            Request request = new Request(CodeRegistry.METHOD_GET, false);
            request.setURI("/.well-known/core");
            Option urihostOption = new Option(OptionNumberRegistry.URI_HOST);
            urihostOption.setStringValue(macStr);
            request.addOption(urihostOption);
            request.setMID(CoapServer.getInstance().nextMID());
//            int mid = new Random().nextInt(5000);
//            if ( mid < 0 )
//                mid = -mid;
//            request.setMID(mid);
            request.prettyPrint();
            return request;
        }
    }
}
