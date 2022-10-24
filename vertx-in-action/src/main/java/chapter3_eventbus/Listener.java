package chapter3_eventbus;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;

/**
 * Listener monitors new temperature measures and logs them using SLF4J.
 *
 * We do not take advantage of message headers in this example, but it is possible to use them for any metadata that
 * does not belong to the message body. A common header is that of an “action,” to help receivers know what the message
 * is about.
 *
 * @author sm@creativefusion.net
 */
public class Listener extends AbstractVerticle {

    private final Logger logger = LoggerFactory.getLogger(Listener.class);

    private final DecimalFormat format = new DecimalFormat("#.##");
    // We don’t need the full double value, so we format all temperatures to two-decimal string representations

    @Override
    public void start() {
        EventBus bus = vertx.eventBus();
        // The consumer method allows subscribing to messages and a callback handles all event-bus messages
        bus.<JsonObject>consumer("sensor.updates", msg -> {
            JsonObject body = msg.body();
            String id = body.getString("id");
            String temperature = format.format(body.getDouble("temp"));
            logger.info("{} reports a temperature ~{}C", id, temperature);
        });
    }
}