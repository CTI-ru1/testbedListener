/**
 * Copyright (c) 2008 Andrew Rapp. All rights reserved.
 *
 * This file is part of XBee-API.
 *
 * XBee-API is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XBee-API is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with XBee-API.  If not, see <http://www.gnu.org/licenses/>.
 */

package eu.uberdust.datacollector.test;

import com.rapplogic.xbee.api.XBeeException;
import com.rapplogic.xbee.api.wpan.RxResponse16;
import eu.mksense.MessageListener;
import eu.mksense.XBeeRadio;
import org.apache.log4j.Logger;

public class XBeeTest {
    private final static Logger LOGGER = Logger.getLogger(XBeeTest.class);
    private static final String PORT = "/dev/ttyUSB0";
    private static final int RATE = 38400;
    private static final int MPORT = 112;
    private static final int CHANNEL = 12;


    public static void main(String[] args) throws XBeeException {

        try {
            LOGGER.info("Opening...");
            XBeeRadio.getInstance().open(PORT, RATE);
            LOGGER.info("Connection Opened");
            XBeeRadio.getInstance().setChannel(CHANNEL);
            LOGGER.info("Channel: " + XBeeRadio.getInstance().getChannel());
            LOGGER.info("Address: " + XBeeRadio.getInstance().getMyAddress());
            LOGGER.info("XbeeAddress: " + XBeeRadio.getInstance().getMyXbeeAddress());
            LOGGER.info("PID: " + XBeeRadio.getInstance().getPanId());

            XBeeRadio.getInstance().addMessageListener(MPORT, new MessageListener() {
                @Override
                public void receive(RxResponse16 rxResponse16) {
                    LOGGER.info(rxResponse16);
                }
            });
        } catch (Exception e) {
            LOGGER.error(e, e);
        }
    }
}