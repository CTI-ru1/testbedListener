package eu.uberdust.testbedlistener.coap;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import edu.emory.mathcs.backport.java.util.Arrays;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: amaxilatis
 * Date: 9/25/12
 * Time: 12:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class CoapCheckTask {
    /**
     * COAP Server socket.
     */
    private transient DatagramSocket socket;

    public CoapCheckTask() {
        Request mess = new Request(CodeRegistry.METHOD_GET, false);
        mess.setURI("/.well-known/core");
        mess.setMID(new Random().nextInt());
        mess.setContentType(MediaTypeRegistry.TEXT_PLAIN);
        mess.setAccept(MediaTypeRegistry.TEXT_PLAIN);


        mess.prettyPrint();
//        System.out.println(Arrays.toString(mess.toByteArray()));
        //Start the udp socket
//        try {
//            socket = new DatagramSocket(5632);
//            System.out.println(socket.getPort());
//        } catch (SocketException e) {
//            e.printStackTrace();
//        }


//        byte[] buf = new byte[1024];
//        final DatagramPacket packet = new DatagramPacket(buf, 1024);
//
//        try {
//            if (socket == null) {
//                System.out.println("EEEEEERRROOORR");
//            }
//            DatagramPacket p = new DatagramPacket(mess.toByteArray(), 0, mess.toByteArray().length, InetAddress.getByName("150.140.5.64"), 5632);
//            new Thread((new Runnable() {
//                @Override
//                public void run() {
//                    System.out.println("Waiting for data");
//                    try {
//                        socket.receive(packet);
//                    } catch (IOException e) {
//                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//                    }
//                    System.out.println("Received");
//                }
//            })).start();

//            System.out.println("send");
//            socket.send(p);
//            System.out.println("sent");

//            socket.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

    }

    public static void main(String[] args) {
        new CoapCheckTask();
    }
}
