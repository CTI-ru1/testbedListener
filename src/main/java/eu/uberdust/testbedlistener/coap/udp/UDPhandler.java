package eu.uberdust.testbedlistener.coap.udp;

import ch.ethz.inf.vs.californium.coap.Message;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

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
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("UDPhandler-Thread #%d").build();
        executorService = Executors.newCachedThreadPool(threadFactory);
    }


    @Override
    public void run() {

        while (true) {
            final byte[] buf = new byte[1024];
            final DatagramPacket packet = new DatagramPacket(buf, 1024);
            try {
                LOGGER.info("Waiting for data");
                socket.receive(packet);
                LOGGER.info("received from " + packet.getSocketAddress());
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
        final byte[] inData = cleanupData(packet.getData());
//        try {
//        final Request udpRequest = (Request) Message.fromByteArray(inData);
        executorService.submit(new CoapUdpRequestHandler(packet));
//        } catch (ClassCastException cce) {
//            LOGGER.info("ISRESPONSE");
//            final Response udpRequest = (Response) Message.fromByteArray(inData);
//            executorService.submit(new CoapUdpRequestHandler(udpRequest, packet.getAddress().getHostAddress(), this));
//        }

    }

    public void send(Message request, String device) throws IOException {
        DatagramPacket packet = new DatagramPacket(request.toByteArray(), request.toByteArray().length, InetAddress.getByName(device), 5683);
        socket.send(packet);
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
