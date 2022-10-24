package chapter2_verticles.deploy;

import io.vertx.core.Vertx;

/**
 * @author sm@creativefusion.net
 */
public class Main {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new Deployer());
    }
}
