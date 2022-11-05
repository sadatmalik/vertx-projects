package chapter5_promises_futures.callbacks;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.codec.BodyCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The start method first creates a WebClient instance and then starts an HTTP server on port 8080. The web client
 * class comes from the vertx-web-client module and greatly simplifies making HTTP requests compared to the HTTP client
 * in the Vert.x core APIs. It especially simplifies HTTP body handling and conversions: you can convert a body to
 * plain text, to JSON, or to general-purpose Vert.x buffers.
 *
 * @author sm@creativefusion.net
 */
public class CollectorService extends AbstractVerticle {

    private final Logger logger = LoggerFactory.getLogger(CollectorService.class);
    private WebClient webClient;

    @Override
    public void start() {
        webClient = WebClient.create(vertx);
        vertx.createHttpServer()
                .requestHandler(this::handleRequest)
                .listen(8080);
    }

    private void handleRequest(HttpServerRequest request) {
        List<JsonObject> responses = new ArrayList<>();
        // Note that we use an AtomicInteger here not because of concurrency, but rather because we need an object
        // to increment an integer from the callbacks.
        AtomicInteger counter = new AtomicInteger(0);
        for (int i = 0; i < 3; i++) {
            webClient
                    .get(3000 + i, "localhost", "/")
                    // This predicate triggers an error when the HTTP status code is not in the 2xx range:
                    .expect(ResponsePredicate.SC_SUCCESS)
                    // treats the body as a JSON object:
                    .as(BodyCodec.jsonObject())
                    .send(ar -> {
                        if (ar.succeeded()) {
                            responses.add(ar.result().body());
                        } else {
                            logger.error("Sensor down?", ar.cause());
                        }
                        if (counter.incrementAndGet() == 3) {
                            JsonObject data = new JsonObject()
                                    .put("data", new JsonArray(responses));
                            sendToSnapshot(request, data);
                        }
                    });
        }
    }

    private void sendToSnapshot(HttpServerRequest request, JsonObject data) {
        webClient
                .post(4000, "localhost", "/")
                .expect(ResponsePredicate.SC_SUCCESS)
                .sendJsonObject(data, ar -> {
                    if (ar.succeeded()) {
                        sendResponse(request, data);
                    } else {
                        logger.error("Snapshot down?", ar.cause());
                        request.response().setStatusCode(500).end();
                    }
                });
    }

    private void sendResponse(HttpServerRequest request, JsonObject data) {
        request.response()
                .putHeader("Content-Type", "application/json")
                .end(data.encode());
    }
}