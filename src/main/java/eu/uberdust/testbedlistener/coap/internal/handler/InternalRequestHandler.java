package eu.uberdust.testbedlistener.coap.internal.handler;

import ch.ethz.inf.vs.californium.coap.Message;

/**
 * Created with IntelliJ IDEA.
 * User: amaxilatis
 * Date: 6/13/13
 * Time: 3:52 PM
 * To change this template use File | Settings | File Templates.
 */
public interface InternalRequestHandler {
    void handle(Message udpRequest, Message response);
}
