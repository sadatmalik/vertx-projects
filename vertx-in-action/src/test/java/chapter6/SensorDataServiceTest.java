package chapter6;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Percentage.withPercentage;

/**
 * The prepare method is executed before each test case, to prepare the test environment. We use it here to
 * deploy the DataVerticle verticle and then fetch the service proxy and store it in the dataService field.
 *
 * Since deploying a verticle is an asynchronous operation, the prepare method is injected with a Vertx context
 * and a VertxTestContext object to notify when it has completed.
 */
@ExtendWith(VertxExtension.class)
class SensorDataServiceTest {

    private SensorDataService dataService;

    @BeforeEach
    void prepare(Vertx vertx, VertxTestContext ctx) {
        // We deploy the verticle that internally exposes a service, and we expect a successful deployment
        vertx.deployVerticle(new DataVerticle(), ctx.succeeding(id -> {
            dataService = SensorDataService.createProxy(vertx, "sensor.data-service");
            ctx.completeNow(); // We notify that the setup has completed
        }));
    }

    @Test
    void noSensor(VertxTestContext ctx) {
        // A checkpoint is mainly used to ensure that an asynchronous operation passed at a certain line
        Checkpoint failsToGet = ctx.checkpoint();
        Checkpoint zeroAvg = ctx.checkpoint();

        // failing is a helper for Handler<AsyncResult>, and verify wraps assertions
        dataService.valueFor("abc", ctx.failing(err -> ctx.verify(() -> {
            assertThat(err.getMessage()).startsWith("No value has been observed");
            failsToGet.flag();
        })));

        dataService.average(ctx.succeeding(data -> ctx.verify(() -> {
            double avg = data.getDouble("average");
            assertThat(avg).isCloseTo(0.0d, withPercentage(1.0d));
            zeroAvg.flag();
        })));
    }


    /*
     * This test simulates two sensors with identifiers abc and def by sending fake sensor data updates over the
     * event bus, just like a sensor would do. We then have determinism in our assertions, and we can check the
     * behavior for both valueFor and average methods.
     */
    @Test
    void withSensors(Vertx vertx, VertxTestContext ctx) {
        Checkpoint getValue = ctx.checkpoint();
        Checkpoint goodAvg = ctx.checkpoint();

        // Messages to mock sensors
        JsonObject m1 = new JsonObject().put("id", "abc").put("temp", 21.0d);
        JsonObject m2 = new JsonObject().put("id", "def").put("temp", 23.0d);

        // We send the messages
        vertx.eventBus()
                .publish("sensor.updates", m1)
                .publish("sensor.updates", m2);

        dataService.valueFor("abc", ctx.succeeding(data -> ctx.verify(() -> {
            assertThat(data.getString("sensorId")).isEqualTo("abc");
            assertThat(data.getDouble("value")).isEqualTo(21.0d);
            getValue.flag();
        })));

        dataService.average(ctx.succeeding(data -> ctx.verify(() -> {

            // AssertJ has assertions for floating-point values with error margins

            assertThat(data.getDouble("average")).isCloseTo(22.0, withPercentage(1.0d));
            goodAvg.flag();
        })));
    }
}