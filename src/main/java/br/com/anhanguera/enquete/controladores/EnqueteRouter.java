package br.com.anhanguera.enquete.controladores;

import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.Route;
import akka.util.Timeout;
import br.com.anhanguera.enquete.atores.EnqueteActor;
import br.com.anhanguera.enquete.dominio.Enquete;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import akka.actor.*;
import static akka.pattern.PatternsCS.ask;

import static akka.http.javadsl.server.Directives.*;
import static akka.http.javadsl.server.Directives.complete;
import static akka.http.javadsl.server.Directives.delete;
import static akka.http.javadsl.unmarshalling.StringUnmarshallers.INTEGER;

public class EnqueteRouter {

	private static List<Enquete> enquetes = new ArrayList<>();

	public static Route routes(ActorSystem system) {
		return pathPrefix("enquete", () -> route(
				get(() -> {
					return complete(StatusCodes.OK, enquetes, Jackson.<List<Enquete>>marshaller());
				}),

				put(() -> entity(Jackson.unmarshaller(Enquete.class), enquete -> {				
					return complete(StatusCodes.OK, enquete, Jackson.<Enquete>marshaller());
				})),
				
				post(() -> entity(Jackson.unmarshaller(Enquete.class), enquete -> {
					ActorRef enqueteActor = system.actorOf(Props.create(EnqueteActor.class));
					CompletionStage<List<Enquete>> enqueteComp = ask(enqueteActor, new EnqueteActor.CadastrarEnquete(enquete),
							new Timeout(Duration.create(5, TimeUnit.SECONDS))).thenApply(r -> {
								System.out.println("resposta");
								System.out.println(r);
								return (List<Enquete>)r;
							});
					return completeOKWithFuture(enqueteComp, Jackson.marshaller());
				})),

				path("alternate", () -> put(() -> entity(Jackson.unmarshaller(Enquete.class), enquete -> {
					ActorRef enqueteActor = system.actorOf(Props.create(EnqueteActor.class));
					// enqueteActor.tell(new
					// EnqueteActor.CadastrarEnquete(enquete),
					// ActorRef.noSender());

					ask(enqueteActor, new EnqueteActor.CadastrarEnquete(enquete),
							new Timeout(Duration.create(5, TimeUnit.SECONDS))).thenApplyAsync(resposta -> {
								System.out.println("resposta");
								return resposta;
							});

					return complete(StatusCodes.OK, enquete, Jackson.<Enquete>marshaller());
				}))), path(INTEGER, petId -> route(delete(() -> {
					Iterator<Enquete> it = enquetes.iterator();
					while (it.hasNext()) {
						Enquete eq = it.next();
						if (eq.getId().equals(petId)) {
							enquetes.remove(eq);
						}
					}
					return complete(StatusCodes.NO_CONTENT);
				})))));

	}
}
