package eu.uberdust.testbedlistener.coap;

import org.apache.commons.collections.map.ListOrderedMap;
import org.restlet.data.MediaType;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Created by amaxilatis on 4/22/14.
 */
public class Presenter extends ServerResource {
    /**
     * LOGGER.
     */
    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(Presenter.class);

    @Get
    public StringRepresentation handler() throws IOException {

        final String path = URI.create(getRequest().getResourceRef().toString()).getPath().substring(1);
        LOGGER.info(path);

        if ("status".equals(path)) {

            return handleStatus();
        } else {
            return handleCache();
        }
//        return new StringRepresentation("not found");
    }

    private StringRepresentation handleCache() {
        StringBuilder payload = new StringBuilder("");
        final Map<String, CacheEntry> cache = ResourceCache.getInstance().getCache();
        payload.append("" +
                "<html>" +
                "   <head>" +
                "       <link href=\"//netdna.bootstrapcdn.com/bootstrap/3.1.1/css/bootstrap.min.css\" rel=\"stylesheet\">" +
                "       <script type=\"text/javascript\" language=\"javascript\" src=\"https://code.jquery.com/jquery-1.11.0.min.js\"></script>" +
                "       <script type=\"text/javascript\" language=\"javascript\" src=\"https://datatables.net/release-datatables/media/js/jquery.dataTables.js\"></script>" +
                "       <script src=\"//netdna.bootstrapcdn.com/bootstrap/3.1.1/js/bootstrap.min.js\"></script>" +
                "       <script type=\"text/javascript\" charset=\"utf-8\">\n" +
                "           $(document).ready(function() {\n" +
                "               $('#table').dataTable();\n" +
                "           } );" +
                "       </script>" +
                "   </head>" +
                "   <body>" +
                "   <div style='width:80%;text-align: center;'>" +
                "       <table id='table' style='text-align: right;width: 100%;'>" +
                "           <thead>" +
                "               <tr>" +
                "                   <td>Host<td>Value<td>Timestamp<td>Age<td>Observes lost" +
                "               </tr>" +
                "           </thead>" +
                "           <tbody>");

        String uriHost = "";
        final Pattern pattern = Pattern.compile(uriHost);
        for (final String resourceURIString : cache.keySet()) {

            if (!"".equals(uriHost) && !(pattern.matcher(resourceURIString).find())) {
                continue;
            }

            final CacheEntry pair = cache.get(resourceURIString);
            final long timeDiff = (System.currentTimeMillis() - pair.getTimestamp()) / 1000;
            payload.append("" +
                    "           <tr>")
                    .append("<td>").append(resourceURIString)
                    .append("<td>").append(pair.getValue())
                    .append("<td>").append(new Date(pair.getTimestamp()))
                    .append("<td>").append(timeDiff).append("sec").append(timeDiff > pair.getMaxAge() ? " *" : "")
                    .append("<td>").append(pair.getLostCounter())
                    .append("   </tr>");

        }
        payload.append("" +
                "           </tbody>" +
                "       </table>" +
                "   </div>" +
                "   </body>");
        return new StringRepresentation(payload.toString(), MediaType.TEXT_HTML);

    }


    private StringRepresentation handleStatus() {

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
        statusElements.put("CacheEntry Size", ResourceCache.getInstance().getCache().keySet().size() + " nodes");
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
        return new StringRepresentation(payload.toString());

    }
}
