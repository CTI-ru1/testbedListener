package eu.uberdust.testbedlistener.mq.listener;

import com.sensorflare.mq.RabbitMQListener;
import eu.uberdust.testbedlistener.coap.Cache;
import eu.uberdust.testbedlistener.coap.CacheHandler;

/**
 * A {@see RabbitMQListener} Class that receives command requests and forwards them to the gateways (if available).
 *
 * @author Dimitrios Amaxilatis
 */
public class RabbitMQCommandsListener implements RabbitMQListener {
    @Override
    public String getConsumerTag() {
        return "SparksCommandListener";  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void receive(String s, String s2, String s3, byte[] bytes) {
        final String command = new String(bytes);
        System.out.println(command);
        if (command.contains(",")) {
            final String key = command.split(",")[0].replaceAll("0x", "");
            final String payload = command.split(",")[1];
            final Cache item = CacheHandler.getInstance().getValue(key);
            if (item != null) {
                item.getCollector().postMessage(key, payload);
            }
        }
    }
}
