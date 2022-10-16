import io.vertx.core.Vertx;
import com.example.MainVerticle;

/**
 * @author sm@creativefusion.net
 */
public class Main {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MainVerticle());
    }
}
