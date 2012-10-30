package eu.uberdust.testbedlistener.util;

/**
 * Created with IntelliJ IDEA.
 * User: amaxilatis
 * Date: 10/5/12
 * Time: 3:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class HereIamMessage {

    private String mess;

    public String getMess() {
        return mess;
    }

    public HereIamMessage(byte[] payload) {

        StringBuilder sb = new StringBuilder();
//        System.out.println(payload.length);

        for (int i : payload) {
            sb.append((char) i);
        }
        mess = sb.toString();
    }

    public boolean isValid() {
        return mess.equals("hereiam");
    }
}
