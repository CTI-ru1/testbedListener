package eu.uberdust.testbedlistener.util;

/**
 * Created by IntelliJ IDEA.
 * User: amaxilatis
 * Date: 5/22/12
 * Time: 12:47 AM
 * To change this template use File | Settings | File Templates.
 */
public class Converter {
    public static int[] AddressToInteger(final String address) {
        final int[] macAddress = new int[2];
        if (address.length() == 4) {
            macAddress[0] = Integer.valueOf(address.substring(0, 2), 16);
            macAddress[1] = Integer.valueOf(address.substring(2, 4), 16);
        } else if (address.length() == 3) {
            macAddress[0] = Integer.valueOf(address.substring(0, 1), 16);
            macAddress[1] = Integer.valueOf(address.substring(1, 3), 16);
        }
        return macAddress;
    }

    public static byte[] AddressToByte(final String address) {
        final byte[] macAddress = new byte[2];
        if (address.length() == 4) {
            macAddress[0] = Integer.valueOf(address.substring(0, 2), 16).byteValue();
            macAddress[1] = Integer.valueOf(address.substring(2, 4), 16).byteValue();
        } else if (address.length() == 3) {
            macAddress[0] = Integer.valueOf(address.substring(0, 1), 16).byteValue();
            macAddress[1] = Integer.valueOf(address.substring(1, 3), 16).byteValue();
        }
        return macAddress;
    }
}
