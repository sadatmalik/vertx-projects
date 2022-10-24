package com.example;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.*;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;

public class MainVerticle extends AbstractVerticle {
  @Override
  public void start(Promise<Void> start) throws Exception {
    DeploymentOptions options = new DeploymentOptions()
      .setWorker(true)
      .setInstances(4);
    vertx.deployVerticle(HelloVerticle.class, options);
    //vertx.deployVerticle("Hello.groovy");
    //vertx.deployVerticle("Hello.js");

    Handler<AsyncResult<Void>> dbMigrationResultHandler = result -> this.handleMigrationResult(start, result);
    vertx.executeBlocking(this::doDatabaseMigrations, dbMigrationResultHandler);

    Router router = Router.router(vertx);
//        router.route().handler(ctx -> {
//           String authToken = ctx.request().getHeader("AUTH_TOKEN");
//           if ("mySuperSecretAuthToken".equals(authToken)) {
//               ctx.next();
//           } else {
//               ctx.response().setStatusCode(401).setStatusMessage("UNAUTHORIZED").end();
//           }
//        });
    SessionStore store = LocalSessionStore.create(vertx);
    router.route().handler(LoggerHandler.create());
    router.route().handler(SessionHandler.create(store));
    router.route().handler(CorsHandler.create("localhost"));
    router.route().handler(CSRFHandler.create(vertx, "secret"));

    router.get("/api/v1/hello").handler(this::helloVertx);
    router.get("/api/v1/hello/:name").handler(this::helloVertxName);
    router.route().handler(StaticHandler.create("web"));

    doConfig(start, router);
  }

  void handleMigrationResult(Promise<Void> start, AsyncResult<Void> result) {
    if (result.failed()) {
      start.fail(result.cause());
    }
  }

  void doDatabaseMigrations(Promise<Void> promise) {
    Flyway flyway = Flyway.configure()
      .dataSource("jdbc:postgresql://127.0.0.1:5432/todo", "postgres", "introduction")
      .load();
    try {
      flyway.migrate();
      promise.complete();
    } catch (FlywayException fe) {
      promise.fail(fe);
    }
  }

  private void doConfig(Promise<Void> start, Router router) {
    ConfigStoreOptions defaultConfig = new ConfigStoreOptions()
      .setType("file")
      .setFormat("json")
      .setConfig(new JsonObject().put("path", "config.json"));

    ConfigRetrieverOptions opts = new ConfigRetrieverOptions()
      .addStore(defaultConfig);

    ConfigRetriever cfgRetriever = ConfigRetriever.create(vertx, opts);

    Handler<AsyncResult<JsonObject>> handler =
      asyncResult -> this.handleConfigResults(start, router, asyncResult);

    cfgRetriever.getConfig(handler);
  }

  private void handleConfigResults(Promise<Void> start, Router router, AsyncResult<JsonObject> asyncResult) {
    if (asyncResult.succeeded()) {
      JsonObject config = asyncResult.result();
      JsonObject http = config.getJsonObject("http");
      int httpPort = http.getInteger("port");
      vertx.createHttpServer().requestHandler(router).listen(httpPort);
      start.complete();
    } else {
      start.fail("Unable to load configuration.");
    }
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
