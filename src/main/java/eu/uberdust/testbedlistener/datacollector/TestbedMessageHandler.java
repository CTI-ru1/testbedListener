package eu.uberdust.testbedlistener.datacollector;

import de.uniluebeck.itm.tr.runtime.wsnapp.WSNAppMessages;
import eu.uberdust.communication.UberdustClient;
import eu.uberdust.testbedlistener.datacollector.parsers.CoapMessageParser;
import org.apache.log4j.Logger;
import org.json.JSONException;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestbedMessageHandler {
    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(TestbedMessageHandler.class);

    /**
     * Singleton instance.
     */
    private static TestbedMessageHandler instance = null;

    /**
     * executors for handling incoming messages.
     */
    private final transient ExecutorService executorService;
    private int testbedId;
    private String testbedPrefix;
    private String capabilityPrefix;

    public String getTestbedPrefix() {
        return testbedPrefix;
    }

    public void setTestbedPrefix(final String testbedPrefix) {
        this.testbedPrefix = testbedPrefix;
    }

    public int getTestbedId() {
        return testbedId;
    }

    public void setTestbedId(final int testbedId) {
        this.testbedId = testbedId;

        testbedPrefix = null;
        capabilityPrefix = null;
        try {
            try {
                testbedPrefix = UberdustClient.getInstance().getUrnPrefix(testbedId);
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            capabilityPrefix = UberdustClient.getInstance().getUrnCapabilityPrefix(testbedId);
        } catch (JSONException e) {
            LOGGER.error(e, e);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }

    public TestbedMessageHandler() {
        executorService = Executors.newCachedThreadPool();
    }


    /**
     * Get Singleton instance
     *
     * @return the unique Property Reader Instance.
     */
    public synchronized static TestbedMessageHandler getInstance() {
        if (instance == null) {
            instance = new TestbedMessageHandler();
        }
        return instance;
    }

    public synchronized void handle(final WSNAppMessages.Message message) {
//        executorService.submit(new CoapMessageParser(message.getSourceNodeId(), message.getBinaryData().toByteArray(), testbedPrefix, capabilityPrefix, this));
//        Thread d = new Thread(new CoapMessageParser(message.getSourceNodeId(), message.getBinaryData().toByteArray()));
//        d.start();
    }


}
