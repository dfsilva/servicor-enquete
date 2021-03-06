package br.com.anhanguera.enquete;

import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.server.Route;
import akka.stream.ActorMaterializer;
import br.com.anhanguera.enquete.controladores.EnqueteRouter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.IOException;

import static akka.http.javadsl.server.Directives.*;

public class EnqueteServer {

    public static Route routes() {
        return route(
                path("", () ->
                        getFromResource("web/index.html")
                ),
                EnqueteRouter.routes()
        );
    }

    public static void main(String[] args) throws IOException {

        Config config = ConfigFactory.load();
        ActorSystem system = ActorSystem.create("enquete-server", config);

        final ActorMaterializer materializer = ActorMaterializer.create(system);
        final ConnectHttp host = ConnectHttp.toHost("127.0.0.1", 8080);

        Http.get(system).bindAndHandle(routes()
                .flow(system, materializer), host, materializer);

        System.out.println("Pressione ENTER para finalizar...");
        System.in.read();
        system.terminate();
    }

}
