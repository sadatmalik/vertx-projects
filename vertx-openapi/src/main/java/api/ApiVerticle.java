package api;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * @author sm@creativefusion.net
 */
public class ApiVerticle extends AbstractVerticle {

    private HttpServer server;

    final List<JsonObject> pets = new ArrayList<>(Arrays.asList(
            new JsonObject().put("id", 1).put("name", "Fufi").put("tag", "ABC"),
            new JsonObject().put("id", 2).put("name", "Garfield").put("tag", "XYZ"),
            new JsonObject().put("id", 3).put("name", "Puffa")
    ));

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new ApiVerticle());
    }

    @Override
    public void start(Promise<Void> startPromise) {
        // (2)
        // Something went wrong during router factory initialization
        RouterBuilder.create(this.vertx, "petstore.yaml")
                .onSuccess(routerBuilder -> { // (1)
                    // You can start building the router using routerBuilder
                    setRouteHandlers(routerBuilder);
                    Router router = routerBuilder.createRouter();
                    setErrorHandlers(router);
                    server = vertx.createHttpServer(new HttpServerOptions().setPort(8080).setHost("localhost")); // (5)
                    server.requestHandler(router).listen(); // (6)
                }).onFailure(startPromise::fail);
    }

    private void setRouteHandlers(RouterBuilder routerBuilder) {
        setListPetsHandler(routerBuilder);
        setCreatePetsHandler(routerBuilder);
        setShowPetsByIdHandler(routerBuilder);
    }

    private void setErrorHandlers(Router router) {
        setNotFoundErrorHandler(router);
        setValidationErrorHandler(router);
    }

    private void setListPetsHandler(RouterBuilder routerBuilder) {
        routerBuilder.operation("listPets").handler(routingContext ->
                routingContext
                        .response() // (1)
                        .setStatusCode(200)
                        .putHeader(HttpHeaders.CONTENT_TYPE, "application/json") // (2)
                        .end(new JsonArray(getAllPets()).encode()) // (3)
        );
    }

    private void setCreatePetsHandler(RouterBuilder routerBuilder) {
        routerBuilder.operation("createPets").handler(routingContext -> {
            RequestParameters params = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY); // (1)
            JsonObject pet = params.body().getJsonObject(); // (2)
            addPet(pet);
            routingContext
                    .response()
                    .setStatusCode(200)
                    .end(); // (3)
        });
    }

    private void setShowPetsByIdHandler(RouterBuilder routerBuilder) {
        routerBuilder.operation("showPetById").handler(routingContext -> {
            RequestParameters params = routingContext.get("parsedParameters"); // (1)
            Integer id = params.pathParameter("petId").getInteger(); // (2)
            Optional<JsonObject> pet = getAllPets()
                    .stream()
                    .filter(p -> p.getInteger("id").equals(id))
                    .findFirst(); // (3)
            if (pet.isPresent())
                routingContext
                        .response()
                        .setStatusCode(200)
                        .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                        .end(pet.get().encode()); // (4)
            else
                routingContext.fail(404, new Exception("Pet not found")); // (5)
        });
    }

    private void setNotFoundErrorHandler(Router router) {
        router.errorHandler(404, routingContext -> { // (2)
            JsonObject errorObject = new JsonObject() // (3)
                    .put("code", 404)
                    .put("message",
                            (routingContext.failure() != null) ?
                                    routingContext.failure().getMessage() :
                                    "Not Found"
                    );
            routingContext
                    .response()
                    .setStatusCode(404)
                    .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                    .end(errorObject.encode()); // (4)
        });
    }

    private void setValidationErrorHandler(Router router) {
        router.errorHandler(400, routingContext -> {
            JsonObject errorObject = new JsonObject()
                    .put("code", 400)
                    .put("message",
                            (routingContext.failure() != null) ?
                                    routingContext.failure().getMessage() :
                                    "Validation Exception"
                    );
            routingContext
                    .response()
                    .setStatusCode(400)
                    .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                    .end(errorObject.encode());
        });
    }

    @Override
    public void stop() {
        this.server.close();
    }

    private List<JsonObject> getAllPets() {
        return this.pets;
    }

    private void addPet(JsonObject pet) {
        this.pets.add(pet);
    }



}
