package chapter5_promises_futures.reactivex.intro;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;

import java.util.concurrent.TimeUnit;

/**
 * A first RxJava example.
 *
 * @author sm@creativefusion.net
 */
public class Intro {

    public static void main(String[] args) throws InterruptedException {

        // This is an observable of a predefined sequence
        // We map to a string, We transform the string, For each item, we print to the standard output

        // The just factory method creates an Observable<Integer> source. We then use two map operators
        // to transform the stream. The first one converts from an Observable<Integer> to an Observable<String>.
        // The second one prepends the @ character to each item. Finally, subscribe performs a subscription
        // where System.out.println is called for each item.

        Observable.just(1, 2, 3)
                .map(Object::toString)
                .map(s -> "@" + s)
                .subscribe(System.out::println);


        // Sources may emit errors, in which case the subscriber can be notified

        // The observable of string values will emit one error. The map operator will never be called, since it
        // operates only on values, not errors. We can see that subscribe now has two parameters; the second one
        // is the callback to process errors. In this example, we just print the stack trace, but in networked
        // applications, for example, we would do error recovery.

        Observable.<String>error(() -> new RuntimeException("Woops"))
                .map(String::toUpperCase)
                .subscribe(System.out::println, Throwable::printStackTrace);


        // Once a subscription has been made, zero or many items are emitted. Then the stream terminates with
        // either an error or a notification that it has completed.

        // This example shows us the form of subscribe where all events can be handled: an event, an error, and
        // the completion of the stream. The example also shows further operators:

        // - doOnSubscribe and doOnNext are actions (with potential side effects) that can be triggered as items
        // pass along the stream.
        // - delay allows delaying when events start to be emitted further down the stream.
        // - buffer groups events (into lists), so here we get events in pairs.

        Observable
                .just("--", "this", "is", "--", "a", "sequence", "of", "items", "!")
                .doOnSubscribe(d -> System.out.println("Subscribed!"))
                .delay(5, TimeUnit.SECONDS)
                .filter(s -> !s.startsWith("--"))
                .doOnNext(x -> System.out.println("doOnNext: " + x))
                .map(String::toUpperCase)
                .buffer(2)
                .subscribe(
                        pair -> System.out.println("next: " + pair),
                        Throwable::printStackTrace,
                        () -> System.out.println("~Done~"));


        Single<String> s1 = Single.just("foo");
        Single<String> s2 = Single.just("bar");
        Flowable<String> m = Single.merge(s1, s2);
        m.subscribe(System.out::println);

        Thread.sleep(10_000);
    }
}