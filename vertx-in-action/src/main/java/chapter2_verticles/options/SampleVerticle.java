package chapter2_verticles.options;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author sm@creativefusion.net
 */
public class SampleVerticle extends AbstractVerticle {

    private final Logger logger = LoggerFactory.getLogger(SampleVerticle.class);

    @Override
    public void start() {
        logger.info("n = {}", config().getInteger("n", -1));
    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        for (int n = 0; n < 4; n++) {
            JsonObject conf = new JsonObject().put("n", n);
            DeploymentOptions opts = new DeploymentOptions()
                    .setConfig(conf)
                    .setInstances(n);
            vertx.deployVerticle("chapter2.options.SampleVerticle", opts);
        }
    }
}