package eu.uberdust.testbedlistener.coap.udp;

import ch.ethz.inf.vs.californium.coap.*;
import eu.uberdust.testbedlistener.coap.Cache;
import eu.uberdust.testbedlistener.coap.CacheHandler;
import eu.uberdust.testbedlistener.coap.CoapServer;
import eu.uberdust.testbedlistener.util.Converter;
import eu.uberdust.testbedlistener.util.PropertyReader;
import eu.uberdust.testbedlistener.util.TokenManager;
import eu.uberdust.testbedlistener.util.commiter.WsCommiter;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.util.List;
import java.util.Random;

/**
 * Handler Thread fro the COAP upd packets of the server.
 */
public class EthernetCoapHandler implements Runnable {//NOPMD

    /**
     * LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(EthernetCoapHandler.class);
    private Response response;
    private String address;
    private String testbedPrefix;
    private String capabilityPrefix;
    private EthernetUDPhandler udPhandler;
    private Random rand;
    private final SocketAddress socketAddress;

    /**
     * Constructor.
     */
    public EthernetCoapHandler(final Response response, final DatagramPacket packet, EthernetUDPhandler udPhandler) {
        this.udPhandler = udPhandler;
        this.response = response;
        this.socketAddress = packet.getSocketAddress();
        this.address = packet.getAddress().getHostAddress();
        this.testbedPrefix = PropertyReader.getInstance().getTestbedPrefix();
        this.capabilityPrefix = PropertyReader.getInstance().getTestbedCapabilitiesPrefix();
        rand = new Random();
    }

    @Override
    public void run() {
        String payload = response.getPayloadString();
        LOGGER.info(payload);
        StringBuilder payloadBuilder = new StringBuilder();
        for (Character c : payload.toCharArray()) {
            if (c != 0) {
                payloadBuilder.append(c);
            }
        }

        payload = CoapServer.getInstance().getPending(response.getMID()) + payloadBuilder.toString();
        payload = payload.replaceAll("mA", "");
        LOGGER.info("Payload " + payload);
        String path;
        if (response.hasOption(OptionNumberRegistry.TOKEN)) {
            path = CoapServer.getInstance().checkEthernet(response.getTokenString());
        } else {
            path = CoapServer.getInstance().checkEthernet(response.getMID());
        }
        LOGGER.info(path);
        if (path.endsWith("/.well-known/core")) {
            LOGGER.info("is for well-known");
            List<String> capabilities = Converter.extractCapabilities(payload);
            reportToUberdustCapability(capabilities, address);

            for (String capability : capabilities) {
                if (capability.contains("well-known/core")) {
                    continue;
                }
                final Cache pair = CacheHandler.getInstance().getValue(address, capability);
                if (pair != null) {
                    if (System.currentTimeMillis() - pair.getTimestamp() < pair.getMaxAge() * 1000) {
                        LOGGER.info("Skipping " + address + "/" + capability + " as UpToDate");
                        continue;
                    } else {
                        LOGGER.error("Outdated " + address + "/" + capability + " timeout:" + pair.getTimestamp());
                    }
                }
//                if (capability.contains("1"))
                requestCapability(capability);
//                if (capability.contains("2"))
//                    requestCapability(capability);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
            LOGGER.info("HERE:" + response.getOptions(OptionNumberRegistry.BLOCK2).get(0).getIntValue());
            if (response.hasOption(OptionNumberRegistry.BLOCK2)) {
                if (response.getOptions(OptionNumberRegistry.BLOCK2).get(0).getIntValue() == 10) {
                    LOGGER.info("Requesting Block2 ");
                    requestBlock2(Converter.extractRemainder(payload));
                } else if (response.getOptions(OptionNumberRegistry.BLOCK2).get(0).getIntValue() == 26) {
                    LOGGER.info("Requesting Block3 ");
                    requestBlock3(Converter.extractRemainder(payload));
                } else if (response.getOptions(OptionNumberRegistry.BLOCK2).get(0).getIntValue() == 34) {
                    LOGGER.info("Requesting Block4 ");
                    //requestBlock4(Converter.extractRemainder(payload));
                } else {

                }
            }
        } else {


            String ip = path.substring(0, path.indexOf("/"));
            LOGGER.info(ip);
            String capability = path.replace(ip, "").substring(1);
            LOGGER.info(ip);
            LOGGER.info("Capability " + capability);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    CoapServer.getInstance().ackEthernet(udPhandler, response, socketAddress);
                }
            }).start();

