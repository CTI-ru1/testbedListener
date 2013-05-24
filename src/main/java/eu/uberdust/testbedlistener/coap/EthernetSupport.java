package eu.uberdust.testbedlistener.coap;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import eu.uberdust.testbedlistener.coap.udp.EthernetUDPhandler;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: amaxilatis
 * Date: 10/9/12
 * Time: 4:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class EthernetSupport extends Thread {
    /**
     * LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(EthernetSupport.class);
    private final EthernetUDPhandler udphandler;
    private Random rand;
    private final String device;

    public EthernetSupport(final EthernetUDPhandler udphandler, final String device) {
        this.rand = new Random();
        this.udphandler = udphandler;
        this.device = device;
    }

    @Override
    public void run() {
        LOGGER.info("polling");

        Request request = new Request(CodeRegistry.METHOD_GET, false);
        request.setURI("/.well-known/core");
        int newMID = rand.nextInt() % 35000;
        if (newMID < 0) newMID = -newMID;
        request.setMID(newMID);
        CoapServer.getInstance().addEthernet(device + request.getUriPath(), request.getMID());
        try {
            udphandler.send(request, device);
        } catch (UnknownHostException e) {
            LOGGER.error(e, e);
        } catch (IOException e) {
            LOGGER.error(e, e);
        }
    }

    public static void main(String[] args) {

    }
}
