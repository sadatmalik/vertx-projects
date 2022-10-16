package com.example;

import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

/**
 * @author sm@creativefusion.net
 */
public class MainVerticle extends AbstractVerticle {
    @Override
    public void start() throws Exception {
        Router router = Router.router(vertx);
        router.get("/api/v1/hello").handler(this::helloVertx);
        router.get("/api/v1/hello/:name").handler(this::helloVertxName);

        int httpPort;
        try {
            httpPort = Integer.parseInt(System.getProperty("http.port", "8080"));
        } catch (NumberFormatException nfe) {
            httpPort = 8080;
        }
        vertx.createHttpServer().requestHandler(router).listen(httpPort);
    }

    private void helloVertx(RoutingContext ctx) {
        vertx.eventBus().request("hello.vertx.addr", "", reply -> {
            //ctx.request().response().end("Hello Vertx World!");
            ctx.request().response().end((String) reply.result().body());
        });
    }

    private void helloVertxName(RoutingContext ctx) {
        String name = ctx.pathParam("name");
        vertx.eventBus().request("hello.named.addr", name, reply -> {
//            ctx.request().response().end(String.format("Hello %s!", name));
            ctx.request().response().end(String.format((String) reply.result().body()));
        });


    }

}
