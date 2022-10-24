package chapter3_eventbus.local;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;

/**
 * http GET localhost:8080/sse (httpie)
 * localhost:8080/ (browser)
 *
 * @author sm@creativefusion.net
 */
public class Main {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle("chapter3_eventbus.HeatSensor", new DeploymentOptions().setInstances(4));
        vertx.deployVerticle("chapter3_eventbus.Listener");
        vertx.deployVerticle("chapter3_eventbus.SensorData");
        vertx.deployVerticle("chapter3_eventbus.HttpServer");
    }

}