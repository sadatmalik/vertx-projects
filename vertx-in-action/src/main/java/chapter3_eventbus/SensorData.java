package chapter3_eventbus;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * SensorData keeps a record of the latest observed values for each sensor. It also supports request-response
 * communications: sending a message to sensor.average triggers a computation of the average based on the latest data,
 * and the result is sent back as a response.
 *
 * @author sm@creativefusion.net
 */
public class SensorData extends AbstractVerticle {

    private final HashMap<String, Double> lastValues = new HashMap<>();

    @Override
    public void start() {
        // The start method only declares two event-bus destination handlers
        EventBus bus = vertx.eventBus();
        bus.consumer("sensor.updates", this::update);
        bus.consumer("sensor.average", this::average);
    }

    private void update(Message<JsonObject> message) {
        // When a new measurement is being received, we extract the data from the JSON body
        JsonObject json = message.body();
        lastValues.put(json.getString("id"), json.getDouble("temp"));
    }

    private void average(Message<JsonObject> message) {
        double avg = lastValues.values().stream()
                .collect(Collectors.averagingDouble(Double::doubleValue));
        JsonObject json = new JsonObject().put("average", avg);
        message.reply(json); // The reply method is used to reply to a message
    }
}
