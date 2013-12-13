package eu.uberdust.testbedlistener.util;

import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: amaxilatis
 * Date: 5/22/12
 * Time: 12:47 AM
 * To change this template use File | Settings | File Templates.
 */
public class Converter {

    /**
     * LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(
            Converter.class);

    private static Converter instance = null;

    public static Converter getInstance() {
        synchronized (Converter.class) {
            if (instance == null) {
                instance = new Converter();
            }
        }
        return instance;
    }

    public int[] addressToInteger(final String address) {
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

    public byte[] addressToByte(String address) {
        if (address.contains("0x")) {
            address = address.split("0x")[1];
        }
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

    public String payloadToString(final int[] payload) {
        final StringBuilder stringBuilder = new StringBuilder("Contents:[");
        for (int i : payload) {
            stringBuilder.append("0x").append(Integer.toHexString(i)).append(",");
        }

        return stringBuilder.toString().substring(0, stringBuilder.toString().length() - 1) + "]";
    }


    /**
     * Converts a Byte Array to a hex String.
     *
     * @param payload The Byte Array to convert.
     * @return The hex string of the input.
     */
    public String payloadToString(final byte[] payload) {
        final StringBuilder stringBuilder = new StringBuilder();
        for (int i : payload) {
            String hexstr = Integer.toHexString(i);
            if (hexstr.length() > 2) {
                hexstr = hexstr.substring(hexstr.length() - 2);
            }
            stringBuilder.append(hexstr).append("|");
        }
        return stringBuilder.toString().substring(0, stringBuilder.toString().length() - 1);
    }

    public int[] ByteToInt(byte[] data) {
        final int[] bytes = new int[data.length];
        for (int i = 0; i < data.length; i++) {
            final short read = (short) ((short) data[i] & 0xff);
            bytes[i] = read;
            if (bytes[i] == 255) {
                bytes[i] = 0;
            }

        }
        return bytes;
    }

    public static List<String> extractCapabilities(final String message) {
        LOGGER.debug(message);
        String[] capabilities = message.split(",");
        List caps = new LinkedList();
        for (String capability : capabilities) {
            if (capability.contains("<") && capability.contains(">")) {
                String newCap = capability.replaceAll("<", "").replaceAll(">", "");
                StringBuilder validCap = new StringBuilder();
                for (char c : newCap.toCharArray()) {
                    if (c != 0) {
                        validCap.append(c);
                    }
                }
                caps.add(validCap.toString());
            }
        }
        LOGGER.debug(Arrays.toString(caps.toArray()));
        return caps;
    }

    public String message(byte[] payload) {
        StringBuilder sb = new StringBuilder();
        for (byte b : payload) {
            sb.append(b);
        }
        return sb.toString();
    }

    public static String extractRemainder(String message) {
        LOGGER.debug(message);
        int startTag = message.lastIndexOf("<");
        int endTag = message.lastIndexOf(">");
        if (endTag > startTag) {
            return "";
        } else if (endTag + 2 < message.length()) {
            return message.substring(endTag + 2);
        } else {
            return "";
        }
    }

    public byte[] commaPayloadtoBytes(final String payloadIn) {
        final String[] strBytes = payloadIn.split(",");
        final byte[] payload = new byte[strBytes.length];
        for (int i = 0; i < payload.length; i++) {
            payload[i] = Integer.valueOf(strBytes[i].replaceAll("\n", ""), 16).byteValue();
        }
        return payload;
    }

    public static String byteToString(byte macMSB) {
        String hexstr = Integer.toHexString(macMSB);
        if (hexstr.length() > 2) {
            hexstr = hexstr.substring(hexstr.length() - 2);
        }
        return hexstr;
    }
}
