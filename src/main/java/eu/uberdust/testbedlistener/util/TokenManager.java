package eu.uberdust.testbedlistener.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Created by IntelliJ IDEA.
 * User: amaxilatis
 * Date: 11/27/12
 * Time: 12:56 PM
 */
public class TokenManager {

    private static TokenManager instance = null;
    private Random random;
    private Map<byte[], Integer> existingTokens;

    private TokenManager() {
        random = new Random();
        existingTokens = new HashMap<byte[], Integer>();

    }

    public static TokenManager getInstance() {
        synchronized (TokenManager.class) {
            if (instance == null) {
                instance = new TokenManager();
            }
            return instance;
        }
    }


    public byte[] acquireToken() {
        byte[] tokenBytes = new byte[8];

        do {
            tokenBytes[0] = (byte) 0xaa;
            for (int i = 1; i < 8; i++) {
                tokenBytes[i] = (byte) (random.nextInt(255) - 127);
            }
        } while (existingTokens.containsKey(tokenBytes));
        existingTokens.put(tokenBytes, 1);


        return tokenBytes;
    }
}
