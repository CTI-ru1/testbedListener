package eu.uberdust.testbedlistener.controller.util;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: amaxilatis
 * Date: 5/31/12
 * Time: 3:10 PM
 */
public class RoutingTable {
    private static final Logger LOGGER = Logger.getLogger(RoutingTable.class);
    private static List<Route> routingTable = new ArrayList<Route>();

    public static String getRoute(String node, String capability) {

        for (Route route : routingTable) {
            if (route.getSource().equals(node)
                    && route.getCapability() == null) {
                return route.getDestination();
            } else if (route.getSource().equals(node)
                    && capability.startsWith(route.getCapability())) {
                return route.getDestination();
            }
        }
        return node;
    }

    public static void buildRoutingTables(final String filename) {

        File fXmlFile = new File(filename);
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
                    final Route route = new Route(getTagValue("source", eElement), getTagValue("capability", eElement), getTagValue("destination", eElement));
                    routingTable.add(route);
                    LOGGER.info(route);
                }
            }
            LOGGER.info("-----------------------");
        } catch (ParserConfigurationException e) {
            LOGGER.error(e.getMessage(),e);
        } catch (SAXException e) {
            LOGGER.error(e.getMessage(),e);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(),e);
        }
    }


    private static class Route {
        private String source;
        private String capability;
        private String destination;

        private Route(String source, String capability, String destination) {
            this.source = source;
            this.capability = capability;
            this.destination = destination;
        }

        public String getSource() {
            return source;
        }

        public String getCapability() {
            return capability;
        }

        public String getDestination() {
            return destination;
        }

        @Override
        public String toString() {
            return "Route{" +
                    "source='" + source + '\'' +
                    ", capability='" + capability + '\'' +
                    ", destination='" + destination + '\'' +
                    '}';
        }
    }


    private static String getTagValue(String sTag, Element eElement) {
        try {
            NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();
            Node nValue = (Node) nlList.item(0);
            return nValue.getNodeValue();
        } catch (NullPointerException npe) {
            return null;
        }
    }
}
