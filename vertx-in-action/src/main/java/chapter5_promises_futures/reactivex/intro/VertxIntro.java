package chapter5_promises_futures.reactivex.intro;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.RxHelper;
import io.vertx.reactivex.core.AbstractVerticle;

import java.util.concurrent.TimeUnit;

/**
 * Opens a classic HTTP server that replies Ok to any request. The RxJava variant of AbstractVerticle has an
 * rxStart (and rxStop) method that notifies of deployment success. The verticle has been successfully deployed
 * when the HTTP server has started, so we return a Completable object.
 *
 * The observable that emits events every second behaves essentially as a Vert.x timer would. Several operator
 * methods in the RxJava APIs accept a scheduler object, because they need to defer asynchronous tasks. By default,
 * they call back from an internal worker-thread pool that they manage, which breaks the Vert.x threading model
 * assumptions. We can always pass a Vert.x scheduler to ensure that events are still being called back on the
 * original context event loop.
 *
 * @author sm@creativefusion.net
 */
public class VertxIntro extends AbstractVerticle {

    // rxStart notifies of deployment success using a Completable rather than a Future
    @Override
    public Completable rxStart() {

        // The scheduler enforces the Vert.x threading model
        Observable
                .interval(1, TimeUnit.SECONDS, RxHelper.scheduler(vertx))
                .subscribe(n -> System.out.println("tick"));

        // uses RxJava variant of listen(port, callback).
        return vertx.createHttpServer()
                .requestHandler(r -> r.response().end("Ok"))
                .rxListen(8080)
                .ignoreElement(); // returns a Completable from a Single
    }

    public static void main(String[] args) {
        Vertx.vertx().deployVerticle(new VertxIntro());
    }
}