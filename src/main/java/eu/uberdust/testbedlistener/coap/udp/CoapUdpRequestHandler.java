package eu.uberdust.testbedlistener.coap.udp;

import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import com.rapplogic.xbee.api.XBeeAddress16;
import eu.mksense.XBeeRadio;
import eu.uberdust.testbedlistener.coap.CoapServer;
import eu.uberdust.testbedlistener.util.Converter;
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

        final Message coapRequest = new Message(updRequest.getType(), updRequest.getCode());

        //set MID
        coapRequest.setMID(updRequest.getMID());

        //set PAYLOAD
        coapRequest.setPayload(updRequest.getPayload());

        //set URI_PATH
        final String uriPath = updRequest.getUriPath();
        String nodeUrn = uriPath.substring(1, uriPath.indexOf("/", 1));
        String newUri = uriPath.substring(uriPath.indexOf("/", 1));
        List<Option> uriPatha = Option.split(OptionNumberRegistry.URI_PATH, newUri, "/");
        coapRequest.setOptions(OptionNumberRegistry.URI_PATH, uriPatha);

        //SET URI_QUERY
        List<Option> uriPathb = Option.split(OptionNumberRegistry.URI_QUERY, updRequest.getQuery(), "&");
        coapRequest.setOptions(OptionNumberRegistry.URI_QUERY, uriPathb);

        LOGGER.info("UDP uriPath: " + updRequest.getUriPath());
        LOGGER.info("UDP uriQuery: " + updRequest.getQuery());
        LOGGER.info("COAP uriPath: " + coapRequest.getUriPath());
        LOGGER.info("COAP uriQuery: " + coapRequest.getQuery());

        LOGGER.info("NodeUrn: " + nodeUrn);

        //= packet.getData();
        //len = packet.getLength();
        final byte[] data = coapRequest.toByteArray();
        int len = data.length;
        if (nodeUrn.contains("0x")) {
            nodeUrn = nodeUrn.substring(nodeUrn.indexOf("0x") + 2);

            final int[] bytes = new int[len + 1];
            bytes[0] = 51;
            for (int i = 0; i < len; i++) {
                short read = (short) ((short) data[i] & 0xff);
                bytes[i + 1] = read;
            }

            final StringBuilder messageBinary = new StringBuilder("Requesting[Bytes]:");
            for (int i = 0; i < len + 1; i++) {
                messageBinary.append(bytes[i]).append("|");
            }
            LOGGER.info(messageBinary.toString());

            final int[] macAddress = Converter.AddressToInteger(nodeUrn);
            CoapServer.getInstance().addRequest(nodeUrn, coapRequest);
            final XBeeAddress16 address16 = new XBeeAddress16(macAddress[0], macAddress[1]);

            try {
                LOGGER.info("sending to device");
                XBeeRadio.getInstance().send(address16, 112, bytes);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        } else {
            LOGGER.info("reply from uberdust! Device has no mac address");

            Message message = new Message();
            message.setPayload("urn:node:capability:card");
            message.setURI(nodeUrn);

            byte[] buf = message.toByteArray();
            DatagramPacket replyPacket = new DatagramPacket(buf, buf.length);
            replyPacket.setData(buf);
            replyPacket.setSocketAddress(packet.getSocketAddress());

            CoapServer.getInstance().sendReply(replyPacket);
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
