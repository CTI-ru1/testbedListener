package eu.uberdust.testbedlistener.datacollector;

import eu.uberdust.testbedlistener.datacollector.parsers.CoapMessageParser;
import org.apache.log4j.Logger;

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
    private String testbedPrefix;
    private int testbedId;

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

    public synchronized void handle(final String messageString) {
        TestbedMessage testbedMessage = new TestbedMessage(messageString);
        LOGGER.info("ISTRCOAP" + testbedMessage.isValid());
        if (testbedMessage.isValid()) {
            executorService.submit(new CoapMessageParser(testbedMessage.getSourceNodeId(), testbedMessage.getDatabinary()));
        }
//        {
////            executorService.submit(new TestbedRuntimeParser(messageString));
//        }
    }

    class TestbedMessage {
        String sourceNodeId;
        String timestamp;
        String datastring;
        byte[] databinary;
        boolean valid;

        public TestbedMessage(final String input) {
            valid = true;
            try {
                int sourceStart = input.indexOf("sourceNodeId") + "sourceNodeId".length() + 3;
                int sourceEND = input.indexOf("timestamp") - 2;
                sourceNodeId = input.substring(sourceStart, sourceEND);
                int timeStart = input.indexOf("timestamp") + "timestamp".length() + 3;
                int timeEND = input.indexOf("binaryData") - 2;
                timestamp = input.substring(timeStart, timeEND);
                int dataStart = input.indexOf("binaryData: ") + "binaryData: ".length();
                int dataEND = input.length() - 2;
                datastring = input.substring(dataStart, dataEND);
            } catch (Exception e) {
                valid = false;
                return;
            }
            try {
                LOGGER.info(datastring);
                String temp2 = datastring.split("DATA:")[1];
                temp2 = temp2.replaceAll(" ", "");
                temp2 = temp2.replaceAll("\"", "");

                LOGGER.info(temp2);

                databinary = new byte[temp2.length() / 2 - 1];

                for (int i = 2; i < temp2.length(); i += 2) {
                    databinary[i / 2 - 1] = (byte) ((Character.digit(temp2.charAt(i), 16) << 4) + Character.digit(temp2.charAt(i + 1), 16));
                }
            } catch (Exception e) {
                valid = false;
                return;
            }
            LOGGER.info("ISVALID");

        }

        public String getSourceNodeId() {
            return sourceNodeId;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public String getDatastring() {
            return datastring;
        }

        public byte[] getDatabinary() {
            return databinary;
        }

        public boolean isValid() {
            return valid;
        }
    }

}
