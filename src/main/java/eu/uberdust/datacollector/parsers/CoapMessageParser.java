package eu.uberdust.datacollector.parsers;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import com.rapplogic.xbee.api.XBeeAddress16;
import eu.mksense.XBeeRadio;
import org.apache.log4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Random;

import static ch.ethz.inf.vs.californium.coap.CodeRegistry.METHOD_GET;

/**
 * Parses an XBee message received and adds data to a wisedb database.
 */
public class CoapMessageParser implements Runnable {

    /**
     * LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(CoapMessageParser.class);

    /**
     * Testbed Capability prefix.
     */
    private final String capabilityPrefix;

    /**
     * Index of capability.
     */
    private final static int INDEX = 7;

    /**
     * The testbed prefix.
     */
    private final String testbedPrefix;

    /**
     * The testbed id.
     */
    private final int testbedId;

    /**
     * The Mac Address of the remote node.
     */
    private final XBeeAddress16 remoteAddress;

    /**
     * The payload of the received message.
     */
    private final int[] payload;

    /**
     * Default Constructor.
     *
     * @param address       the address of the node
     * @param payload       the payload message to be parsed.
     * @param testbedPrefix the testbed prefix
     * @param testbedId     the testbed id
     */
    public CoapMessageParser(final XBeeAddress16 address, final int[] payload,
                             final String testbedPrefix, final int testbedId, final String capabilityPrefix) {

        this.payload = payload;
        remoteAddress = address;
        this.testbedPrefix = testbedPrefix;
        this.capabilityPrefix = capabilityPrefix;
        this.testbedId = testbedId;
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        final String address = Integer.toHexString(remoteAddress.getAddress()[0]) + Integer.toHexString(remoteAddress.getAddress()[1]);

        if (!address.equals("472")) return;
        LOGGER.info("from " + address + " with " + payload[0] + " Length is: " + payload.length + "@ " + new Date(System.currentTimeMillis()));
        StringBuilder stringBuilder = new StringBuilder("contents:");
        for (int i : payload) {
            stringBuilder.append(Integer.toHexString(i)).append("|");
        }
        LOGGER.info(stringBuilder.toString());
        if (payload[0] == 1)   // check for broadcasting message
        {
//            if (CoapServer.getInstance().registerEndpoint(address)) {
            //Message request = new Message();
            //request.setURI("/.well-known/core");
            int[] mpayload = new int["33:52:01:00:00:9b:2e:77:65:6c:6c:2d:6b:6e:6f:77:6e:04:63:6f:72:65".split(":").length];
            int i = 0;
            for (String mbyte : "33:52:01:00:00:9b:2e:77:65:6c:6c:2d:6b:6e:6f:77:6e:04:63:6f:72:65".split(":")) {
                mpayload[i] = Integer.parseInt(mbyte, 16);
                i++;
            }
            try {
                LOGGER.info("Sending to arduino");
                XBeeRadio.getInstance().send(remoteAddress, 112, mpayload);
            } catch (Exception e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
//            }
        } else if (payload[0] == 51) // coap message
        {
            byte byteArr[] = new byte[payload.length];
            for (int i = 1; i < payload.length; i++) {
                byteArr[i] = (byte) payload[i];

            }
            //need to parse message
            //TODO californium parser
            if (payload[3] == 0 && payload[4] == 0) {  //getting .well-known/core autoconfig phase
                Message response = Message.fromByteArray(byteArr);
                byte[] inPayload = response.getPayload();
                StringBuilder message = new StringBuilder("Response:");
                for (int i = 0; i < inPayload.length; i++) {
                    message.append((char) inPayload[i]);

                }
                LOGGER.info(message.toString());
                String[] temp = message.toString().split("<");
                for(int i=2; i<temp.length; i++)
                {
                    String[] temp2 = temp[i].split(">");
                    String[] temp3 = new String[temp2[0].length()+1];
                    temp3[0] = "/"+temp2[0];
                    LOGGER.info(temp3);
                    Random mid = new Random();
                    URI uri = null;
                    StringBuilder temp4 = new StringBuilder();
                    for (String s : temp3) {
                        temp4.append(s);
                    }
                    try {
                        uri = new URI(temp4.toString());
                    } catch (URISyntaxException e) {
                        e.printStackTrace();//To change body of catch statement use File | Settings | File Templates.
                    }
                    Message request = new Message(uri, Message.messageType.NON, METHOD_GET, mid.nextInt() % 65535, null);
                    //request.addOption(new Option(OptionNumberRegistry.OBSERVE));

                    byte[] toSend = request.toByteArray();
                    int len = toSend.length;
                    int[] bytes = new int[len+ 1];
                    bytes[0] = 51;
                    for (int k = 0; k < len; k++) {
                        short read = (short) ((short) toSend[i] & 0xff);
                        bytes[k + 1] = read;
                    }

                    StringBuilder messageINFO = new StringBuilder("Requesting:");
                    for (int k = 0; i < len; i++) {
                        message.append(bytes[k]).append("|");
                    }

                    LOGGER.info(messageINFO.toString());
                    final Integer[] macAddress = new Integer[2];
                    String destination = "472";
                    if (destination.length() == 4) {
                        macAddress[0] = Integer.valueOf(destination.substring(0, 2), 16);
                        macAddress[1] = Integer.valueOf(destination.substring(2, 4), 16);
                    } else if (destination.length() == 3) {
                        macAddress[0] = Integer.valueOf(destination.substring(0, 1), 16);
                        macAddress[1] = Integer.valueOf(destination.substring(1, 3), 16);
                    }
                    final XBeeAddress16 address16 = new XBeeAddress16(macAddress[0], macAddress[1]);
                    try {
                        LOGGER.info("sending to device");
                        XBeeRadio.getInstance().send(address16, 112, bytes);
                    } catch (Exception e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }
                //Request request = new GETRequest();
                //request.setURI();
                return;
            }

        }

    }
}
