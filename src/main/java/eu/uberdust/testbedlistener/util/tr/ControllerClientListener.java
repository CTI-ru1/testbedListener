package eu.uberdust.testbedlistener.util.tr;

import eu.wisebed.api.common.Message;
import eu.wisebed.api.controller.Controller;
import eu.wisebed.api.controller.RequestStatus;

import javax.jws.WebParam;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: akribopo
 * Date: 10/3/11
 * Time: 4:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class ControllerClientListener implements Controller {

    @Override
    public void experimentEnded() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void receive(@WebParam(name = "msg", targetNamespace = "") final List<Message> messages) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void receiveNotification(@WebParam(name = "msg", targetNamespace = "") final List<String> strings) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void receiveStatus(@WebParam(name = "status", targetNamespace = "") final List<RequestStatus> requestStatuses) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
