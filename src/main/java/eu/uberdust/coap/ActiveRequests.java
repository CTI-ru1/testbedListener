package eu.uberdust.coap;

import ch.ethz.inf.vs.californium.coap.Message;
import com.sun.media.sound.MidiInDeviceProvider;

/**
 * Created by IntelliJ IDEA.
 * User: ven0m
 * Date: 5/21/12
 * Time: 2:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class ActiveRequests {
    private static String[][] uriPATH;
    private static int[][] mid;
    private static String[][] token;
    private static boolean[][] registered;
    private static final int MAX_HOSTS = 20;
    private static final int MAX_ACTIVE = 20;
    private static String[] hosts;

    public ActiveRequests()
    {
        registered = new boolean[MAX_HOSTS][MAX_ACTIVE];
        uriPATH = new String[MAX_HOSTS][MAX_ACTIVE];
        mid = new int[MAX_HOSTS][MAX_ACTIVE];
        token = new String[MAX_HOSTS][MAX_ACTIVE];
        hosts = new String[MAX_HOSTS];
        for (int i = 0; i < MAX_HOSTS; i++) {
            hosts[i] = "";
            for (int j = 0; j < MAX_ACTIVE; j++) {
                registered[i][j] = false;
            }
        }
    }

    public static void addRequest(String host, Message request) {
        int hostID = addHost(host);
        hosts[hostID] = host;
        int i = getEmptySlot(hostID);
        registered[hostID][i] = true;
        uriPATH[hostID][i] = request.getUriPath();
        mid[hostID][i] = request.getMID();
        token[hostID][i] = request.getTokenString();
    }

    private static int addHost(String host) {
        int hold = -1;
        for (int i = 0; i < MAX_HOSTS; i++) {
            if(hosts[i].equals(host)) {
                return i;
            }
            if(hosts[i].isEmpty() && hold == -1) {
                hold = i;

            }
        }
        return hold;
    }

    private static int getEmptySlot(int hostID) {
        for (int i = 0; i < MAX_ACTIVE; i++) {
            if(registered[hostID][i] == false)
                return i;
        }
        return -1;
    }
    public static String matchResponse(String host, Message response) {
        int hostID = -1;
        for (int i = 0; i < MAX_HOSTS; i++) {
            if(hosts[i].equals(host)) {
                hostID = i;
                break;
            }
        }
        if(response.getTokenString().isEmpty()) {
            for (int i = 0; i < MAX_ACTIVE; i++) {
                if(mid[hostID][i] == response.getMID() && registered[hostID][i] == true) {
                    registered[hostID][i] = false;
                    mid[hostID][i] = 0;
                    token[hostID][i] = "";
                    return uriPATH[hostID][i];
                }
            }
        }
        else {
            for (int i = 0; i < MAX_ACTIVE; i++) {
                if(token[hostID][i].equals(response.getTokenString()) && registered[hostID][i] == true) {
                    return uriPATH[hostID][i];
                }
            }
        }
        return null;
    }
}
