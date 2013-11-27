package eu.uberdust.testbedlistener.coap.internal.handler;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Message;
import eu.uberdust.testbedlistener.coap.CacheHandler;
import eu.uberdust.testbedlistener.coap.CoapServer;
import eu.uberdust.testbedlistener.coap.PendingRequestHandler;
import org.apache.commons.collections.map.ListOrderedMap;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Internal Request Handler used to display information related to the CoAP Server.
 *
 * @author Dimitrios Amaxilatis
 * @date 6/13/13
 */
public class StatusRequestHandler implements InternalRequestHandlerInterface {

    @Override
    public void handle(Message udpRequest, Message response) {
        if (udpRequest.getCode() == CodeRegistry.METHOD_GET) {
            Map<String, String> statusElements = new ListOrderedMap();
            statusElements.put("Status", "Online");

            final long elapsed = System.currentTimeMillis() - CoapServer.getInstance().getStartTime();
            final String elapsedString = String.format("%d days, %d hours, %d min, %d sec",
                    TimeUnit.MILLISECONDS.toDays(elapsed),
                    TimeUnit.MILLISECONDS.toHours(elapsed) - TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(elapsed)),
                    TimeUnit.MILLISECONDS.toMinutes(elapsed) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(elapsed)),
                    TimeUnit.MILLISECONDS.toSeconds(elapsed) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapsed))
            );
            statusElements.put("Uptime", elapsedString);
            statusElements.put("Running Threads", String.valueOf(Thread.activeCount()));
            statusElements.put("Cache Size", CacheHandler.getInstance().getCache().keySet().size() + " nodes");
            statusElements.put("Pending", String.valueOf(PendingRequestHandler.getInstance().getPendingRequestList().size()));


            final Properties prop = new Properties();

            try {
                final int mb = 1024 * 1024;
                //Getting the runtime reference from system
                final Runtime runtime = Runtime.getRuntime();
                //Print used memory
                statusElements.put("Used Memory", ((runtime.totalMemory() - runtime.freeMemory()) / mb) + " MB");
                //Print free memory
                statusElements.put("Free Memory", (runtime.freeMemory() / mb) + " MB");
                //Print total available memory
                statusElements.put("Total Memory", (runtime.totalMemory() / mb) + " MB");
                //Print Maximum available memory
                statusElements.put("Max Memory", (runtime.maxMemory() / mb) + " MB");
                prop.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("version.properties"));
                statusElements.put("Version", String.valueOf(prop.get("version")));
                statusElements.put("Build", String.valueOf(prop.get("build")));
            } catch (Exception e) {
                e.printStackTrace();
            }

            StringBuilder payload = new StringBuilder("");
            for (String key : statusElements.keySet()) {
                payload.append(key).append(":").append("\t").append(statusElements.get(key)).append("\n");
            }

            payload.append("====================================================================\n");

            ThreadGroup currentGroup =
                    Thread.currentThread().getThreadGroup();
            int noThreads = currentGroup.activeCount();
            Thread[] lstThreads = new Thread[noThreads];
            currentGroup.enumerate(lstThreads);
            for (int i = 0; i < noThreads; i++) {
                payload.append("Thread No:").append(i).append(" = ").append(lstThreads[i].getName()).append("\n");
            }

            response.setContentType(MediaTypeRegistry.TEXT_PLAIN);
            response.setPayload(payload.toString());
        } else {
            response.setCode(CodeRegistry.RESP_METHOD_NOT_ALLOWED);
        }
    }
}
