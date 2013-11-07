package eu.uberdust.testbedlistener.coap.internal.handler;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.Message;
import eu.uberdust.testbedlistener.coap.CoapServer;
import eu.uberdust.testbedlistener.mqtt.MqttConnectionManager;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Internal Request Handler used to display information related to the Arduino Gateways Connected to this CoAP Server.
 * Displays 3 levels of information: (1) Top-Level for all Testbeds and Gateways, (2) Testbed-Level for all the Gateways
 * of the specific Testbed and (3) Gateway-Level for the specific Gateway.
 *
 * @author Dimitrios Amaxilatis
 * @date 6/13/13
 */
public class ArduinoGatewayRequestHandler implements InternalRequestHandlerInterface {
    /**
     * /**
     * LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(ArduinoGatewayRequestHandler.class);

    /**
     * InternalRequestHandlerInterface Constructor
     * Used to declare all the available endpoints handled by this Handler.
     *
     * @param internalRequestHandlers the map with the InternalRequestEndpoints for the CoapServer.
     */

    public ArduinoGatewayRequestHandler(final HashMap<String, InternalRequestHandlerInterface> internalRequestHandlers) {

        //final variable for the Timer.
        final InternalRequestHandlerInterface thisObject = this;

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    //Get All Testbeds/Gateways.
                    final Map<String, HashMap<String, Long>> gateways = CoapServer.getInstance().getArduinoGateways();

                    //Register Testbeds
                    for (final String testbedUrn : gateways.keySet()) {
                        internalRequestHandlers.put("/" + testbedUrn, thisObject);
                        //RegisterGateways
                        for (final String gatewayDevice : gateways.get(testbedUrn).keySet()) {
                            internalRequestHandlers.put("/" + testbedUrn + "/" + gatewayDevice, thisObject);
                        }
                    }
                } catch (final Exception e) {
                    //Used to avoid Exceptions in the TimerTask.
                    LOGGER.error(e, e);
                }
            }
        }, 1000, 10000);
    }

    @Override
    public void handle(final Message udpRequest, final Message response) {
        //Response
        final StringBuilder payload = new StringBuilder("");
        //Requested Path
        final String requestURIpath = udpRequest.getUriPath();
        //Top-Level
        if ("/gateways".equals(requestURIpath)) {
            if (udpRequest.getCode() == CodeRegistry.METHOD_GET) {

                final Map<String, HashMap<String, Long>> gways = CoapServer.getInstance().getArduinoGateways();
                for (final String gateway : gways.keySet()) {
                    payload.append(describeTestbed(gateway, gways.get(gateway)));

                }
                response.setPayload(payload.toString());
                return;
            } else {
                response.setPayload("Not Allowed!");
                response.setCode(CodeRegistry.RESP_METHOD_NOT_ALLOWED);
                return;
            }
        } else {

            final String[] parts = requestURIpath.split("/");
            if (udpRequest.getCode() == CodeRegistry.METHOD_GET) {
                if (parts.length == 2) {
                    final String testbedHash = parts[1];
                    if (CoapServer.getInstance().getArduinoGateways().containsKey(testbedHash)) {
                        HashMap<String, Long> gways = CoapServer.getInstance().getArduinoGateways().get(testbedHash);
                        payload.append(describeTestbed(testbedHash, gways));
                        response.setPayload(payload.toString());
                        return;
                    } else {
                        response.setCode(CodeRegistry.RESP_NOT_FOUND);
                        return;
                    }
                }
                if (parts.length == 3) {
                    final String testbedHash = parts[1];
                    final String deviceID = parts[2];
                    if (CoapServer.getInstance().getArduinoGateways().get(testbedHash).containsKey(deviceID)) {
                        final Long timestamp = CoapServer.getInstance().getArduinoGateways().get(testbedHash).get(deviceID);
                        payload.append(describeGateway(deviceID, timestamp));
                        response.setPayload(payload.toString());
                        return;
                    } else {
                        response.setCode(CodeRegistry.RESP_NOT_FOUND);
                        return;
                    }

                }
            } else if (udpRequest.getCode() == CodeRegistry.METHOD_POST) {
                if (parts.length == 3) {
                    final String testbedHash = parts[1];
                    final String deviceID = parts[2];
                    if (CoapServer.getInstance().getArduinoGateways().get(testbedHash).containsKey(deviceID)) {
                        if ("reset".equals(udpRequest.getPayloadString())) {
                            MqttConnectionManager.getInstance().publish("s" + testbedHash + "," + deviceID, "reset");
                            response.setPayload("Sent Reset To Gateway!");
                            response.setCode(CodeRegistry.RESP_VALID);
                            return;
                        } else {
                            response.setPayload("Bad Request Payload!");
                            response.setCode(CodeRegistry.RESP_BAD_REQUEST);
                            return;
                        }
                    } else {
                        response.setPayload("Gateway Device not Registed!");
                        response.setCode(CodeRegistry.RESP_NOT_FOUND);
                        return;
                    }
                } else {
                    response.setPayload("Not Allowed!");
                    response.setCode(CodeRegistry.RESP_METHOD_NOT_ALLOWED);
                    return;
                }
            } else {
                response.setPayload("Not Allowed!");
                response.setCode(CodeRegistry.RESP_METHOD_NOT_ALLOWED);
                return;
            }
        }
        response.setPayload("BAD REQUEST");
        response.setCode(CodeRegistry.RESP_BAD_REQUEST);
        return;

    }

    private String describeTestbed(final String gateway, final Map<String, Long> gateways) {
        final StringBuilder payload = new StringBuilder();
        payload.append(gateway);
        payload.append("\n");
        for (final String deviceId : gateways.keySet()) {
            final Long timestamp = gateways.get(deviceId);
            payload.append(describeGateway(deviceId, timestamp));
        }
        return payload.toString();
    }

    private String describeGateway(final String deviceID, final long timestamp) {
        final StringBuilder payload = new StringBuilder()
                .append("\t")
                .append(deviceID)
                .append(" @ ")
                .append(new Date(timestamp))
                .append("\n");
        return payload.toString();
    }

}
