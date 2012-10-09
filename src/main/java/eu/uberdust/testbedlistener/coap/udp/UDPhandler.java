package eu.uberdust.testbedlistener.coap.udp;

import ch.ethz.inf.vs.californium.coap.Request;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * UDP handler thread.
 * Continously waits for new incoming udp connections and starts new handler threads.
 */
public class UDPhandler extends Thread {//NOPMD
    /**
     * LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(UDPhandler.class);

    private transient final DatagramSocket socket;

    /**
     * executors for handling incoming messages.
     */
    private final transient ExecutorService executorService;

    public UDPhandler(final DatagramSocket socket) {
        this.socket = socket;
        executorService = Executors.newCachedThreadPool();
    }


    @Override
    public void run() {
        final byte[] buf = new byte[1024];
        while (true) {
            final DatagramPacket packet = new DatagramPacket(buf, 1024);
            try {
                LOGGER.info("Waiting for data");
                socket.receive(packet);
                LOGGER.info("received from " + packet.getAddress().getHostAddress());
            } catch (IOException e) {
                LOGGER.fatal(e.getMessage(), e);
            }
            processNewRequest(packet);


        }
    }

    /**
     * Adds the new request to the executorService.
     *
     * @param packet the request as a UDP packet.
     */
    private void processNewRequest(final DatagramPacket packet) {
        executorService.submit(new CoapUdpRequestHandler(packet));
    }

    public void send(Request request, String device) throws IOException {
        DatagramPacket packet = new DatagramPacket(request.toByteArray(), request.toByteArray().length, InetAddress.getByName(device), 5683);
        socket.send(packet);
    }
}
