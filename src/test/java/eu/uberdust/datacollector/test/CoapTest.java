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

import ch.ethz.inf.vs.californium.coap.Request;
import com.rapplogic.xbee.api.XBeeException;
import eu.uberdust.testbedlistener.util.Converter;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import static ch.ethz.inf.vs.californium.coap.CodeRegistry.METHOD_GET;

public class CoapTest {
    private final static Logger LOGGER = Logger.getLogger(CoapTest.class);


    public static void main(String[] args) throws XBeeException {
        BasicConfigurator.configure();

        Request request = new Request(METHOD_GET, false);
        request.setURI("/.well-known/core");
        int[] payload = Converter.getInstance().ByteToInt(request.toByteArray());

        LOGGER.info(Converter.getInstance().payloadToString(payload));
        LOGGER.info(Converter.getInstance().payloadToString(request.toByteArray()));

    }

}