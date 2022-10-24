package chapter2.hello;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This verticle defines two event handlers: one for periodic tasks every five seconds, and one for processing HTTP
 * requests in an HTTP server.
 *
 * The main method instantiates a global Vert.x instance and deploys an instance of the verticle.
 *
 * @author sm@creativefusion.net
 */
public class HelloVerticle extends AbstractVerticle {
    private final Logger logger = LoggerFactory.getLogger(HelloVerticle.class);
    private long counter = 1;

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new HelloVerticle());
    }

    @Override
    public void start() {

        vertx.setPeriodic(5000, id -> {
            logger.info("tick");
        });

        vertx.createHttpServer()
                .requestHandler(req -> {
                    logger.info("Request #{} from {}", counter++, req.remoteAddress().host());
                    req.response().end("Hello!");
                })
                .listen(8080);

        logger.info("Open http://localhost:8080/");
    }

}
