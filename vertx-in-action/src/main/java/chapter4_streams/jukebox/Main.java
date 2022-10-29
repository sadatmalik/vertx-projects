package chapter4_streams.jukebox;

import io.vertx.core.Vertx;

/**
 * Jukebox provides the main music-streaming logic and HTTP server interface for music players to connect to.
 *
 * NetControl provides a text-based TCP protocol for remotely controlling the jukebox application.
 *
 * @author sm@creativefusion.net
 */
public class Main {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new Jukebox());
        vertx.deployVerticle(new NetControl());
    }
}