package eu.uberdust.testbedlistener.coap;

/**
 * Contains all information about an active request from a source.
 */
public class Cache {

    private String value;
    private int observeLostCounter;

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

    public void setTimestamp(final Long timestamp) {
        this.timestamp = timestamp;
    }

    private Long timestamp;

    public Cache(final String value, final Long timestamp, final int maxAge, final int contentType) {
        this.value = value;
        this.timestamp = timestamp;
        this.maxAge = maxAge;
        this.contentType = contentType;
        this.observeLostCounter = 0;
    }

    public void put(final String value, final long timestamp, final int maxAge, final int contentType) {
        this.value = value;
        this.timestamp = timestamp;
        this.maxAge = maxAge;
        this.contentType = contentType;
//        if (System.currentTimeMillis() -timestamp > maxAge*1000) {
//            this.observelostCounter++;
//        }
    }

    public int getLostCounter() {
        return observeLostCounter;
    }

    public void incLostCounter() {
        observeLostCounter++;
    }
}
