package eu.uberdust.testbedlistener.coap;

import org.apache.log4j.Logger;

import java.net.SocketAddress;

/**
 * Contains all information about an active request from a source.
 */
public class Cache {

    private String value;
    private int observelostCounter;

    public int getContentType() {
        return contentType;
    }

    private int contentType;

    public int getMaxAge() {
        return maxAge;
    }

    private int maxAge;

    public String getValue() {
        return value;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    private Long timestamp;

    public Cache(final String value, final Long timestamp, final int maxAge, final int contentType) {
        this.value = value;
        this.timestamp = timestamp;
        this.maxAge = maxAge;
        this.contentType = contentType;
        this.observelostCounter = 0;
    }

    public void put(final String value, final long timestamp, final int maxAge, final int contentType, int lostCounter) {
        this.value = value;
        this.timestamp = timestamp;
        this.maxAge = maxAge;
        this.contentType = contentType;
        this.observelostCounter = lostCounter;
    }

    public void incLostCounter() {
        observelostCounter++;
    }

    public int getLostCounter() {
        return observelostCounter;
    }
}
