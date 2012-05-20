package eu.uberdust.datacollector;

import ch.ethz.inf.vs.californium.coap.Message;
import com.rapplogic.xbee.api.XBeeAddress16;
import org.apache.log4j.Logger;

import java.net.DatagramPacket;

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
        byte[] data = packet.getData();
        int len = packet.getLength();

        Message mess = Message.fromByteArray(data);

        final String uriPath = mess.getUriPath();
        String nodeUrn = uriPath.substring(1, uriPath.indexOf("/", 1));

        LOGGER.info(mess.getUriPath());
        LOGGER.info(nodeUrn);

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

            final Integer[] macAddress = new Integer[2];

            if (nodeUrn.length() == 4) {
                macAddress[0] = Integer.valueOf(nodeUrn.substring(0, 2), 16);
                macAddress[1] = Integer.valueOf(nodeUrn.substring(2, 4), 16);
            } else if (nodeUrn.length() == 3) {
                macAddress[0] = Integer.valueOf(nodeUrn.substring(0, 1), 16);
                macAddress[1] = Integer.valueOf(nodeUrn.substring(1, 3), 16);
            }

            final XBeeAddress16 address16 = new XBeeAddress16(macAddress[0], macAddress[1]);

            try {
                LOGGER.info("sending to device");
//            XBeeRadio.getInstance().send(address16, 112, bytes);
            } catch (Exception e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        } else {
            LOGGER.info("reply from uberdust!");
        }
    }
}
