package br.com.anhanguera.enquete.atores;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import akka.cluster.singleton.ClusterSingletonProxy;
import akka.cluster.singleton.ClusterSingletonProxySettings;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.cluster.Cluster;
import akka.cluster.ddata.DistributedData;
import akka.cluster.ddata.Key;
import akka.cluster.ddata.ORSet;
import akka.cluster.ddata.ORSetKey;
import akka.cluster.ddata.Replicator;
import akka.http.javadsl.marshallers.jackson.Jackson;
import br.com.anhanguera.enquete.dominio.Enquete;
import scala.concurrent.duration.Duration;

public class EnqueteActor extends AbstractLoggingActor {
	
	private final Cluster no = Cluster.get(context().system());
	
	private ActorRef responderPara;

	private ActorRef persistenceSingleton = getContext().actorOf(
			ClusterSingletonProxy.props(
					"/user/enquetePersistence",
					ClusterSingletonProxySettings.create(getContext().system())));

	//Trata todas as mensagens enviadas para este ator
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(CadastrarEnquete.class, this::cadastrarEnquete)
				.match(ListarEnquetes.class, this::listarEnquetes)
                .match(SingletonPersistentActor.Inserted.class, (msg)->{
                    responderPara.tell(new ArrayList<Enquete>(), getSelf());
                })
				.match(SingletonPersistentActor.Enquetes.class, (msg)->{
					responderPara.tell(msg.getEnquetes(), getSelf());
				})
				.build();
	}

	@Override
	public void preStart() throws Exception {
		super.preStart();
		log().debug("Pre Starting Actor"+ getSelf().path());
	}

	@Override
	public void preRestart(Throwable reason, Optional<Object> message) throws Exception {
		super.preRestart(reason, message);
		log().debug("Pre Restarting "+ getSelf().path());
	}

	private void cadastrarEnquete(CadastrarEnquete envelope) {
		log().info("Cadastrando Enquete: "+ envelope);
		responderPara = getSender();

        Enquete enquete = envelope.enquete;
		enquete.setId(new Date().getTime());
		persistenceSingleton.tell(new SingletonPersistentActor.Insert(enquete), getSelf());
	}

	private void listarEnquetes(ListarEnquetes envelope){
		responderPara = getSender();
		persistenceSingleton.tell(new SingletonPersistentActor.ShowAll(), getSelf());
	}
	
	public static class CadastrarEnquete implements Serializable{
		private static final long serialVersionUID = 1L;
		public final Enquete enquete;
		public CadastrarEnquete(Enquete enquete){
			this.enquete = enquete;
		}
		@Override
		public String toString() {
			return "CadastrarEnquete{" +
					"enquete=" + enquete +
					'}';
		}
	}

	public static class ListarEnquetes implements Serializable{
		private static final long serialVersionUID = 1L;
		public ListarEnquetes() {
		}
	}

}
