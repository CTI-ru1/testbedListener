package eu.uberdust.datacollector;

import com.rapplogic.xbee.api.XBeeAddress16;
import eu.mksense.XBeeRadio;
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
        StringBuilder packet2String = new StringBuilder("request:");
        for (byte b : data) {
            packet2String.append(Byte.toString(b)).append("|");
        }
        LOGGER.info(packet2String.toString());
        final Integer[] macAddress = new Integer[2];
        String destination = "472";
        if (destination.length() == 4) {
            macAddress[0] = Integer.valueOf(destination.substring(0, 2), 16);
            macAddress[1] = Integer.valueOf(destination.substring(2, 4), 16);
        } else if (destination.length() == 3) {
            macAddress[0] = Integer.valueOf(destination.substring(0, 1), 16);
            macAddress[1] = Integer.valueOf(destination.substring(1, 3), 16);
        }

        LOGGER.info("here");
        final XBeeAddress16 address16 = new XBeeAddress16(macAddress[0], macAddress[1]);
        LOGGER.info("here1");

        int[] bytes = new int[len+ 1];
        bytes[0] = 51;
        for (int i = 0; i < len; i++) {
            short read = (short) ((short) data[i] & 0xff);
            bytes[i + 1] = read;
        }

        StringBuilder message = new StringBuilder("Requesting:");
        for (int i = 0; i < len; i++) {
            message.append(bytes[i]).append("|");
        }

        LOGGER.info(message.toString());
        try {
            LOGGER.info("sending to device");
            XBeeRadio.getInstance().send(address16, 112, bytes);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}