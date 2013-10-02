package eu.uberdust.testbedlistener;

import eu.uberdust.testbedlistener.coap.CoapServer;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Created with IntelliJ IDEA.
 * User: amaxilatis
 * Date: 6/12/13
 * Time: 12:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class HeartBeatJob implements Job {
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        try {
            CoapServer.getInstance().publish("heartbeat", "reset");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
