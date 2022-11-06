package chapter5_promises_futures.future;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * The code of the main method that deploys verticles is the same as in the callbacks variant, except that
 * we deploy a different CollectorService verticle.
 *
 * @author sm@creativefusion.net
 */
public class Main {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        vertx.deployVerticle("chapter5_promises_futures.sensor.HeatSensor",
                new DeploymentOptions().setConfig(new JsonObject()
                        .put("http.port", 3000)));

        vertx.deployVerticle("chapter5_promises_futures.sensor.HeatSensor",
                new DeploymentOptions().setConfig(new JsonObject()
                        .put("http.port", 3001)));

        vertx.deployVerticle("chapter5_promises_futures.sensor.HeatSensor",
                new DeploymentOptions().setConfig(new JsonObject()
                        .put("http.port", 3002)));

        vertx.deployVerticle("chapter5_promises_futures.snapshot.SnapshotService");
        vertx.deployVerticle("chapter5_promises_futures.future.CollectorService");
    }
}