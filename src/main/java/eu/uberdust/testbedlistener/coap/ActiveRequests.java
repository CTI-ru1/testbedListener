package eu.uberdust.testbedlistener.coap;

import org.apache.log4j.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: ven0m
 * Date: 5/21/12
 * Time: 2:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class ActiveRequests {

    /**
     * LOGGER.
     */
    private static final Logger LOGGER = Logger.getLogger(ActiveRequests.class);

    private String uriPATH;
    private int mid;
    private String token;
    private String host;

    public String getUriPATH() {
        return uriPATH;
    }

    public int getMid() {
        return mid;
    }

    public String getToken() {
        return token;
    }

    public String getHost() {
        return host;
    }

    public ActiveRequests(String uriPATH, int mid, String token, String host) {
        this.uriPATH = uriPATH;
        this.mid = mid;
        this.token = token;
        this.host = host;
    }

    //    public ActiveRequests() {
//        registered = new boolean[MAX_HOSTS][MAX_ACTIVE];
//        uriPATH = new String[MAX_HOSTS][MAX_ACTIVE];
//        mid = new int[MAX_HOSTS][MAX_ACTIVE];
//        token = new String[MAX_HOSTS][MAX_ACTIVE];
//        hosts = new String[MAX_HOSTS];
//        for (int i = 0; i < MAX_HOSTS; i++) {
//            hosts[i] = "";
//            for (int j = 0; j < MAX_ACTIVE; j++) {
//                registered[i][j] = false;
//            }
//        }
//    }

//
//    public static String matchResponse(String host, Message response) {
//        int hostID = -1;
//        for (int i = 0; i < MAX_HOSTS; i++) {
//            if (hosts[i].equals(host)) {
//                hostID = i;
//                break;
//            }
//        }
//        LOG
//        if (response.getTokenString().isEmpty()) {
//            for (int i = 0; i < MAX_ACTIVE; i++) {
//                if (mid[hostID][i] == response.getMID() && registered[hostID][i] == true) {
//                    registered[hostID][i] = false;
//                    mid[hostID][i] = 0;
//                    token[hostID][i] = "";
//                    return uriPATH[hostID][i];
//                }
//            }
//        } else {
//            for (int i = 0; i < MAX_ACTIVE; i++) {
//                if (token[hostID][i].equals(response.getTokenString()) && registered[hostID][i] == true) {
//                    return uriPATH[hostID][i];
//                }
//            }
//        }
//        return null;
//    }
}
