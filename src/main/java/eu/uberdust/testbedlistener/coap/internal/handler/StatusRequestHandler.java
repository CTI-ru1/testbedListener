package eu.uberdust.testbedlistener.coap.internal.handler;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Message;
import eu.uberdust.testbedlistener.coap.CacheHandler;
import eu.uberdust.testbedlistener.coap.CoapServer;
import eu.uberdust.testbedlistener.coap.PendingRequestHandler;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: amaxilatis
 * Date: 6/13/13
 * Time: 3:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class StatusRequestHandler implements InternalRequestHandlerInterface {
    @Override
    public void handle(Message udpRequest, Message response) {
        if (udpRequest.getCode() == CodeRegistry.METHOD_GET) {
            StringBuilder payload = new StringBuilder("");
            payload.append("Online").append("\n");
            final Properties prop = new Properties();
            try {
                final long elapsed = System.currentTimeMillis() - CoapServer.getInstance().getStartTime();
                final String elapsedString = String.format("%d days, %d hours, %d min, %d sec",
                        TimeUnit.MILLISECONDS.toDays(elapsed),
                        TimeUnit.MILLISECONDS.toHours(elapsed) - TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(elapsed)),
                        TimeUnit.MILLISECONDS.toMinutes(elapsed) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(elapsed)),
                        TimeUnit.MILLISECONDS.toSeconds(elapsed) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapsed))
                );
                payload.append("Uptime:").append(elapsedString).append("\n");
                payload.append("Running Threads:").append(Thread.activeCount()).append("\n");
                payload.append("Cache Size:").append(CacheHandler.getInstance().getCache().keySet().size()).append(" nodes").append("\n");
                payload.append("Pending Connections:").append(PendingRequestHandler.getInstance().getPendingRequestList().size()).append("\n");
                final int mb = 1024 * 1024;
                //Getting the runtime reference from system
                final Runtime runtime = Runtime.getRuntime();
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
            response.setPayload(payload.toString());
        } else {
            response.setCode(CodeRegistry.RESP_METHOD_NOT_ALLOWED);
        }
    }
}
