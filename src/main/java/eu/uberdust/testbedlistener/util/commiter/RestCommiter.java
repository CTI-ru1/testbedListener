package eu.uberdust.testbedlistener.util.commiter;

import eu.uberdust.communication.protobuf.Message;
import eu.uberdust.testbedlistener.util.PropertyReader;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by IntelliJ IDEA.
 * User: amaxilatis
 * Date: 12/4/11
 * Time: 2:15 PM
 */
public class RestCommiter {

    /**
     * LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(RestCommiter.class);

    /**
     * The base url of the uberdust server.
     */
    private static final String TESTBED_SERVER = "http://uberdust.cti.gr/rest";

    /**
     * Add a new nodeReading using the rest interfaces.
     *
     * @param nodeReading the node reading to add
     */
    public RestCommiter(final Message.NodeReadings.Reading nodeReading) {
        final StringBuilder urlBuilder = new StringBuilder(TESTBED_SERVER);
        urlBuilder.append('/').append(PropertyReader.getInstance().getTestbedId())
                .append("/node/").append(nodeReading.getNode())
                .append("/capability/").append(nodeReading.getCapability())
                .append("/insert/timestamp/").append(nodeReading.getTimestamp());

        if (nodeReading.hasDoubleReading()) {
            //uberdust.cti.gr/rest/testbed/1/node/urn:wisebed:ctitestbed:virtual:0.I.9/capability/light1/insert/timestamp/1/reading/1/
            urlBuilder.append("/reading/").append(nodeReading.getDoubleReading())
                    .append("/");
            callUrl(urlBuilder.toString());
        } else if (nodeReading.hasStringReading()) {
            urlBuilder.append("/stringreading/").append(nodeReading.getStringReading())
                    .append("/");
            callUrl(urlBuilder.toString());
        }
    }

    /**
     * Adds a new link reading using the rest interfaces.
     *
     * @param linkReading the link reading to add
     */
    public RestCommiter(final Message.LinkReadings.Reading linkReading) {
        final StringBuilder urlBuilder = new StringBuilder(TESTBED_SERVER);
        urlBuilder.append('/').append(PropertyReader.getInstance().getTestbedId())
                .append("/link/").append(linkReading.getSource()).append('/').append(linkReading.getTarget())
                .append("/capability/").append(linkReading.getCapability())
                .append("/insert/timestamp/").append(linkReading.getTimestamp());

        if (linkReading.hasDoubleReading()) {
            //uberdust.cti.gr/rest/testbed/1/node/urn:wisebed:ctitestbed:virtual:0.I.9/capability/light1/insert/timestamp/1/reading/1/
            urlBuilder.append("/reading/").append(linkReading.getDoubleReading())
                    .append("/");
            callUrl(urlBuilder.toString());
        } else if (linkReading.hasStringReading()) {
            urlBuilder.append("/stringreading/").append(linkReading.getStringReading())
                    .append("/");
            callUrl(urlBuilder.toString());
        }
    }

    /**
     * Opens a connection over the Rest Interfaces to the server and adds the event.
     *
     * @param urlString the string url that describes the event
     */

    private void callUrl(final String urlString) {
        HttpURLConnection httpURLConnection = null;

        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            LOGGER.error(e);
            return;
        }

        try {
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.connect();

            if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                LOGGER.debug("Added " + urlString);
            } else {
                final StringBuilder errorBuilder = new StringBuilder("Problem ");
                errorBuilder.append("with ").append(urlString);
                errorBuilder.append(" Response: ").append(httpURLConnection.getResponseCode());
                LOGGER.error(errorBuilder.toString());
            }
            httpURLConnection.disconnect();
        } catch (IOException e) {
            LOGGER.error(e);
        }


    }
}
