package eu.uberdust.testbedlistener.coap;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.Request;
import eu.uberdust.testbedlistener.coap.udp.EthernetUDPhandler;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.UnknownHostException;

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
    private EthernetUDPhandler udphandler;
//    private Random rand;
    private Message udpRequest;

    public EthernetSupport(final EthernetUDPhandler udphandler, Message udpRequest) {
//        this.rand = new Random();
        this.udphandler = udphandler;
        this.udpRequest = udpRequest;
//        LOGGER.info("polling");
    }

    @Override
    public void run() {
        LOGGER.error("polling");

        Request request = new Request(CodeRegistry.METHOD_GET, false);
        request.setURI("/.well-known/core");
        int newMID = 0% 35000;
        if (newMID < 0) newMID = -newMID;
        request.setMID(newMID);
        LOGGER.info(udpRequest.getPeerAddress().getAddress().getHostAddress() + request.getUriPath());
        CoapServer.getInstance().addEthernet(udpRequest.getPeerAddress().getAddress().getHostAddress() + request.getUriPath(), request.getMID());
        try {
            udphandler.send(request, udpRequest.getPeerAddress().getAddress());
        } catch (UnknownHostException e) {
            LOGGER.error(e, e);
        } catch (IOException e) {
            LOGGER.error(e, e);
        }
    }

    public static void main(String[] args) {

    }
}
