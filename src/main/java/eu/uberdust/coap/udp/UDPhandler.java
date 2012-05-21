package eu.uberdust.coap.udp;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by IntelliJ IDEA.
 * User: amaxilatis
 * Date: 5/19/12
 * Time: 7:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class UDPhandler extends Thread {
    private static final Logger LOGGER = Logger.getLogger(CoapUdpRequestHandler.class);

    private DatagramSocket socket;
    private DatagramPacket packet;

    /**
     * executors for handling incoming messages.
     */
    private final transient ExecutorService executorService;

    public UDPhandler(DatagramSocket socket) {
        this.socket = socket;
        executorService = Executors.newCachedThreadPool();
    }


    @Override
    public void run() {
        byte[] buf = new byte[1024];
        packet = new DatagramPacket(buf, 1024);
        while (true) {
            try {
                LOGGER.info("Waiting for data");
                socket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            executorService.submit(new CoapUdpRequestHandler(packet));

        }
    }
}
