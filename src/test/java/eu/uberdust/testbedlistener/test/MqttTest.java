package eu.uberdust.testbedlistener.test;

import eu.uberdust.testbedlistener.datacollector.collector.MqttCollector;
import eu.uberdust.testbedlistener.util.PropertyReader;

import java.net.SocketException;

/**
 * Created with IntelliJ IDEA.
 * User: amaxilatis
 * Date: 6/4/13
 * Time: 12:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class MqttTest {

    public static void main(String[] args) throws SocketException {

        PropertyReader.getInstance().setFile("listener.properties");
        MqttCollector mqList = new MqttCollector("tcp://localhost:1883", "testbed1/#", true);
        new Thread(mqList).start();
    }

}


