package eu.uberdust.testbedlistener.coap;

import ch.ethz.inf.vs.californium.coap.*;
import eu.uberdust.testbedlistener.datacollector.parsers.CoapMessageParser;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

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

    /**
     * Empty Constructor.
     */
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

    public Message handleRequest(String uriHost, Message udpRequest, SocketAddress socketAddress) {
        //To change body of created methods use File | Settings | File Templates.
        Message response = new Message();
        StringBuilder payload = new StringBuilder();
        if (udpRequest.isConfirmable()) {
            response.setType(Message.messageType.ACK);
        } else if (udpRequest.isNonConfirmable()) {
            response.setType(Message.messageType.NON);
        }
        response.setCode(CodeRegistry.RESP_CONTENT);
        response.setMID(udpRequest.getMID());

        String path = udpRequest.getUriPath();
        if (!"".equals(uriHost) && !"/cache".equals(path)) {
            return udpRequest;
        }
        if (path.contains("/device/")) {
            //forward to device or respond from cache

            String[] temp = path.split("/device/");
            temp = temp[1].split("/");
            String device = temp[0];
            final StringBuilder uriPath = new StringBuilder();
            for (int i = 1; i < temp.length; i++) {
                uriPath.append("/").append(temp[i]);
            }
            udpRequest.setURI(uriPath.toString());
            Option host = new Option(OptionNumberRegistry.URI_HOST);
            host.setStringValue(device);
            udpRequest.setOption(host);

            if (udpRequest.getCode() == CodeRegistry.METHOD_GET && !udpRequest.hasOption(OptionNumberRegistry.OBSERVE)) {
                Cache pair = CacheHandler.getInstance().getValue(device, uriPath.toString());
                if (System.currentTimeMillis() - pair.getTimestamp() > pair.getMaxAge()*1000) {
                    return udpRequest;
                } else {
                    response.setContentType(pair.getContentType());
//                    payload.append("CACHE - ").append(new Date(pair.getTimestamp())).append(" - ").append(pair.getValue());
                    payload.append(pair.getValue());
                    Option etag = new Option(OptionNumberRegistry.ETAG);
                    etag.setIntValue((int) (System.currentTimeMillis() - pair.getTimestamp()));
                    response.setOption(etag);
                    response.setMaxAge(pair.getMaxAge());
                }
            } else {
                return udpRequest;
            }
        } else if ("/.well-known/core".equals(path)) {
            if (udpRequest.getCode() == CodeRegistry.METHOD_GET) {
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
                response.setContentType(MediaTypeRegistry.APPLICATION_LINK_FORMAT);
            } else {
                response.setCode(CodeRegistry.RESP_METHOD_NOT_ALLOWED);
            }
        } else if ("/status".equals(path)) {
            if (udpRequest.getCode() == CodeRegistry.METHOD_GET) {
                payload.append("Online").append("\n");
                Properties prop = new Properties();
                try {
                    long elapsed = System.currentTimeMillis() - CoapServer.getInstance().getStartTime();
                    String elapsedString = String.format("%d days, %d hours, %d min, %d sec",
                            TimeUnit.MILLISECONDS.toDays(elapsed),
                            TimeUnit.MILLISECONDS.toHours(elapsed) - TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(elapsed)),
                            TimeUnit.MILLISECONDS.toMinutes(elapsed) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(elapsed)),
                            TimeUnit.MILLISECONDS.toSeconds(elapsed) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapsed))
                    );
                    payload.append("Uptime:").append(elapsedString).append("\n");
                    payload.append("Running Threads:").append(Thread.activeCount()).append("\n");
                    payload.append("Cache Size:").append(CacheHandler.getInstance().getCache().keySet().size()).append(" nodes").append("\n");
                    payload.append("Pending Connections:").append(PendingRequestHandler.getInstance().getPendingRequestList().size()).append("\n");
                    int mb = 1024 * 1024;
                    //Getting the runtime reference from system
                    Runtime runtime = Runtime.getRuntime();
                    //Print used memory
                    payload.append("Used Memory:").append((runtime.totalMemory() - runtime.freeMemory()) / mb).append(" MB").append("\n");
                    //Print free memory
                    payload.append("Free Memory:").append(runtime.freeMemory() / mb).append(" MB").append("\n");
                    //Print total available memory
                    payload.append("Total Memory:").append(runtime.totalMemory() / mb).append(" MB").append("\n");
                    //Print Maximum available memory
                    payload.append("Max Memory:").append(runtime.maxMemory() / mb).append(" MB").append("\n");
                    prop.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("version.properties"));
                    payload.append("Version:").append(prop.get("version")).append("\n");
                    payload.append("Build:").append(prop.get("build")).append("\n");

                    payload.append("\n").append("****").append("\n");
                    payload.append(".well-known/core requests: ").append(CoapServer.getInstance().getRequestWellKnownCounter()).append("\n");
                    payload.append("Observe Requests:").append(CoapServer.getInstance().getRequestObserveCounter()).append("\n");
                    payload.append("Observe Responses:").append(CoapServer.getInstance().getResponseObserveCounter()).append("\n");
                    payload.append("Observe Lost:").append(CoapServer.getInstance().getObserveLostCounter()).append("\n");

                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
                response.setContentType(MediaTypeRegistry.TEXT_PLAIN);
            } else {
                response.setCode(CodeRegistry.RESP_METHOD_NOT_ALLOWED);
            }
        } else if ("/endpoints".equals(path)) {
            if (udpRequest.getCode() == CodeRegistry.METHOD_GET) {
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
                response.setContentType(MediaTypeRegistry.TEXT_PLAIN);
            } else {
                response.setCode(CodeRegistry.RESP_METHOD_NOT_ALLOWED);
            }
        } else if ("/activeRequests".equals(path)) {
            if (udpRequest.getCode() == CodeRegistry.METHOD_GET) {
                Map<Integer, ActiveRequest> activeRequests = CoapServer.getInstance().getActiveRequestsMID();
                for (int key : activeRequests.keySet()) {
                    ActiveRequest activeRequest = activeRequests.get(key);
                    payload.append(activeRequest.getHost()).append("\t").append(activeRequest.getToken()).append("\t").append(activeRequest.getMid()).append("\t").append(activeRequest.getUriPath()).append("\t").append(activeRequest.getTimestamp()).append("\t").append(activeRequest.getMid()).append("\t").append(activeRequest.getCount()).append("\n");

                }
                payload.append("\n");
                Map<String, ActiveRequest> activeRequests2 = CoapServer.getInstance().getActiveRequestsTOKEN();
                for (String key : activeRequests2.keySet()) {
                    ActiveRequest activeRequest = activeRequests2.get(key);
                    payload.append(activeRequest.getHost()).append("\t").append(activeRequest.getToken()).append("\t").append(activeRequest.getMid()).append("\t").append(activeRequest.getUriPath()).append("\t").append(activeRequest.getTimestamp()).append("\t").append(activeRequest.getMid()).append("\t").append(activeRequest.getCount()).append("\n");

                }
                response.setContentType(MediaTypeRegistry.TEXT_PLAIN);
            } else {
                response.setCode(CodeRegistry.RESP_METHOD_NOT_ALLOWED);
            }
        } else if ("/pendingRequests".equals(path)) {
            if (udpRequest.getCode() == CodeRegistry.METHOD_GET) {
                List<PendingRequest> pendingRequests = PendingRequestHandler.getInstance().getPendingRequestList();
                for (PendingRequest pendingRequest : pendingRequests) {
                    payload.append(pendingRequest.getUriHost());
                    if (pendingRequest.getUriHost().length() == 3) {
                        payload.append(" ");
                    }
                    payload.append(" - ");
                    payload.append(pendingRequest.getSocketAddress()).append(" - ");
                    payload.append(pendingRequest.getMid()).append(" - ");
                    payload.append(pendingRequest.getToken()).append("\n");
                }
                response.setContentType(MediaTypeRegistry.TEXT_PLAIN);
            } else {
                response.setCode(CodeRegistry.RESP_METHOD_NOT_ALLOWED);
            }
        } else if ("/cache".equals(path)) {
            if (udpRequest.getCode() == CodeRegistry.METHOD_GET) {
                Map<String, Map<String, Cache>> cache = CacheHandler.getInstance().getCache();
                payload.append("Host\tPath\tValue\tTimestamp\t\t\tAge\tObserves lost\n");
                for (String device : cache.keySet()) {
                    if (!"".equals(uriHost) && !device.equals(uriHost)) {
                        continue;
                    }
                    for (String uriPath : cache.get(device).keySet()) {
                        Cache pair = cache.get(device).get(uriPath);
                        long timediff = (System.currentTimeMillis() - pair.getTimestamp()) / 1000;
                        payload.append(device).append("\t").append(uriPath).append("\t").append(pair.getValue()).append("\t").append(new Date(pair.getTimestamp())).append("\t").append(timediff).append("sec").append(timediff > pair.getMaxAge() ? " *" : "").append("\t").append(pair.getLostCounter()).append("\n");
                    }
                }
                response.setContentType(MediaTypeRegistry.TEXT_PLAIN);
            } else if (udpRequest.getCode() == CodeRegistry.METHOD_POST) {
                CacheHandler.getInstance().clearCache();
                payload.append("Cache cleared");
                response.setContentType(MediaTypeRegistry.TEXT_PLAIN);
                response.setCode(CodeRegistry.RESP_CHANGED);
            } else {
                response.setCode(CodeRegistry.RESP_METHOD_NOT_ALLOWED);
            }
        } else if ("/wakeup".equals(path) && udpRequest.getCode() == 2) {
            if (udpRequest.getCode() == CodeRegistry.METHOD_POST) {
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
                response.setContentType(MediaTypeRegistry.TEXT_PLAIN);
            } else {
                response.setCode(CodeRegistry.RESP_METHOD_NOT_ALLOWED);
            }
        } else {
            response.setCode(CodeRegistry.RESP_NOT_FOUND); //not found
        }
        response.setPayload(payload.toString());
        CoapServer.getInstance().sendReply(response.toByteArray(), socketAddress);
        return null;
    }
}
