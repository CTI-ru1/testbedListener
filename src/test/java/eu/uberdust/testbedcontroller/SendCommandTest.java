package eu.uberdust.testbedcontroller;

import com.google.common.collect.Lists;
import de.uniluebeck.itm.wisebed.cmdlineclient.BeanShellHelper;
import de.uniluebeck.itm.wisebed.cmdlineclient.wrapper.WSNAsyncWrapper;
import eu.uberdust.util.PropertyReader;
import eu.wisebed.api.common.Message;
import eu.wisebed.api.sm.ExperimentNotRunningException_Exception;
import eu.wisebed.api.sm.SessionManagement;
import eu.wisebed.api.sm.UnknownReservationIdException_Exception;
import eu.wisebed.api.wsn.WSN;
import eu.wisebed.testbed.api.wsn.WSNServiceHelper;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by IntelliJ IDEA.
 * User: akribopo
 * Date: 3/12/12
 * Time: 1:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class SendCommandTest {

    private static final byte PAYLOAD_PREFIX = 0xb;
    private static final byte[] PAYLOAD_HEADERS = new byte[]{0x7f, 0x69, 0x70};

    private String secretReservationKeys;
    private String sessionManagementEndpointURL;
    private String nodeUrnsToListen;
    private String pccHost;
    private Integer pccPort;


    private WSNAsyncWrapper wsn;
    private List<String> nodeURNs = new ArrayList<String>();


    /**
     * static instance(ourInstance) initialized as null.
     */
    private static SendCommandTest ourInstance = null;

    /**
     * TestbedController is loaded on the first execution of TestbedController.getInstance()
     * or the first access to TestbedController.ourInstance, not before.
     *
     * @return ourInstance
     */
    public static SendCommandTest getInstance() {
        synchronized (SendCommandTest.class) {
            if (ourInstance == null) {
                ourInstance = new SendCommandTest();
            }
        }

        return ourInstance;
    }

    /**
     * Private constructor suppresses generation of a (public) default constructor.
     */
    private SendCommandTest() {
        PropertyConfigurator.configure("/home/akribopo/Projects/uberdust/testbedListener/src/main/resources/log4j.properties");
        readProperties();
        connectToRuntime();
    }

    private void readProperties() {
        secretReservationKeys = PropertyReader.getInstance().getProperties().getProperty("testbed.secretReservationKeys");
        sessionManagementEndpointURL = PropertyReader.getInstance().getProperties().getProperty("testbed.sm.endpointurl");
        nodeUrnsToListen = PropertyReader.getInstance().getProperties().getProperty("testbed.nodeUrns");
        pccHost = PropertyReader.getInstance().getProperties().getProperty("testbed.hostname");
        pccPort = Integer.parseInt(PropertyReader.getInstance().getProperties().getProperty("testbed.port"));
        nodeURNs = Lists.newArrayList(nodeUrnsToListen.split(","));

        if (nodeURNs.isEmpty()) {
            throw new RuntimeException("No node Urns To Listen");
        } else {
            for (String nodeURN : nodeURNs) {
                System.out.println(nodeURN);
                System.out.println(nodeURN);
            }
        }
    }

    private void connectToRuntime() {

        String wsnEndpointURL = null;
        final SessionManagement sessionManagement = WSNServiceHelper.getSessionManagementService(sessionManagementEndpointURL);
        try {
            wsnEndpointURL = sessionManagement.getInstance(BeanShellHelper.parseSecretReservationKeys(secretReservationKeys), "NONE");
        } catch (final ExperimentNotRunningException_Exception e) {
            e.printStackTrace();
        } catch (final UnknownReservationIdException_Exception e) {
            e.printStackTrace();
        }
        System.out.println("Got a WSN instance URL, endpoint is: " + wsnEndpointURL);

        final WSN wsnService = WSNServiceHelper.getWSNService(wsnEndpointURL);
        wsn = WSNAsyncWrapper.of(wsnService);
        System.out.println("Retrieved the following node URNs: {}" + nodeURNs);
    }

    public static String setValue(String a) {
        return "0,0,0,0,1,ff,4,b5,47,1,0,fc,0,#".replaceAll("#", a);
    }


    public void Send(Message msg) {
        System.out.println(nodeURNs);
        wsn.send(nodeURNs, msg, 100, TimeUnit.SECONDS);
        System.out.println("send");
    }


    public static void main(final String[] args) throws InterruptedException {

        BasicConfigurator.configure();
        SendCommandTest.getInstance();

        //final String macAddress = "4ec";
        //final String command = "0,0,f,f,89,0,1,FF,0";

        final String macAddress = "c56f";
        //turn on Led 1

        //turn on Led 2
        final String command = "0,0,0,1,ff,4,b5,47,5,0,fc,0,0,0,8,0,8,0,8,0,8";


        //SendCommandTest.getInstance().Send(getMessage(macAddress, command, true));
        SendCommandTest.getInstance().Send(getMessage(macAddress, setValue("14"),false));
        Thread.sleep(1000);

    }

    public static Message getMessage(String macAddress, String command, boolean isForiSenseNetwork) {
        final String[] strPayload = command.split(",");
        final byte[] payload = new byte[strPayload.length];
        for (int i = 0; i < payload.length; i++) {
            payload[i] = Integer.valueOf(strPayload[i], 16).byteValue();
        }

        final byte[] macBytes = new byte[2];
        if (macAddress.length() == 4) {
            macBytes[0] = Integer.valueOf(macAddress.substring(0, 2), 16).byteValue();
            macBytes[1] = Integer.valueOf(macAddress.substring(2, 4), 16).byteValue();
        } else if (macAddress.length() == 3) {
            macBytes[0] = Integer.valueOf(macAddress.substring(0, 1), 16).byteValue();
            macBytes[1] = Integer.valueOf(macAddress.substring(1, 3), 16).byteValue();
        }

        payload[0] = PAYLOAD_PREFIX;
        payload[1] = macBytes[0];
        payload[2] = macBytes[1];

        if (isForiSenseNetwork) {
            payload[3] = 0x7f;
            payload[4] = 0x69;
            payload[5] = 0x70;
        }

        System.out.println(Arrays.toString(payload));
        final Message msg = new Message();
        msg.setBinaryData(payload);
        msg.setSourceNodeId("urn:wisebed:ctitestbed:0x1");
        try {
            msg.setTimestamp(DatatypeFactory.newInstance().newXMLGregorianCalendar(
                    (GregorianCalendar) GregorianCalendar.getInstance()));
        } catch (final DatatypeConfigurationException e) {
            System.out.println(e);
        }
        return msg;
    }

}


