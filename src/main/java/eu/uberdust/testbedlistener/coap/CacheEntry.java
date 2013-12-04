package eu.uberdust.testbedlistener.coap;

import eu.uberdust.testbedlistener.datacollector.collector.CollectorMqtt;

/**
 * Contains all information about an active request from a source.
 */
public class CacheEntry {

    /**
     * The value of the IoT Resource.
     */
    private String value;

    /**
     * Returns the value of the IoT Resource.
     *
     * @return the value of the IoT Resource.
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of the IoT Resource.
     *
     * @param value the value of the IoT Resource.
     */
    public void setValue(final String value) {
        this.value = value;
    }

    /**
     * The time the IoT Resource's value was received.
     */
    private Long timestamp;

    /**
     * Returns time the IoT Resource's value was received.
     *
     * @return the time the IoT Resource's value was received.
     */
    public Long getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the time the IoT Resource's value was received.
     *
     * @param timestamp the time the IoT Resource's value was received.
     */
    public void setTimestamp(final Long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * The validity of the IoT Resource's value.
     */
    private int maxAge;

    /**
     * Returns the validity of the IoT Resource's value.
     *
     * @return the validity of the IoT Resource's value.
     */
    public int getMaxAge() {
        return maxAge;
    }

    /**
     * Sets the validity of the IoT Resource's value.
     *
     * @param maxAge the validity of the IoT Resource's value.
     */
    public void setMaxAge(final int maxAge) {
        this.maxAge = maxAge;
    }

    /**
     * The Content Type of the IoT Resource's value.
     */
    private int contentType;

    /**
     * Returns the Content Type of the IoT Resource's value.
     *
     * @return the Content Type of the IoT Resource's value.
     */
    public int getContentType() {
        return contentType;
    }

    /**
     * Sets the Content Type of the IoT Resource's value.
     *
     * @param contentType the Content Type of the IoT Resource's value.
     */
    public void setContentType(final int contentType) {
        this.contentType = contentType;
    }

    /**
     * The number of times the Observer for the IoT Resource had to be reset.
     */
    private int observeLostCounter;

    /**
     * Returns the number of times the Observer for the IoT Resource had to be reset.
     *
     * @return the number of times the Observer for the IoT Resource had to be reset.
     */
    public int getLostCounter() {
        return observeLostCounter;
    }

    /**
     * Set the number of times the Observer for the IoT Resource had to be reset.
     *
     * @param observeLostCounter the number of times the Observer for the IoT Resource had to be reset.
     */
    public void setObserveLostCounter(final int observeLostCounter) {
        this.observeLostCounter = observeLostCounter;
    }

    /**
     * The {@see CollectorMqtt} associated to this IoT Resource.
     */
    private CollectorMqtt collector;

    /**
     * Returns  the {@see CollectorMqtt} associated to this IoT Resource.
     *
     * @return the {@see CollectorMqtt} associated to this IoT Resource.
     */
    public CollectorMqtt getCollector() {
        return collector;
    }

    /**
     * Set the {@see CollectorMqtt} associated to this IoT Resource.
     *
     * @param collector the {@see CollectorMqtt} associated to this IoT Resource.
     */
    public void setCollector(final CollectorMqtt collector) {
        this.collector = collector;
    }

    public CacheEntry(final String value, final Long timestamp, final int maxAge, final int contentType, final CollectorMqtt collector) {
        this.value = value;
        this.timestamp = timestamp;
        this.maxAge = maxAge;
        this.contentType = contentType;
        this.observeLostCounter = 0;
        this.collector = collector;
    }
}
