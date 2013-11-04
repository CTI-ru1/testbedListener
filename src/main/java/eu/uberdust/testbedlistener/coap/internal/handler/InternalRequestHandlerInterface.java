package eu.uberdust.testbedlistener.coap.internal.handler;

import ch.ethz.inf.vs.californium.coap.Message;

/**
 * Interface used to add CoAP endpoints that are handled by the CoAP Server and not the IoT devices.
 *
 * @author Dimitrios Amaxilatis
 * @date 13/6/13
 */
public interface InternalRequestHandlerInterface {
    void handle(final Message udpRequest, final Message response);
}
