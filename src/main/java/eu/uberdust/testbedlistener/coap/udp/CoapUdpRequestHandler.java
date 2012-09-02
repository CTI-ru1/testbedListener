package eu.uberdust.testbedlistener.coap.udp;

import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import eu.uberdust.testbedlistener.coap.CoapServer;
import org.apache.log4j.Logger;

import java.net.DatagramPacket;
import java.util.List;

/**
 * Handler Thread fro the COAP upd packets of the server.
 */
public class CoapUdpRequestHandler implements Runnable {//NOPMD

    /**
     * LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(CoapUdpRequestHandler.class);

    private transient final DatagramPacket packet;

    /**
     * Constructor.
     *
     * @param packet The UDP packet of the request.
     */
    public CoapUdpRequestHandler(final DatagramPacket packet) {
        this.packet = packet;
    }

    @Override
    public void run() {
        final byte[] inData = cleanupData(packet.getData());
        final Message updRequest = Message.fromByteArray(inData);

        Request coapRequest;
        if (updRequest.getType().equals(Message.messageType.CON)) {

            coapRequest = new Request(updRequest.getCode(), true);
        } else {
            coapRequest = new Request(updRequest.getCode(), false);
        }

        //set MID
        coapRequest.setMID(updRequest.getMID());

        //set PAYLOAD
        coapRequest.setPayload(updRequest.getPayload());

        //set URI_PATH
        final String uriPath = updRequest.getUriPath();
        String nodeUrn = uriPath.substring(1, uriPath.indexOf('/', 1));
        final String newUri = uriPath.substring(uriPath.indexOf('/', 1));

        final List<Option> uriPatha = Option.split(OptionNumberRegistry.URI_PATH, newUri, "/");
        coapRequest.setOptions(OptionNumberRegistry.URI_PATH, uriPatha);

        //SET URI_QUERY
        final List<Option> uriPathb = Option.split(OptionNumberRegistry.URI_QUERY, updRequest.getQuery(), "&");
        coapRequest.setOptions(OptionNumberRegistry.URI_QUERY, uriPathb);

        LOGGER.info("UDP uriPath: " + updRequest.getUriPath());
        LOGGER.info("UDP uriQuery: " + updRequest.getQuery());
        LOGGER.info("COAP uriPath: " + coapRequest.getUriPath());
        LOGGER.info("COAP uriQuery: " + coapRequest.getQuery());

        LOGGER.info("NodeUrn: " + nodeUrn);

        //= packet.getData();
        //len = packet.getLength();
        final byte[] data = coapRequest.toByteArray();
        if (nodeUrn.contains("0x")) {
            nodeUrn = nodeUrn.substring(nodeUrn.indexOf("0x") + 2);

            final boolean hasQuery = coapRequest.hasOption(OptionNumberRegistry.URI_QUERY);
            LOGGER.info(packet.getSocketAddress());
            CoapServer.getInstance().addRequest(nodeUrn, coapRequest, packet.getSocketAddress(), hasQuery);
            CoapServer.getInstance().sendRequest(data, nodeUrn);

        } else {
            LOGGER.info("reply from uberdust! Device has no mac address");

            final Message message = new Message();
            message.setPayload("urn:node:capability:card");
            message.setURI(nodeUrn);

            CoapServer.getInstance().sendReply(message.toByteArray(), packet.getSocketAddress());
        }
    }

    /**
     * Clear tailing zeros from the udpPacket.
     *
     * @param data the incoming packet.
     * @return the data from the udp packet without tailing zeros.
     */
    private byte[] cleanupData(final byte[] data) {
        int myLen = data.length;
        for (int i = data.length - 1; i >= 0; i--) {
            if (data[i] == 0) {
                myLen = i;
            }
        }
        final byte[] adata = new byte[myLen];
        System.arraycopy(data, 0, adata, 0, myLen);
        LOGGER.info(adata.length);
        return adata;
    }
}
