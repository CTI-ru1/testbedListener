package eu.uberdust.testbedlistener.coap.udp;

import ch.ethz.inf.vs.californium.coap.Response;
import org.apache.log4j.Logger;

/**
 * Handler Thread fro the COAP upd packets of the server.
 */
public class CoapUdpResponseHandler implements Runnable {//NOPMD

    /**
     * LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(CoapUdpResponseHandler.class);
    private Response response;
    private String address;

    /**
     * Constructor.
     */
    public CoapUdpResponseHandler(final Response response, final String address) {
        this.response = response;
        this.address = address;
    }

    @Override
    public void run() {
        LOGGER.info("Path " + response.getUriPath());
        LOGGER.info("Payload " + response.getPayloadString());
    }
}
