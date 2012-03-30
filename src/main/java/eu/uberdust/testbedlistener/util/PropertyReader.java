package eu.uberdust.testbedlistener.util;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.IOException;
import java.util.Properties;

/**
 * Reads Properties from a master property file and offers them to Applications.
 */
public class PropertyReader {
    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(PropertyReader.class);

    /**
     * Singleton instance.
     */
    private static PropertyReader instance = null;
    /**
     * The property file.
     */
    final Properties properties;
    /**
     * The name of the property File.
     */
    private static final String PROPERTY_FILE = "testbedlistener.properties";

    /**
     * Default Constructor.
     */
    private PropertyReader() {
        PropertyConfigurator.configure(Thread.currentThread().getContextClassLoader().getResource("log4j.properties"));

        properties = new Properties();
        try {
            properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream(PROPERTY_FILE));
        } catch (IOException e) {
            LOGGER.error("No properties file found! " + PROPERTY_FILE + " not found!");
            return;
        }
        LOGGER.info("Loaded properties from file: " + PROPERTY_FILE);
    }

    /**
     * Get Singleton instance
     *
     * @return the unique Property Reader Instance.
     */
    public synchronized static PropertyReader getInstance() {
        if (instance == null) {
            instance = new PropertyReader();
        }
        return instance;
    }

    /**
     * Retruns a the property file so that applications can access the file contents.
     *
     * @return the Property File Object.
     */
    public Properties getProperties() {
        LOGGER.debug("getProperties()");
        return properties;
    }
}
