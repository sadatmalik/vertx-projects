package chapter5_promises_futures.snapshot;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The HTTP request handler expects an HTTP POST request, extracts the body using a body handler, and logs the
 * received data.
 *
 * @author sm@creativefusion.net
 */
public class SnapshotService extends AbstractVerticle {

    private final Logger logger = LoggerFactory.getLogger(SnapshotService.class);

    @Override
    public void start() {
        vertx.createHttpServer()
                .requestHandler(req -> {
                    if (badRequest(req)) {
                        req.response().setStatusCode(400).end();
                    }
                    req.bodyHandler(buffer -> {
                        logger.info("Lastest temperatures: {}", buffer.toJsonObject().encodePrettily());
                        req.response().end();
                    });
                })
                .listen(config().getInteger("http.port", 4000));
    }

    private boolean badRequest(HttpServerRequest req) {
        return !req.method().equals(HttpMethod.POST) ||
                !"application/json".equals(req.getHeader("Content-Type"));
    }
}