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
    private byte[] payload;

    public String getMess() {
        return mess;
    }

    public HereIamMessage(byte[] payload) {
        this.payload = payload;
        StringBuilder sb = new StringBuilder();

        for (int i = 2; i < payload.length; i++) {
            sb.append((char) payload[i]);
        }
        mess = sb.toString();
    }

    public boolean isValid() {
//        System.out.println("HereIamMessageTest:"+mess);
        return mess.equals("hereiam");
    }
}
