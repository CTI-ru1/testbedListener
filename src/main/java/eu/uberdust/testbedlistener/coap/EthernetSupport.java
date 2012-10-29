package eu.uberdust.testbedlistener.coap;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import eu.uberdust.testbedlistener.coap.udp.UDPhandler;
import eu.uberdust.testbedlistener.util.PropertyReader;
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
public class EthernetSupport implements Runnable {
    /**
     * LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(EthernetSupport.class);
    private UDPhandler udphandler;
    private Random rand;

    public EthernetSupport(UDPhandler thread) {
        this.udphandler = thread;
        this.rand = new Random();
    }

    @Override
    public void run() {
        LOGGER.info("polling");
        String devices = (String) PropertyReader.getInstance().getProperties().get("polldevices");
//        while (true) {
        for (String device : devices.split(",")) {
            LOGGER.info("Sending to :" + device);
            Request request = new Request(CodeRegistry.METHOD_GET, false);
            request.setURI("/.well-known/core");
            int newMID = rand.nextInt() % 65000;
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

//            for (String device : devices.split(",")) {
//                LOGGER.info("Sending to :" + device);
//                Request request = new Request(CodeRegistry.METHOD_GET, false);
//                request.setURI("/3/c");
//                request.setMID(rand.nextInt() % 65000);
//                CoapServer.getInstance().addEthernet(device + request.getUriPath(), request.getMID());
//                try {
//                    udphandler.send(request, device);
//                } catch (UnknownHostException e) {
//                    LOGGER.error(e, e);
//                } catch (IOException e) {
//                    LOGGER.error(e, e);
//                }
//
//            }


        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
//    }
}
