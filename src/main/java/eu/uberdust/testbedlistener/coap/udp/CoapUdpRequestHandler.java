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
 * Created by IntelliJ IDEA.
 * User: amaxilatis
 * Date: 5/19/12
 * Time: 7:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class CoapUdpRequestHandler implements Runnable {

    /**
     * LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(CoapUdpRequestHandler.class);

    private DatagramPacket packet;

    public CoapUdpRequestHandler(DatagramPacket packet) {
        this.packet = packet;
    }

    @Override
    public void run() {
        byte[] adata = packet.getData();
        int myLen = adata.length;
        for (int i = adata.length - 1; i >= 0; i--) {
            if (adata[i] == 0) {
                myLen = i;
            }
        }
        LOGGER.info(myLen);
        byte[] adata2 = new byte[myLen];
        System.arraycopy(adata, 0, adata2, 0, myLen);
        LOGGER.info(adata.length);
        Message mess = Message.fromByteArray(adata2);
        Message request = new Message(mess.getType(), mess.getCode());
        request.setMID(mess.getMID());
        request.setPayload(mess.getPayload());
        LOGGER.info(mess.getPayload().length);
//        for (Option option : mess.getOptions()) {
//
//            if (option.getIntValue() != OptionNumberRegistry.URI_PATH) {
//                LOGGER.info("1");
//                request.addOption(option);
//            }
//        }
        final String uriPath = mess.getUriPath();
        String nodeUrn = uriPath.substring(1, uriPath.indexOf("/", 1));
        String newUri = uriPath.substring(uriPath.indexOf("/", 1));
        LOGGER.info(newUri);
        List<Option> uriPatha = Option.split(OptionNumberRegistry.URI_PATH, newUri, "/");
        request.setOptions(OptionNumberRegistry.URI_PATH, uriPatha);
        List<Option> uriPathb = Option.split(OptionNumberRegistry.URI_QUERY, mess.getQuery(), "&");
        request.setOptions(OptionNumberRegistry.URI_QUERY, uriPathb);
        LOGGER.info("OptionCount" + request.getOptionCount());

        LOGGER.info(mess.getUriPath());
        LOGGER.info(nodeUrn);

        //= packet.getData();
        //len = packet.getLength();
        byte[] data = request.toByteArray();
        int len = data.length;
        if (nodeUrn.contains("0x")) {
            nodeUrn = nodeUrn.substring(nodeUrn.indexOf("0x") + 2);

            int[] bytes = new int[len + 1];
            bytes[0] = 51;
            for (int i = 0; i < len; i++) {
                short read = (short) ((short) data[i] & 0xff);
                bytes[i + 1] = read;
            }

            StringBuilder messageBinary = new StringBuilder("Requesting[Bytes]:");
            for (int i = 0; i < len + 1; i++) {
                messageBinary.append(bytes[i]).append("|");
            }
            LOGGER.info(messageBinary.toString());

            final int[] macAddress = Converter.AddressToInteger(nodeUrn);
            CoapServer.getInstance().addRequest(nodeUrn, request);
            final XBeeAddress16 address16 = new XBeeAddress16(macAddress[0], macAddress[1]);

            try {
                LOGGER.info("sending to device");
                XBeeRadio.getInstance().send(address16, 112, bytes);
            } catch (Exception e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        } else {
            LOGGER.info("reply from uberdust!");

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
}
