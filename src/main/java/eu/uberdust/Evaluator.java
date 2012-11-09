package eu.uberdust;

import org.apache.log4j.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: amaxilatis
 * Date: 10/30/12
 * Time: 2:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class Evaluator {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(Evaluator.class);

    public Evaluator(final String message, double value, final String unit) {
//        LOGGER.info(new StringBuilder().append(message).append(" ").append(+value).append(" ").append(unit).toString());
//        System.out.println(new StringBuilder().append(message).append(" ").append(+value).append(" ").append(unit).toString());
    }

    public Evaluator(final String message, long value, final String unit) {
//        LOGGER.info(new StringBuilder().append(message).append(" ").append(+value).append(" ").append(unit).toString());
    }
}
