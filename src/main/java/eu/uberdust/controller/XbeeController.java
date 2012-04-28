package eu.uberdust.controller;

import com.google.common.collect.Lists;
import com.rapplogic.xbee.api.XBeeAddress16;
import com.rapplogic.xbee.api.wpan.RxResponse16;
import de.uniluebeck.itm.wisebed.cmdlineclient.BeanShellHelper;
import de.uniluebeck.itm.wisebed.cmdlineclient.protobuf.ProtobufControllerClient;
import de.uniluebeck.itm.wisebed.cmdlineclient.wrapper.WSNAsyncWrapper;
import eu.mksense.XBeeRadio;
import eu.uberdust.DeviceCommand;
import eu.uberdust.testbedlistener.util.PropertyReader;
import eu.wisebed.api.common.Message;
import eu.wisebed.api.sm.ExperimentNotRunningException_Exception;
import eu.wisebed.api.sm.SessionManagement;
import eu.wisebed.api.sm.UnknownReservationIdException_Exception;
import eu.wisebed.api.wsn.WSN;
import eu.wisebed.testbed.api.wsn.WSNServiceHelper;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Controlls the XBee Network.
 */
public class XbeeController implements Observer {

    private static final Logger LOGGER = Logger.getLogger(TestbedController.class);

    /**
     * static instance(ourInstance) initialized as null.
     */
    private static XbeeController ourInstance = null;

    /**
     * XbeeController is loaded on the first execution of XbeeController.getInstance()
     * or the first access to XbeeController.ourInstance, not before.
     *
     * @return ourInstance
     */
    public static XbeeController getInstance() {
        synchronized (XbeeController.class) {
            if (ourInstance == null) {
                ourInstance = new XbeeController();
            }
        }

        return ourInstance;
    }

    /**
     * Private constructor suppresses generation of a (public) default constructor.
     */
    private XbeeController() {
        PropertyConfigurator.configure(this.getClass().getClassLoader().getResource("log4j.properties"));
    }


    public void sendCommand(final String dest, final String payloadIn) {
        final String destination = dest.substring(dest.lastIndexOf(":")).replace("0x", "");

        final Integer[] macAddress = new Integer[2];

        if (destination.length() == 4) {
            macAddress[0] = Integer.valueOf(destination.substring(0, 2), 16);
            macAddress[1] = Integer.valueOf(destination.substring(2, 4), 16);
        } else if (destination.length() == 3) {
            macAddress[0] = Integer.valueOf(destination.substring(0, 1), 16);
            macAddress[1] = Integer.valueOf(destination.substring(1, 3), 16);
        }
        final XBeeAddress16 address16 = new XBeeAddress16(macAddress[0], macAddress[1]);

        final String[] dataString = payloadIn.split(",");

        final int[] data = new int[dataString.length];

        for (int i = 0; i < dataString.length; i++) {
            data[i] = Integer.valueOf(dataString[i], 16);
        }

        try {
            XBeeRadio.getInstance().send(address16, 112, data);
        } catch (final Exception e) {
            LOGGER.error(e);
        }
    }

    public static void main(final String[] args) {
        TestbedController.getInstance();
    }

    @Override
    public void update(final Observable observable, final Object o) {
        LOGGER.debug("called update ");
        if (o instanceof DeviceCommand) {

            final DeviceCommand command = (DeviceCommand) o;

            LOGGER.info("TO:" + command.getDestination() + " BYTES:" + command.getPayload());
            sendCommand(command.getDestination(), command.getPayload());

        }
    }
}
