package chapter3_eventbus;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;

import java.util.Random;
import java.util.UUID;

/**
 * HeatSensor generates temperature measures at non-fixed rates and publishes them to subscribers to the sensor.updates
 * destination. Each verticle has a unique sensor identifier.
 *
 * @author sm@creativefusion.net
 */
public class HeatSensor extends AbstractVerticle {

    private final Random random = new Random();
    private final String sensorId = UUID.randomUUID().toString();
    private double temperature = 21.0;

    @Override
    public void start() {
        scheduleNextUpdate();
    }

    private void scheduleNextUpdate() {
        // Updates are scheduled with a random delay between one and six seconds
        vertx.setTimer(random.nextInt(5000) + 1000, this::update);
    }

    private void update(long timerId) {
        temperature = temperature + (delta() / 10);
        JsonObject payload = new JsonObject()
                .put("id", sensorId)
                .put("temp", temperature);
        // publish sends a message to subscribers
        vertx.eventBus().publish("sensor.updates", payload);
        scheduleNextUpdate();
    }

    private double delta() {
        // computes a random positive or negative value to slightly modify the current temperature
        if (random.nextInt() > 0) {
            return random.nextGaussian();
        } else {
            return -random.nextGaussian();
        }
    }
}