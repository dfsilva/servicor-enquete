package br.com.anhanguera.enquete;

import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.cluster.singleton.ClusterSingletonManager;
import akka.cluster.singleton.ClusterSingletonManagerSettings;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.server.Route;
import akka.stream.ActorMaterializer;
import br.com.anhanguera.enquete.atores.SingletonPersistentActor;
import br.com.anhanguera.enquete.controladores.EnqueteRouter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.io.IOException;

import static akka.http.javadsl.server.Directives.*;

public class EnqueteServer {

    public static Route routes(ActorSystem system) {
        return route(
                path("", () ->
                        getFromResource("web/index.html")
                ),
                EnqueteRouter.routes(system)
        );
    }

    public static void main(String[] args) throws IOException {

        Config config = ConfigFactory.load();
        ActorSystem system = ActorSystem.create("enquete-server", config);

        final ActorMaterializer materializer = ActorMaterializer.create(system);
        int httpPort = Integer.parseInt(System.getenv("HTTP_PORT"));
        String httpHost = System.getenv("CLUSTER_IP");
        final ConnectHttp host = ConnectHttp.toHost(httpHost, httpPort);

        Http.get(system).bindAndHandle(routes(system)
                .flow(system, materializer), host, materializer);

        system.actorOf(
                ClusterSingletonManager.props(
                        SingletonPersistentActor.props(),
                        PoisonPill.getInstance(),
                        ClusterSingletonManagerSettings.create(system)
                ),"enquetePersistence");

        System.out.println("Pressione ENTER para finalizar...");
        System.in.read();
        system.terminate();
    }

}
