package chapter2_verticles.deploy;

import io.vertx.core.AbstractVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author sm@creativefusion.net
 */
public class EmptyVerticle extends AbstractVerticle {

    private final Logger logger = LoggerFactory.getLogger(EmptyVerticle.class);

    @Override
    public void start() {
        logger.info("Start");
    }

    @Override
    public void stop() {
        logger.info("Stop");
    }
}