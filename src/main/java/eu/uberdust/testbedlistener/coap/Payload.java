package eu.uberdust.testbedlistener.coap;

import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * User: amaxilatis
 * Date: 11/22/12
 * Time: 12:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class Payload {
    byte[] bytes;

    public Payload(byte[] bytes) {
        this.bytes = bytes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Payload payload = (Payload) o;

        if (!Arrays.equals(bytes, payload.bytes)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }
}
