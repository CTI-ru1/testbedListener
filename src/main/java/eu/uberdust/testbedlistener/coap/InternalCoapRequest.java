package eu.uberdust.testbedlistener.coap;

import ch.ethz.inf.vs.californium.coap.Message;
import eu.uberdust.testbedlistener.coap.udp.UDPhandler;

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

    public InternalCoapRequest() {}

    public static InternalCoapRequest getInstance() {
        synchronized (InternalCoapRequest.class) {
            if (instance == null) {
                instance = new InternalCoapRequest();
            }
            return instance;
        }
    }

    public void handleRequest(Message udpRequest, SocketAddress socketAddress) {
        //To change body of created methods use File | Settings | File Templates.
        Message response = new Message();
        StringBuilder payload = new StringBuilder();
        if (udpRequest.isConfirmable()){
            response.setType(Message.messageType.ACK);
        }
        else if (udpRequest.isNonConfirmable()){
            response.setType(Message.messageType.NON);
        }
        response.setCode(69); // content
        response.setMID(udpRequest.getMID());

        String path = udpRequest.getUriPath();
        if ("/.well-known/core".equals(path)) {
            payload.append("<status>,<endpoints>,<activeRequests>,<observers>\n");
            response.setContentType(40);
        }
        else if ("/status".equals(path)){
            payload.append("Working");
            response.setContentType(0);
        }
        else if ("/endpoints".equals(path)) {
            Map<String, Long> endpoints = CoapServer.getInstance().getEndpoints();
            for (String endpoint : endpoints.keySet()) {
                payload.append(endpoint);
                if (endpoint.length() == 3)
                    payload.append(" ");
                payload.append(" - ");
                payload.append(new Date(endpoints.get(endpoint)));
                payload.append("\n");
            }
            response.setContentType(0);
        }
        else if("/activeRequests".equals(path)) {
            List<ActiveRequest> activeRequests = CoapServer.getInstance().getActiveRequests();
            for (ActiveRequest activeRequest : activeRequests) {
                payload.append(activeRequest.getHost());
                if (activeRequest.getHost().length() == 3)
                    payload.append(" ");
                payload.append(" - ");
                payload.append(activeRequest.getSocketAddress()).append(" - ");
                payload.append(activeRequest.getUriPath()).append(" - ");
                payload.append(activeRequest.getMid()).append("\n")   ;

            }
            response.setContentType(0);
        }
        else if("/observers".equals(path)) {
            ArrayList<CoapServer.TokenItem> observers = CoapServer.getInstance().getObservers();
            for (CoapServer.TokenItem observer : observers) {
                payload.append(observer).append("\n");
            }
            response.setContentType(0);
        }
        else {
            response.setCode(132); //not found
        }
        response.setPayload(payload.toString());
        CoapServer.getInstance().sendReply(response.toByteArray(), socketAddress);
    }
}
