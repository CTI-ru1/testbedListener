package eu.uberdust.testbedlistener.coap.viewer;

import ch.ethz.inf.vs.californium.coap.Message;
import org.apache.log4j.Logger;
import org.codehaus.plexus.util.StringOutputStream;

import java.io.PrintStream;

/**
 * Created by IntelliJ IDEA.
 * User: amaxilatis
 * Date: 11/29/12
 * Time: 12:03 PM
 */
public class IncomingMessage {
    /**
     * LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(IncomingMessage.class);

    public IncomingMessage(byte[] payload) {
        try {
            Message mess = Message.fromByteArray(payload);
            PrintStream ps = new PrintStream(new StringOutputStream());
            mess.prettyPrint(ps);
            LOGGER.info(ps.toString());
        } catch (Exception e) {
        }

    }
}
