import com.example.HelloVerticle;
import com.example.MainVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;

/**
 * @author sm@creativefusion.net
 */
public class Main {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        DeploymentOptions options = new DeploymentOptions()
                .setWorker(true)
                .setInstances(4);
        vertx.deployVerticle(HelloVerticle.class, options);
        vertx.deployVerticle(new MainVerticle());
    }
}
