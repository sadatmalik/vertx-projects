package chapter5_promises_futures.future;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.codec.BodyCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author sm@creativefusion.net
 */
public class CollectorService extends AbstractVerticle {

    private final Logger logger =
            LoggerFactory.getLogger(chapter5_promises_futures.future.CollectorService.class);
    private WebClient webClient;

    @Override
    public void start(Promise<Void> promise) {
        webClient = WebClient.create(vertx);
        vertx.createHttpServer()
                .requestHandler(this::handleRequest)
                .listen(8080)
                .onFailure(promise::fail)
                .onSuccess(ok -> {
                    System.out.println("http://localhost:8080/");
                    promise.complete();
                });
    }

    /*
     * use CompositeFuture to make one future out of several. The all static method results in a future
     * that is completed when all futures are completed, and that fails when any future has failed. There
     * are also any and join methods that have different semantics.
     *
     * Once all temperatures have been successfully received, the call to flatMap sends data to the snapshot
     * service, which is an asynchronous operation.
     */
    private void handleRequest(HttpServerRequest request) {
        CompositeFuture.all(
                        fetchTemperature(3000),
                        fetchTemperature(3001),
                        fetchTemperature(3002))
                // chain with another asynchronous method:
                .flatMap(this::sendToSnapshot)
                .onSuccess(data -> request.response()
                        .putHeader("Content-Type", "application/json")
                        .end(data.encode()))
                .onFailure(err -> {
                    logger.error("Something went wrong", err);
                    request.response().setStatusCode(500).end();
                });
    }

    /*
     * This code is similar to that of fetchTemperature because we use a method of WebClient that returns
     * a Future.
     */
    private Future<JsonObject> sendToSnapshot(CompositeFuture temps) {
        List<JsonObject> tempData = temps.list();
        JsonObject data = new JsonObject()
                .put("data", new JsonArray()
                        .add(tempData.get(0))
                        .add(tempData.get(1))
                        .add(tempData.get(2)));
        return webClient
                .post(4000, "localhost", "/")
                .expect(ResponsePredicate.SC_SUCCESS)
                // Future-based variant:
                .sendJson(data)
                .map(response -> data);
    }

    /*
     * returns a future of a JsonObject, and to achieve that we use the future-returning variant of the
     * WebClient send method, and then map the result to extract just the JSON data.
     */
    private Future<JsonObject> fetchTemperature(int port) {
        return webClient
                .get(port, "localhost", "/")
                .expect(ResponsePredicate.SC_SUCCESS)
                .as(BodyCodec.jsonObject())
                .send()
                .map(HttpResponse::body); // Extract and return just the body
    }
}