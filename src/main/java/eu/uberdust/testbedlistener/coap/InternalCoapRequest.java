package eu.uberdust.testbedlistener.coap;

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

            payload.append(udpRequest.getOptionCount()).append(" - ");


            String[] temp = path.split("/device/");
            temp = temp[1].split("/");
            String device = temp[0];
            String uriPath = temp[1];
            udpRequest.setURI(uriPath);
            Option host = new Option(OptionNumberRegistry.URI_HOST);
            host.setStringValue(device);
            udpRequest.setOption(host);
//            payload.append(udpRequest.getOptionCount()).append(" - ");
//            payload.append(udpRequest.getUriPath()).append(" - ");
//            payload.append(host.getStringValue()).append("\n");
//            response.setContentType(0);
//            response.setURI(uriPath);
//            //response.setOption(host);
            return udpRequest;
        } else if ("/.well-known/core".equals(path)) {
            payload.append("<status>,<endpoints>,<activeRequests>,<pendingRequests>,<observers>");
            Map<String, Map<String, Long>> endpoints = CoapServer.getInstance().getEndpoints();
            for (String endpoint : endpoints.keySet()) {
                payload.append(",<device/").append(endpoint).append(">");
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
        } else if ("/observers".equals(path)) {
            ArrayList<CoapServer.TokenItem> observers = CoapServer.getInstance().getObservers();
            for (CoapServer.TokenItem observer : observers) {
                payload.append(observer).append("\n");
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
