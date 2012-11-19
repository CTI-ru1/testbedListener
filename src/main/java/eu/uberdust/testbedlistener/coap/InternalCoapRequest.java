package eu.uberdust.testbedlistener.coap;

import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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

    public InternalCoapRequest() {
    }

    public static InternalCoapRequest getInstance() {
        synchronized (InternalCoapRequest.class) {
            if (instance == null) {
                instance = new InternalCoapRequest();
            }
            return instance;
        }
    }

    public Message handleRequest(Message udpRequest, SocketAddress socketAddress) {
        //To change body of created methods use File | Settings | File Templates.
        Message response = new Message();
        StringBuilder payload = new StringBuilder();
        if (udpRequest.isConfirmable()) {
            response.setType(Message.messageType.ACK);
        } else if (udpRequest.isNonConfirmable()) {
            response.setType(Message.messageType.NON);
        }
        response.setCode(69); // content
        response.setMID(udpRequest.getMID());

        String path = udpRequest.getUriPath();
        if (path.contains("/device/")) {
            //forward to device or respond from cache

            String[] temp = path.split("/device/");
            temp = temp[1].split("/");
            String device = temp[0];
            StringBuilder uriPath = new StringBuilder();
            for(int i=1; i<temp.length; i++) {
                uriPath.append("/").append(temp[i]);
            }
            //String uriPath = temp[1];
            udpRequest.setURI(uriPath.toString());
            Option host = new Option(OptionNumberRegistry.URI_HOST);
            host.setStringValue(device);
            udpRequest.setOption(host);
            
            if (udpRequest.getCode() == 1) {
                Cache pair = CacheHandler.getInstance().getValue(device, uriPath.toString());
                if(pair == null) {
                    return udpRequest;
                }
                else {
                    response.setContentType(0);
                    payload.append("CACHE - ").append(new Date(pair.getTimestamp())).append(" - ").append(pair.getValue());
                }
            }
            else {
                return udpRequest;
            }
        } else if ("/.well-known/core".equals(path)) {
            payload.append("<status>,<endpoints>,<activeRequests>,<pendingRequests>,<cache>");
            Map<String, Map<String, Long>> endpoints = CoapServer.getInstance().getEndpoints();
            for (String endpoint : endpoints.keySet()) {
                for (String resource : endpoints.get(endpoint).keySet()) {
                    if (".well-known/core".equals(resource)) {
                        payload.append(",<device/").append(endpoint).append(">");
                    }
                    else {
                        payload.append(",<device/").append(endpoint).append("/").append(resource).append(">");
                    }
                }
            }
            payload.append("\n");
            response.setContentType(40);
        } else if ("/status".equals(path)) {
            payload.append("Working");
            response.setContentType(0);
        } else if ("/endpoints".equals(path)) {
            Map<String, Map<String, Long>> endpoints = CoapServer.getInstance().getEndpoints();
            for (String endpoint : endpoints.keySet()) {
                for (String uripath : endpoints.get(endpoint).keySet()) {
                    payload.append(new Date(endpoints.get(endpoint).get(uripath)));
                    payload.append(" - ");
                    if (endpoint.length() == 3) {
                        payload.append(" ");
                    }
                    payload.append(endpoint);
                    payload.append("/" + uripath);
                    payload.append("\n");
                }
            }
            response.setContentType(0);
        } else if ("/activeRequests".equals(path)) {
            List<ActiveRequest> activeRequests = CoapServer.getInstance().getActiveRequests();
            for (ActiveRequest activeRequest : activeRequests) {
                if (activeRequest.getHost().length() == 3) {
                    payload.append(" ");
                }
                payload.append(
                        String.format("%d- %s%s",
                                activeRequest.getMid(),
                                activeRequest.getHost(),
                                activeRequest.getUriPath())
                ).append("\n");

            }
            response.setContentType(0);
        } else if ("/pendingRequests".equals(path)) {
            List<PendingRequest> pendingRequests = PendingRequestHandler.getInstance().getPendingRequestList();
            for (PendingRequest pendingRequest : pendingRequests) {
                payload.append(pendingRequest.getUriHost());
                if (pendingRequest.getUriHost().length() == 3)
                    payload.append(" ");
                payload.append(" - ");
                payload.append(pendingRequest.getSocketAddress()).append(" - ");
                payload.append(pendingRequest.getMid()).append(" - ");
                payload.append(pendingRequest.getToken()).append("\n");
            }
        } else if ("/cache".equals(path)) {
            Map<String, Map<String, Cache>> cache = CacheHandler.getInstance().getCache();
            for (String device : cache.keySet()) {
                for (String uriPath : cache.get(device).keySet()) {
                    Cache pair = cache.get(device).get(uriPath);
                    payload.append(device).append("\t").append(uriPath).append("\t").append(pair.getValue()).append("\t").append(new Date(pair.getTimestamp())).append("\n");
                }
            }
            response.setContentType(0);
        } else {
            response.setCode(132); //not found
        }
        response.setPayload(payload.toString());
        CoapServer.getInstance().sendReply(response.toByteArray(), socketAddress);
        return null;
    }
}
