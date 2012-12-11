package eu.uberdust.testbedlistener.coap;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import eu.uberdust.testbedlistener.util.PropertyReader;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.MalformedURLException;
import java.net.SocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: amaxilatis
 * Date: 10/30/12
 * Time: 11:56 AM
 * To change this template use File | Settings | File Templates.
 */
public class GatewayManager {
    /**
     * LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(GatewayManager.class);
    private Map<String, String> gateways;

    /**
     * the only instance of PendingRequestHandler.
     */
    private static GatewayManager instance = null;

    /**
     * Constructor.
     */
    public GatewayManager() {
        gateways = new HashMap<String, String>();
        loadGatewaysFromUberdust();
        loadGatewaysFromFile();

    }

    /**
     * Singleton getInstance.
     *
     * @return the only instance of GatewayManager.
     */
    public static GatewayManager getInstance() {
        synchronized (GatewayManager.class) {
            if (instance == null) {
                instance = new GatewayManager();
            }
            return instance;
        }
    }
    private void loadGatewaysFromUberdust() {
        try {
            URL url = new URL(new StringBuilder()
                    .append("http://")
                    .append(PropertyReader.getInstance().getProperties().get("uberdust.server"))
                    .append(PropertyReader.getInstance().getProperties().get("uberdust.basepath"))
                    .append("/rest/testbed/")
                    .append(PropertyReader.getInstance().getProperties().get("wisedb.testbedid"))
                    .append("/capability/gateway/tabdelimited").toString());
            final URLConnection con = url.openConnection();
            final InputStream out = con.getInputStream();
            final StringBuilder response = new StringBuilder();
            int str = out.read();
            while (str != -1) {
                response.append((char) str);
                str = out.read();
            }

            out.close();
            final String[] splitted = response.toString().split("\n");
            for (final String row : splitted) {
                String urn = row.split("\t")[0];
                urn = urn.substring(urn.lastIndexOf(":0x") + 3);
                final String value = row.split("\t")[3];
                if ("1.0".equals(value)) {
                    gateways.put(urn, urn);
                } else if (value.contains("0x")) {
                    gateways.put(urn, value.split("0x")[1]);
                }
            }
        } catch (MalformedURLException e) {
            LOGGER.error(e, e);
        } catch (IOException e) {
            LOGGER.error(e, e);
        }
    }
    private void loadGatewaysFromFile() {
        BufferedReader bin = null;
        try {
            bin = new BufferedReader(new FileReader("gateways"));
            String str = bin.readLine();
            while (str != null) {
                if (str.contains(",")) {
                    final String source = str.split(",")[0].substring(str.split(",")[0].lastIndexOf(":0x") + 3);
                    final String from = str.split(",")[1].substring(str.split(",")[1].lastIndexOf(":0x") + 3);
                    gateways.put(source, from);

                } else {
                    if (str.contains(":")) {
                        str = str.substring(str.lastIndexOf(":0x") + 3);
                    }
                    gateways.put(str, str);
                }
                LOGGER.info("adding " + str);
                str = bin.readLine();
            }
        } catch (FileNotFoundException e) {
            LOGGER.error(e, e);
        } catch (IOException e) {
            LOGGER.error(e, e);
        }
    }

    public boolean hasGateway(final String destination) {
        return gateways.containsKey(destination);
    }

    public String getGateway(final String destination) {
        return gateways.get(destination);
    }
}
