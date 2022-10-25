package chapter3_eventbus.cluster;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * If you are using IPv6 and encountering issues, you can add the -Djava.net.preferIPv4Stack=true flag to the JVM
 * parameters.
 *
 * http://localhost:8080
 * http://localhost:8081
 *
 * @author sm@creativefusion.net
 */
public class FirstInstance {

    private static final Logger logger = LoggerFactory.getLogger(FirstInstance.class);

    public static void main(String[] args) {
        Vertx.clusteredVertx(new VertxOptions(), ar -> {
            if (ar.succeeded()) {
                logger.info("First instance has been started");
                Vertx vertx = ar.result();
                vertx.deployVerticle("chapter3_eventbus.HeatSensor", new DeploymentOptions().setInstances(4));
                vertx.deployVerticle("chapter3_eventbus.HttpServer");
            } else {
                logger.error("Could not start", ar.cause());
            }
        });
    }
}
