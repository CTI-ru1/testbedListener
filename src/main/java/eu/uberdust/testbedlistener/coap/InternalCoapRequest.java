package eu.uberdust.testbedlistener.coap;

import ch.ethz.inf.vs.californium.coap.*;
import eu.uberdust.testbedlistener.coap.internal.handler.*;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: ven0m
 * Date: 11/16/12
 * Time: 3:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class InternalCoapRequest {

    private static InternalCoapRequest instance = null;
    private HashMap<String, InternalRequestHandler> internalRequestHandlers;

    /**
     * Empty Constructor.
     */
    public InternalCoapRequest() {
        internalRequestHandlers = new HashMap<String, InternalRequestHandler>();
        internalRequestHandlers.put("/status", new StatusRequestHandler());
        internalRequestHandlers.put("/endpoints", new EndpointsRequestHandler());
        internalRequestHandlers.put("/activeRequests", new ActiveRequestsRequestHandler());
        internalRequestHandlers.put("/pendingRequests", new PendingRequestsRequestHandler());
        internalRequestHandlers.put("/cache", new CacheRequestHandler());
        internalRequestHandlers.put("/wakeup", new WakeupRequestHandler());
        internalRequestHandlers.put("/routes", new RoutesRequestHandler());
        internalRequestHandlers.put("/ethernet", new EthernetRequestHandler());
        internalRequestHandlers.put("/gateway/xbee", new XbeeGatewayRequestHandler());
        internalRequestHandlers.put("/gateway/arduino", new ArduinoGatewayRequestHandler());
    }

    public static InternalCoapRequest getInstance() {
        synchronized (InternalCoapRequest.class) {
            if (instance == null) {
                instance = new InternalCoapRequest();
            }
            return instance;
        }
    }

    public Message handleRequest(final String uriHost, final Message udpRequest, final SocketAddress socketAddress) {
        //To change body of created methods use File | Settings | File Templates.
        final Message response = new Message();

        if (udpRequest.isConfirmable()) {
            response.setType(Message.messageType.ACK);
        } else if (udpRequest.isNonConfirmable()) {
            response.setType(Message.messageType.NON);
        }
        response.setCode(CodeRegistry.RESP_CONTENT);
        response.setMID(udpRequest.getMID());

        final String path = udpRequest.getUriPath();
        if (!"".equals(uriHost) && !"/cache".equals(path)) {
            return udpRequest;
        }
        if (path.contains("/device/")) {
            //forward to device or respond from cache
            StringBuilder payload = new StringBuilder("");

            String[] temp = path.split("/device/");
            temp = temp[1].split("/");
            final String device = temp[0];
            final StringBuilder uriPath = new StringBuilder();
            for (int i = 1; i < temp.length; i++) {
                uriPath.append("/").append(temp[i]);
            }
            udpRequest.setURI(uriPath.toString());
            final Option host = new Option(OptionNumberRegistry.URI_HOST);
            host.setStringValue(device);
            udpRequest.setOption(host);

            if (udpRequest.getCode() == CodeRegistry.METHOD_GET && !udpRequest.hasOption(OptionNumberRegistry.OBSERVE)) {
                final Cache pair = CacheHandler.getInstance().getValue(device, uriPath.toString());
                if (System.currentTimeMillis() - pair.getTimestamp() > pair.getMaxAge() * 1000) {
                    return udpRequest;
                } else {
                    response.setContentType(pair.getContentType());
//                    payload.append("CACHE - ").append(new Date(pair.getTimestamp())).append(" - ").append(pair.getValue());
                    payload.append(pair.getValue());
                    final Option etag = new Option(OptionNumberRegistry.ETAG);
                    etag.setIntValue((int) (System.currentTimeMillis() - pair.getTimestamp()));
                    response.setOption(etag);
                    response.setMaxAge(pair.getMaxAge());
                }
            } else {
                return udpRequest;
            }
            response.setPayload(payload.toString());
        } else if ("/.well-known/core".equals(path)) {
            if (udpRequest.getCode() == CodeRegistry.METHOD_GET) {
                StringBuilder payload = new StringBuilder("");
                for (String handlerName : internalRequestHandlers.keySet()) {
                    payload.append(",").append("<").append(handlerName.substring(1)).append(">");
                }
                Map<String, Map<String, Long>> endpoints = CoapServer.getInstance().getEndpoints();
                for (String endpoint : endpoints.keySet()) {
                    for (String resource : endpoints.get(endpoint).keySet()) {
                        if (".well-known/core".equals(resource)) {
                            payload.append(",<device/").append(endpoint).append(">");
                        } else {
                            payload.append(",<device/").append(endpoint).append("/").append(resource).append(">");
                        }
                    }
                }
                payload.append("\n");
                response.setContentType(MediaTypeRegistry.APPLICATION_LINK_FORMAT);
                response.setPayload(payload.toString().substring(1));
            } else {
                response.setCode(CodeRegistry.RESP_METHOD_NOT_ALLOWED);
            }
        } else {
            if (internalRequestHandlers.containsKey(path)) {
                InternalRequestHandler handler = internalRequestHandlers.get(path);
                handler.handle(udpRequest, response);
            } else {
                response.setCode(CodeRegistry.RESP_NOT_FOUND); //not found
            }
        }
        CoapServer.getInstance().sendReply(response.toByteArray(), socketAddress);
        return null;
    }
}


