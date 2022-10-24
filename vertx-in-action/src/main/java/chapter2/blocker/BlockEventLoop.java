package chapter2.blocker;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;

/**
 * @author sm@creativefusion.net
 */
public class BlockEventLoop extends AbstractVerticle {

    @Override
    public void start() {
        vertx.setTimer(1000, id -> {
            while (true);
        });
    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new BlockEventLoop());
    }
}