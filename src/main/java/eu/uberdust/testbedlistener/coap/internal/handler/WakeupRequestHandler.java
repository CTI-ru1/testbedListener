package eu.uberdust.testbedlistener.coap.internal.handler;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Message;
import eu.uberdust.testbedlistener.datacollector.parsers.CoapMessageParser;

/**
 * Created with IntelliJ IDEA.
 * User: amaxilatis
 * Date: 6/13/13
 * Time: 3:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class WakeupRequestHandler implements InternalRequestHandler {
    @Override
    public void handle(Message udpRequest, Message response) {
        StringBuilder payload = new StringBuilder("");
        if (udpRequest.getCode() == CodeRegistry.METHOD_POST) {
            String device = udpRequest.getPayloadString();
            if (device.length() == 3) {
                device = "0" + device;
            }
            byte[] data = new byte[11];
            int pos = 0;
            data[pos++] = 0x69;
            data[pos++] = 102; // w/e
            for (int i = 0; i < 4; i += 2) {
                data[pos++] = (byte) ((Character.digit(device.charAt(i), 16) << 4) + Character.digit(device.charAt(i + 1), 16));
            }
            data[pos++] = 0x68;
            data[pos++] = 0x65;
            data[pos++] = 0x72;
            data[pos++] = 0x65;
            data[pos++] = 0x69;
            data[pos++] = 0x61;
            data[pos] = 0x6D;

//            final Thread parser = new Thread(new CoapMessageParser("0x" + udpRequest.getPayloadString(), data, null, null, this));
//            parser.start();

            payload.append("Here I am simulation on ").append(udpRequest.getPayloadString());
            response.setContentType(MediaTypeRegistry.TEXT_PLAIN);
            response.setPayload(payload.toString());
            response.setCode(CodeRegistry.RESP_VALID);
        } else {
            response.setCode(CodeRegistry.RESP_METHOD_NOT_ALLOWED);
        }

    }
}
