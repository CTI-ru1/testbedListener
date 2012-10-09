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
        final Message udpRequest = Message.fromByteArray(inData);

        List<Option> udpOptions = udpRequest.getOptions();
        for (Option udpOption : udpOptions) {
            LOGGER.info("udp-option: " + udpOption.getName());
        }

        Request coapRequest;
        if (udpRequest.getType().equals(Message.messageType.CON)) {
            coapRequest = new Request(udpRequest.getCode(), true);
        } else {
            coapRequest = new Request(udpRequest.getCode(), false);
        }
        LOGGER.info("incoming request of type:" + udpRequest.getType());
        coapRequest.setType(udpRequest.getType());

        //set MID
        coapRequest.setMID(udpRequest.getMID());

        //set PAYLOAD
        coapRequest.setPayload(udpRequest.getPayload());

        //set URI_PATH
        final String uriPath = udpRequest.getUriPath();
        String nodeUrn = uriPath.substring(1, uriPath.indexOf('/', 1));
        final String newUri = uriPath.substring(uriPath.indexOf('/', 1));
        LOGGER.info("here");

        if (udpRequest.hasOption(OptionNumberRegistry.URI_PATH)) {
            final List<Option> uriPatha = Option.split(OptionNumberRegistry.URI_PATH, newUri, "/");
            coapRequest.setOptions(OptionNumberRegistry.URI_PATH, uriPatha);
        }

        if (udpRequest.hasOption(OptionNumberRegistry.URI_QUERY)) {
            //SET URI_QUERY
            final List<Option> uriPathb = Option.split(OptionNumberRegistry.URI_QUERY, udpRequest.getQuery(), "&");
            coapRequest.setOptions(OptionNumberRegistry.URI_QUERY, uriPathb);
        }

        if (udpRequest.hasOption(OptionNumberRegistry.BLOCK2)) {
            coapRequest.setOption(udpRequest.getOptions(OptionNumberRegistry.BLOCK2).get(0));
            byte[] bytes = udpRequest.getOptions(OptionNumberRegistry.BLOCK2).get(0).getRawValue();
        }

        LOGGER.info("123");
        if (udpRequest.hasOption(OptionNumberRegistry.OBSERVE)) {
            LOGGER.info("HAS OBSERVE");
            coapRequest.setOptions(OptionNumberRegistry.OBSERVE, udpRequest.getOptions(OptionNumberRegistry.OBSERVE));
        }
        if (udpRequest.hasOption(OptionNumberRegistry.TOKEN)) {
            LOGGER.info("HAS TOKEN");
            coapRequest.setOptions(OptionNumberRegistry.TOKEN, udpRequest.getOptions(OptionNumberRegistry.TOKEN));
        }

        LOGGER.info("UDP uriPath: " + udpRequest.getUriPath());
        LOGGER.info("UDP uriQuery: " + udpRequest.getQuery());
        LOGGER.info("COAP uriPath: " + coapRequest.getUriPath());
        LOGGER.info("COAP uriQuery: " + coapRequest.getQuery());

        List<Option> options = coapRequest.getOptions();
        for (Option option : options) {
            LOGGER.info("hasOption: " + option.getName());
        }


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
        LOGGER.info(data.length);
        int myLen = data.length;
        for (int i = data.length - 1; i >= 0; i--) {
            if (data[i] != 0) {
                myLen = i + 1;
                break;
            }
        }
        final byte[] adata = new byte[myLen];
        System.arraycopy(data, 0, adata, 0, myLen);
        LOGGER.info(adata.length);
        return adata;
    }
}
