package eu.uberdust.testbedlistener.coap.internal.handler.test;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import eu.uberdust.testbedlistener.coap.internal.handler.WakeupRequestHandler;
import org.junit.Test;


public class WakeupRequestHandlerTest {
    @Test
    public void testHandleOnGet() throws Exception {
        WakeupRequestHandler handler = new WakeupRequestHandler();
        Request request = new Request(CodeRegistry.METHOD_GET, false);
        Response response = new Response();
        handler.handle(request, response);
        assert (response.getCode() == CodeRegistry.RESP_METHOD_NOT_ALLOWED);

    }

    @Test
    public void testHandleOnPost() throws Exception {
        WakeupRequestHandler handler = new WakeupRequestHandler();
        Request request = new Request(CodeRegistry.METHOD_POST, false);
        Response response = new Response();
        request.setPayload("aaa");
        handler.handle(request, response);
        assert ("Here I am simulation on aaa".equals(response.getPayloadString()));
        assert (response.getCode() == CodeRegistry.RESP_VALID);
    }

    @Test
    public void testHandleOnPut() throws Exception {
        WakeupRequestHandler handler = new WakeupRequestHandler();
        Request request = new Request(CodeRegistry.METHOD_PUT, false);
        Response response = new Response();
        handler.handle(request, response);
        assert (response.getCode() == CodeRegistry.RESP_METHOD_NOT_ALLOWED);
    }

    @Test
    public void testHandleOnDelete() throws Exception {
        WakeupRequestHandler handler = new WakeupRequestHandler();
        Request request = new Request(CodeRegistry.METHOD_DELETE, false);
        Response response = new Response();
        handler.handle(request, response);
        assert (response.getCode() == CodeRegistry.RESP_METHOD_NOT_ALLOWED);
    }
}
