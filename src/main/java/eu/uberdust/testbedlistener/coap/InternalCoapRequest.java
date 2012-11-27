package eu.uberdust.testbedlistener.coap;

import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import eu.uberdust.testbedlistener.datacollector.parsers.CoapMessageParser;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: ven0m
 * Date: 11/16/12
 * Time: 3:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class InternalCoapRequest {

    private static final Logger LOGGER = Logger.getLogger(InternalCoapRequest.class);

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
            for (int i = 1; i < temp.length; i++) {
                uriPath.append("/").append(temp[i]);
            }
            //String uriPath = temp[1];
            udpRequest.setURI(uriPath.toString());
            Option host = new Option(OptionNumberRegistry.URI_HOST);
            host.setStringValue(device);
            udpRequest.setOption(host);

            if (udpRequest.getCode() == 1 && !udpRequest.hasOption(OptionNumberRegistry.OBSERVE)) {
                Cache pair = CacheHandler.getInstance().getValue(device, uriPath.toString());
                if (pair == null) {
                    return udpRequest;
                } else {
                    response.setContentType(pair.getContentType());
//                    payload.append("CACHE - ").append(new Date(pair.getTimestamp())).append(" - ").append(pair.getValue());
                    payload.append(pair.getValue());
                    Option etag = new Option(OptionNumberRegistry.ETAG);
                    etag.setIntValue((int) (System.currentTimeMillis() - pair.getTimestamp()));
                    response.setOption(etag);
                }
            } else {
                return udpRequest;
            }
        } else if ("/.well-known/core".equals(path)) {
            payload.append("<status>,<endpoints>,<activeRequests>,<pendingRequests>,<cache>,<wakeup>");
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
            response.setContentType(40);
        } else if ("/status".equals(path)) {
            payload.append("Online").append("\n");
            Properties prop = new Properties();
            try {
                prop.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("version.properties"));
                payload.append("Version:").append(prop.get("version")).append("\n");
                payload.append("Build:").append(prop.get("build")).append("\n");
                payload.append("Running Threads:" + Thread.activeCount()).append("\n");
                payload.append("Cache Size:" + CacheHandler.getInstance().getCache().keySet().size() + " nodes").append("\n");
                payload.append("Pending Connections:" + PendingRequestHandler.getInstance().getPendingRequestList().size()).append("\n");
                int mb = 1024 * 1024;
                //Getting the runtime reference from system
                Runtime runtime = Runtime.getRuntime();
                //Print used memory
                payload.append("Used Memory:").append((runtime.totalMemory() - runtime.freeMemory()) / mb).append(" MB").append("\n");
                //Print free memory
                payload.append("Free Memory:" + runtime.freeMemory() / mb).append(" MB").append("\n");
                //Print total available memory
                payload.append("Total Memory:" + runtime.totalMemory() / mb).append(" MB").append("\n");
                //Print Maximum available memory
                payload.append("Max Memory:" + runtime.maxMemory() / mb).append(" MB").append("\n");

            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
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
                    payload.append("/").append(uripath);
                    payload.append("\n");
                }
            }
            response.setContentType(0);
        } else if ("/activeRequests".equals(path)) {
//            CoapServer.getInstance().cleanActiveRequests();
            List<ActiveRequest> activeRequests = CoapServer.getInstance().getActiveRequests();
            for (ActiveRequest activeRequest : activeRequests) {
                payload.append(activeRequest.getHost()).append("\t").append(activeRequest.getToken()).append("\t").append(activeRequest.getMid()).append("\t").append(activeRequest.getUriPath()).append("\n");

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
                    boolean stale;
                    if (System.currentTimeMillis() - pair.getTimestamp() > pair.getMaxAge() * 1000) {
                        stale = true;
                    } else {
                        stale = false;
                    }
//                    payload.append(device).append("\t").append(uriPath).append("\t").append(pair.getValue()).append("\t").append(new Date(pair.getTimestamp())).append("\t").append(pair.getMaxAge()).append("-").append(stale?"out-of-date":"cached").append("\n");
                    payload.append(device).append("\t").append(uriPath).append("\t").append(pair.getValue()).append("\t").append(new Date(pair.getTimestamp())).append("\t").append((System.currentTimeMillis() - pair.getTimestamp()) / 1000).append("sec").append("\n");

                }
            }
            response.setContentType(0);
        } else if ("/wakeup".equals(path) && udpRequest.getCode() == 2) {
            if (udpRequest.getCode() != 2) {
                response.setCode(133);
            } else {
                String device = udpRequest.getPayloadString();
                if (device.length() == 3) {
                    device = "0" + device;
                }
                byte[] data = new byte[11];
                int pos = 0;
                data[pos++] = 0x69;
                data[pos++] = 102; // w/e
                for (int i = 0; i < 4; i += 2) {
                    data[pos++] = (byte) ((Character.digit(device.charAt(i), 16) << 4) + Character.digit(device.charAt(i + 1), 16));
                }
                data[pos++] = 0x68;
                data[pos++] = 0x65;
                data[pos++] = 0x72;
                data[pos++] = 0x65;
                data[pos++] = 0x69;
                data[pos++] = 0x61;
                data[pos] = 0x6D;

                Thread parser = new Thread(new CoapMessageParser("0x" + udpRequest.getPayloadString(), data));
                parser.start();

                payload.append("Here I am simulation on ").append(udpRequest.getPayloadString());
                response.setContentType(0);
            }
        } else {
            response.setCode(132); //not found
        }
        response.setPayload(payload.toString());
        CoapServer.getInstance().sendReply(response.toByteArray(), socketAddress);
        return null;
    }
}
