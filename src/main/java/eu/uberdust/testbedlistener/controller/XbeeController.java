package eu.uberdust.testbedlistener.controller;

import com.rapplogic.xbee.api.XBeeAddress16;
import eu.mksense.XBeeRadio;
import eu.uberdust.DeviceCommand;
import eu.uberdust.network.NetworkManager;
import eu.uberdust.testbedlistener.coap.CoapServer;
import eu.uberdust.testbedlistener.util.Converter;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.html.HTMLDocument;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

/**
 * Controlls the XBee Network.
 */
public final class XbeeController implements Observer {

    private static final Logger LOGGER = Logger.getLogger(XbeeController.class);

    /**
     * static instance(ourInstance) initialized as null.
     */
    private static XbeeController ourInstance = null;
    private HTMLDocument dom;
    private Map<String, String> routingTable;

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
        routingTable = new HashMap<String, String>();
        parseCustomRouting();
        LOGGER.info("XbeeController initialized");
    }


    public void sendCommand(final String dest, final String capability, final String payloadIn) {
        LOGGER.info("new command->" + dest + "," + capability + "," + payloadIn);
        if (payloadIn.contains(",,")) return;
        final String tDestination = dest.substring(dest.lastIndexOf(":") + 1).replace("0x", "");
        final String destination;
        LOGGER.info("checking key " + tDestination + "-" + capability);
        final String routingKey = tDestination + "-" + capability;
        if (routingTable.containsKey(routingKey)) {
            destination = routingTable.get(routingKey).replace("0x", "");
            LOGGER.info("changing end point " + destination);
        } else {
            destination = tDestination;
        }

        final int[] macAddress = Converter.getInstance().addressToInteger(destination);
        final XBeeAddress16 address16 = new XBeeAddress16(macAddress[0], macAddress[1]);
        final String[] dataString = payloadIn.split(",");
        LOGGER.info("here");
        final int[] data = new int[dataString.length - 3];

        for (int i = 3; i < dataString.length; i++) {
            data[i - 3] = Integer.valueOf(dataString[i], 16);
        }

        try {
            LOGGER.info(address16);
            XBeeRadio.getInstance().send(address16, 112, data);
        } catch (final Exception e) {
            LOGGER.error(e);
        }
    }

    public void sendPayload(final String destination, final byte[] payloadIn) {


        final int[] macAddress = Converter.getInstance().addressToInteger(destination);
        final XBeeAddress16 address16 = new XBeeAddress16(macAddress[0], macAddress[1]);
//        System.out.println(address16 + ":" + payloadIn.length);
        final int[] data = new int[payloadIn.length];

        for (int i = 0; i < data.length; i++) {
            data[i] = payloadIn[i];
//            System.out.println(payloadIn[i] + ":" + data[i] + "/" + Integer.toHexString(data[i]));
        }

        try {
            LOGGER.info(address16);
            XBeeRadio.getInstance().send(address16, 112, data);
        } catch (final Exception e) {
            LOGGER.error(e);
        }
    }

    public static void main(final String[] args) {
        NetworkManager.getInstance().start("192.168.1.5:8080/uberdust/", 2);
        NetworkManager.getInstance().addObserver(XbeeController.getInstance());
//        parseCustomRouting();


    }


    @Override
    public void update(final Observable observable, final Object o) {
        LOGGER.debug("called update ");
        if (o instanceof DeviceCommand) {

            final DeviceCommand command = (DeviceCommand) o;

            LOGGER.info("TO:" + command.getDestination() + " BYTES:" + command.getPayload());
            sendCommand(command.getDestination(), command.getCapability(), command.getPayload());

        }
    }


    private void parseCustomRouting() {
        File fXmlFile = new File("routing.xml");
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = null;

            dBuilder = dbFactory.newDocumentBuilder();

            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();

            LOGGER.info("Root element :" + doc.getDocumentElement().getNodeName());
            NodeList nList = doc.getElementsByTagName("route");
            LOGGER.info("-----------------------");

            for (int temp = 0; temp < nList.getLength(); temp++) {

                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {

                    Element eElement = (Element) nNode;
                    routingTable.put(getTagValue("source", eElement) + "-" + getTagValue("capability", eElement), getTagValue("destination", eElement));
                    LOGGER.info("Key" + getTagValue("source", eElement) + "-" + getTagValue("capability", eElement));
                    LOGGER.info("Destination : " + getTagValue("destination", eElement));
                    LOGGER.info("value is :" + routingTable.get(getTagValue("source", eElement) + "-" + getTagValue("capability", eElement)));

                }
            }
        } catch (ParserConfigurationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (SAXException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private String getTagValue(String sTag, Element eElement) {
        NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();

        Node nValue = (Node) nlList.item(0);

        return nValue.getNodeValue();
    }

}
