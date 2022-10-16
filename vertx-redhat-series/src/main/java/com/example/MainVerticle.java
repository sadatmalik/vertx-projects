package com.example;

import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.Router;

/**
 * @author sm@creativefusion.net
 */
public class MainVerticle extends AbstractVerticle {

    @Override
    public void start() throws Exception {
        Router router = Router.router(vertx);
        router.get("/api/v1/hello").handler(ctx -> ctx.request().response().end("Hello Vertx World!"));
        router.get("/api/v1/hello/:name").handler(ctx -> {
            String name = ctx.pathParam("name");
            ctx.request().response().end(String.format("Hello %s!", name));
        });
        vertx.createHttpServer()
                .requestHandler(router)
                .listen(8080);
    }
}
