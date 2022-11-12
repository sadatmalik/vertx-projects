package chapter6;

import io.vertx.core.AbstractVerticle;
import io.vertx.serviceproxy.ServiceBinder;

/**
 * @author sm@creativefusion.net
 */
public class DataVerticle extends AbstractVerticle {

    @Override
    public void start() {
        new ServiceBinder(vertx)
                .setAddress("sensor.data-service")
                .register(SensorDataService.class, SensorDataService.create(vertx));
    }
}

