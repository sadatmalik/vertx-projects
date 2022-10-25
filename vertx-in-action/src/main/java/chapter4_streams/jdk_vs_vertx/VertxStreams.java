package chapter4_streams.jdk_vs_vertx;

import io.vertx.core.Vertx;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;

/**
 * @author sm@creativefusion.net
 */
public class VertxStreams {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        OpenOptions opts = new OpenOptions().setRead(true);
        vertx.fileSystem().open("pom.xml", opts, ar -> {
            if (ar.succeeded()) {
                AsyncFile file = ar.result();
                file.handler(System.out::println)
                        .exceptionHandler(Throwable::printStackTrace)
                        .endHandler(done -> {
                            System.out.println("\n--- DONE");
                            vertx.close();
                        });
            } else {
                ar.cause().printStackTrace();
            }
        });
    }
}
