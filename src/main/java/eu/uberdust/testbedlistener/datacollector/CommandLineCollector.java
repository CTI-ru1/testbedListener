package eu.uberdust.testbedlistener.datacollector;

import eu.uberdust.testbedlistener.datacollector.parsers.CommandLineParser;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Opens a connection to the XBee and receives messages from all nodes to collect data.
 */
public class CommandLineCollector extends AbstractCollector implements Runnable {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(CommandLineCollector.class);

    /**
     * Default Constructor.
     */
    public CommandLineCollector() {
        PropertyConfigurator.configure(Thread.currentThread().getContextClassLoader().getResource("log4j.properties"));
    }

    @Override
    public void run() {
        //  prompt the user to enter their name
        System.out.println("Enter a new reading: ");

        //  open up standard input
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        String readingString = null;

        //  read the username from the command-line; need to use try/catch with the
        //  readLine() method
        while (true) {
            try {
                LOGGER.info("waiting for input");
                readingString = br.readLine();
                if (readingString.equals("exit")) {
                    System.exit(0);
                }
                CommandLineParser messageParser = new CommandLineParser(readingString);
                messageParser.run();
            } catch (IOException ioe) {
                System.out.println("IO error trying to read your name!");
                System.exit(1);
            }
        }

    }
}

