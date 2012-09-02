package eu.uberdust.testbedlistener.controller;

import eu.uberdust.DeviceCommand;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.util.Observable;
import java.util.Observer;


/**
 * Sends Commands to the Testbed.
 */
public final class CommandLineController implements Observer {

    private static final Logger LOGGER = Logger.getLogger(CommandLineController.class);

    /**
     * static instance(ourInstance) initialized as null.
     */
    private static CommandLineController ourInstance = null;

    /**
     * TestbedController is loaded on the first execution of TestbedController.getInstance()
     * or the first access to TestbedController.ourInstance, not before.
     *
     * @return ourInstance
     */
    public static CommandLineController getInstance() {
        synchronized (CommandLineController.class) {
            if (ourInstance == null) {
                ourInstance = new CommandLineController();
            }
        }

        return ourInstance;
    }

    /**
     * Private constructor suppresses generation of a (public) default constructor.
     */
    private CommandLineController() {
        PropertyConfigurator.configure(this.getClass().getClassLoader().getResource("log4j.properties"));
    }

    @Override
    public void update(final Observable observable, final Object o) {
        LOGGER.debug("called update ");
        if (o instanceof DeviceCommand) {
            final DeviceCommand command = (DeviceCommand) o;
            LOGGER.info("TO:" + command.getDestination() + " BYTES:" + command.getPayload());

        }
    }
}
