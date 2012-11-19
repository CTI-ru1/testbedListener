package eu.uberdust.testbedlistener.coap;

import org.apache.log4j.Logger;

import java.net.SocketAddress;

/**
 * Contains all information about an active request from a source.
 */
public class Cache {

    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    private Long timestamp;

    public Cache(final String value, final Long timestamp ) {
        this.value = value;
        this.timestamp = timestamp;
    }

    public void put(String value, long timestamp) {
        this.value = value;
        this.timestamp = timestamp;
    }
}
