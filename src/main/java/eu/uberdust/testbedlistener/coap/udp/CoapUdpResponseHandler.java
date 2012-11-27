package eu.uberdust.testbedlistener.coap.udp;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import eu.uberdust.testbedlistener.coap.CoapServer;
import eu.uberdust.testbedlistener.util.Converter;
import eu.uberdust.testbedlistener.util.PropertyReader;
import eu.uberdust.testbedlistener.util.TokenManager;
import eu.uberdust.testbedlistener.util.commiter.WsCommiter;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Random;

/**
 * Handler Thread fro the COAP upd packets of the server.
 */
public class CoapUdpResponseHandler implements Runnable {//NOPMD

    /**
     * LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(CoapUdpResponseHandler.class);
    private Response response;
    private String address;
    private String testbedPrefix;
    private String capabilityPrefix;
    private UDPhandler udPhandler;
    private Random rand;

    /**
     * Constructor.
     */
    public CoapUdpResponseHandler(final Response response, final String address, UDPhandler udPhandler) {
        this.udPhandler = udPhandler;
        this.response = response;
        this.address = address;
        this.testbedPrefix = PropertyReader.getInstance().getTestbedPrefix();
        this.capabilityPrefix = PropertyReader.getInstance().getTestbedCapabilitiesPrefix();
        rand = new Random();
    }

    @Override
    public void run() {
        LOGGER.info("Path " + response.getUriPath());
        String payload = response.getPayloadString();
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
            List<String> capabilities = Converter.extractCapabilities(payload);
            reportToUberdustCapability(capabilities, address);
            for (String capability : capabilities) {
                if (capability.contains("well-known/core")) continue;
                requestCapability(capability);
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }


            LOGGER.info("is for well-known");
            if (response.hasOption(OptionNumberRegistry.BLOCK2)) {
                requestBlock2(Converter.extractRemainder(payload));
            }
        } else {
            CoapServer.getInstance().ackEthernet(udPhandler, response, address);


            String ip = path.substring(0, path.indexOf("/"));
            LOGGER.info(ip);
            String capability = path.replace(ip, "").substring(1);
            LOGGER.info(ip);
            LOGGER.info("Capability " + capability);
            String index = capability.split("/")[0];
            String type = capability.split("/")[1];
            LOGGER.info("Value " + type);
            String finalCapability = "c".equals(type) ? "consumption" + index : "switch" + index;
            commitNodeReading(ip, finalCapability, Double.valueOf(payload));
        }
    }

    private void requestCapability(final String capability) {
        Request request = new Request(CodeRegistry.METHOD_GET, false);
        request.setURI(capability);
        int newMID = rand.nextInt() % 65000;
        if (newMID < 0) newMID = -newMID;
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
            if (OptionNumberRegistry.BLOCK2 == option.getOptionNumber()) {
                if (option.getIntValue() == 18) {
                    return;
                }
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


    private void commitNodeReading(final String nodeId, String capability, final Double value) {
        if (capability.equals("temp")) {
            capability = "temperature";
        }


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