            commitNodeReading(ip, capability, Double.valueOf(payload));


        }
    }

    private void requestCapability(final String capability) {
        Request request = new Request(CodeRegistry.METHOD_GET, false);
        request.setURI(capability);
        int newMID = rand.nextInt() % 65000;
        if (newMID < 0) {
            newMID = -newMID;
        }
        request.setMID(newMID);
        request.setOption(new Option(0, OptionNumberRegistry.OBSERVE));
        request.setToken(TokenManager.getInstance().acquireToken());

        CoapServer.getInstance().addEthernet(address + request.getUriPath(), request.getTokenString());
        try {
            udPhandler.send(request, address);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private void requestBlock2(final String remainder) {
        LOGGER.info("needs blockwise");

        for (Option option : response.getOptions()) {
            if (OptionNumberRegistry.BLOCK2 == option.getOptionNumber() && (option.getIntValue() == 18)) {
                return;

            }
        }
        Request request = new Request(CodeRegistry.METHOD_GET, false);
        request.setURI("/.well-known/core");
        request.setMID(response.getMID() + 1);
        Option blockOption = new Option(0x12, OptionNumberRegistry.BLOCK2);
        request.addOption(blockOption);
        request.prettyPrint(System.out);
        CoapServer.getInstance().addEthernet(address + request.getUriPath(), request.getMID());
        CoapServer.getInstance().addPending(request.getMID(), remainder);

        LOGGER.info(request.getUriPath());
        try {
            udPhandler.send(request, address);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private void requestBlock3(final String remainder) {
        LOGGER.info("needs blockwise");

        for (Option option : response.getOptions()) {
            if (OptionNumberRegistry.BLOCK2 == option.getOptionNumber() && (option.getIntValue() == 18)) {
                return;

            }
        }
        Request request = new Request(CodeRegistry.METHOD_GET, false);
        request.setURI("/.well-known/core");
        request.setMID(response.getMID() + 1);
        Option blockOption = new Option(0x13, OptionNumberRegistry.BLOCK2);
        request.addOption(blockOption);
        request.prettyPrint(System.out);
        CoapServer.getInstance().addEthernet(address + request.getUriPath(), request.getMID());
        CoapServer.getInstance().addPending(request.getMID(), remainder);

        LOGGER.info(request.getUriPath());
        try {
            udPhandler.send(request, address);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private void requestBlock4(final String remainder) {
        LOGGER.info("needs blockwise");

        for (Option option : response.getOptions()) {
            if (OptionNumberRegistry.BLOCK2 == option.getOptionNumber() && (option.getIntValue() == 18)) {
                return;

            }
        }
        Request request = new Request(CodeRegistry.METHOD_GET, false);
        request.setURI("/.well-known/core");
        request.setMID(response.getMID() + 1);
        Option blockOption = new Option(0x14, OptionNumberRegistry.BLOCK2);
        request.addOption(blockOption);
        request.prettyPrint(System.out);
        CoapServer.getInstance().addEthernet(address + request.getUriPath(), request.getMID());
        CoapServer.getInstance().addPending(request.getMID(), remainder);

        LOGGER.info(request.getUriPath());
        try {
            udPhandler.send(request, address);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private void commitNodeReading(final String nodeId, String capability, final Double value) {
        if (capability.equals("temp")) {
            capability = "temperature";
        }

        capability = capability.replace("Relay", "r");
        capability = capability.replace("Sensor", "s");
        capability = capability.replace("I", "i");

        CacheHandler.getInstance().setValue(nodeId, capability, 30, 30, value.toString());

        final String nodeUrn = testbedPrefix + nodeId;
        final String capabilityName = (capabilityPrefix + capability).toLowerCase();

        final eu.uberdust.communication.protobuf.Message.NodeReadings.Reading reading = eu.uberdust.communication.protobuf.Message.NodeReadings.Reading.newBuilder()
                .setNode(nodeUrn)
                .setCapability(capabilityName)
                .setTimestamp(System.currentTimeMillis())
                .setDoubleReading(value)
                .build();

        new WsCommiter(reading);
    }


    private void reportToUberdustCapability(List<String> capabilities, String address) {
        eu.uberdust.communication.protobuf.Message.NodeReadings.Builder readings = eu.uberdust.communication.protobuf.Message.NodeReadings.newBuilder();
        for (String capability : capabilities) {
            if (capability.equals("temp")) {
                capability = "temperature";
            }
            capability = capability.replace("Relay", "r");
            capability = capability.replace("Sensor", "s");
            capability = capability.replace("I", "i");

            final String nodeUrn = testbedPrefix + address;
            final String capabilityName = (capability).toLowerCase();

            final eu.uberdust.communication.protobuf.Message.NodeReadings.Reading reading = eu.uberdust.communication.protobuf.Message.NodeReadings.Reading.newBuilder()
                    .setNode(nodeUrn)
                    .setCapability("report")
                    .setTimestamp(System.currentTimeMillis())
                    .setStringReading(capabilityName)
                    .build();
            readings.addReading(reading);
        }
        new WsCommiter(readings.build());
    }
}
