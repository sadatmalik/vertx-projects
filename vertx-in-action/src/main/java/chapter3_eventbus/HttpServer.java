package chapter3_eventbus;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.TimeoutStream;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;

/**
 * HttpServer exposes the HTTP server and serves the web interface. It pushes new values to its clients whenever a new
 * temperature measurement has been observed, and it periodically asks for the current average and updates all the
 * connected clients.
 *
 * The SSE protocol is text-based, and each event is a block with an event type and some data separated by an empty
 * line so 2 successive events looks like this:
 * ```
 * event: foo
 * data: bar
 *
 * event: bar
 * data: 123
 * ```
 *
 * @author sm@creativefusion.net
 */
public class HttpServer extends AbstractVerticle {

    @Override
    public void start() {
        vertx.createHttpServer()
                .requestHandler(this::handler)
                .listen(config().getInteger("port", 8080));
    }

    // note: the vertx-web module provides a nicer router API for conveniently declaring handlers
    private void handler(HttpServerRequest request) {
        if ("/".equals(request.path())) {
            // the sendFile method allows the content of any local file to be streamed to the client. This closes the
            // connection automatically
            request.response().sendFile("index.html");
        } else if ("/sse".equals(request.path())) {
            // Server-sent events will use the /sse resource
            sse(request);
        } else {
            request.response().setStatusCode(404);
        }
    }

    private void sse(HttpServerRequest request) {
        HttpServerResponse response = request.response();
        response
                .putHeader("Content-Type", "text/event-stream") // MIME type for server sent events
                .putHeader("Cache-Control", "no-cache") // since the live stream prevent browser caching
                .setChunked(true);

        MessageConsumer<JsonObject> consumer = vertx.eventBus().consumer("sensor.updates");
        consumer.handler(msg -> {
            response.write("event: update\n"); // sending event blocks is just sending text
            response.write("data: " + msg.body().encode() + "\n\n");
        });

        TimeoutStream ticks = vertx.periodicStream(1000); // We update the average every second
        ticks.handler(id -> {
            vertx.eventBus().<JsonObject>request("sensor.average", "", reply -> {
                if (reply.succeeded()) {
                    response.write("event: average\n");
                    response.write("data: " + reply.result().body().encode() + "\n\n");
                }
            });
        });

        response.endHandler(v -> {
            consumer.unregister();
            ticks.cancel();
        });
        // When the client disconnects (or refreshes the page) we need to unregister the event-bus message consumer
        // and cancel the periodic task that computes averages

    }
}