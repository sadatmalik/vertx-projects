package com.example;

import io.vertx.core.AbstractVerticle;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * @author sm@creativefusion.net
 */
@Slf4j
public class HelloVerticle extends AbstractVerticle {

    String verticleId = UUID.randomUUID().toString();

    @Override
    public void start() throws Exception {
        log.info("started hello verticle");
        vertx.eventBus().consumer("hello.vertx.addr", msg -> {
            msg.reply("Hello Vertx World!");
        });
        vertx.eventBus().consumer("hello.named.addr", msg -> {
            String name = (String) msg.body();
            msg.reply(String.format("Hello %s, from %s!", name, verticleId));
        });
    }
}
