package br.com.anhanguera.enquete.atores;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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
	
	private ActorRef bancoDadosReplicator = DistributedData.get(context().system()).replicator();
    private final Key<ORSet<String>> enquetesKey = ORSetKey.create("enquetes_key");

    private final Replicator.WriteConsistency estrategiaEscrita = new Replicator.WriteAll(Duration.create(3, TimeUnit.SECONDS));
    private final Replicator.ReadConsistency estrategiaLeitura = new Replicator.ReadAll(Duration.create(3, TimeUnit.SECONDS));

	//Trata todas as mensagens enviadas para este ator
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(CadastrarEnquete.class, this::cadastrarEnquete)
				.match(Replicator.GetSuccess.class, this::eUmResponseGetEnquetes,
                        this::recebeuGetSucesso)
                .match(Replicator.UpdateResponse.class, updateResponse -> {
                    log().info("Atualizacao de uma acao de update");
                    log().info(updateResponse.toString());
					bancoDadosReplicator.tell(new Replicator.Get<>(enquetesKey, estrategiaLeitura, Optional.of(sender())), getSelf());

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

	private boolean eUmResponseGetEnquetes(Replicator.GetResponse response){
        return response.key().equals(enquetesKey)
                && (response.getRequest().orElse(null) instanceof ActorRef);
    }

    private void recebeuGetSucesso(Replicator.GetSuccess<ORSet<String>> response){
        log().info("Valores no banco de dados: {}", response.dataValue().getElements());
		List<Enquete> retorno = new ArrayList<>();

		ORSet<String> valores = response.dataValue();

		for(String enqueteStr : valores.getElements()){
			try {
				retorno.add(new ObjectMapper().readValue(enqueteStr, Enquete.class));
			} catch (IOException e) {
				log().error("Erro ao deserializar enquete", e);
			}
		}

		responderPara.tell(retorno, getSelf());
    }
	
	private void cadastrarEnquete(CadastrarEnquete envelope) {
		log().info("Cadastrando Enquete: "+ envelope);
		responderPara = getSender();
		
		Enquete enquete = envelope.enquete;
		enquete.setId(new Date().getTime());
	
		try {
			String enqueteStr = new ObjectMapper().writeValueAsString(enquete);
			Replicator.Update<ORSet<String>> update = new Replicator.Update<ORSet<String>>(
					enquetesKey,
	                ORSet.create(),
	                estrategiaEscrita,
	                atual -> atual.add(no, enqueteStr)
	        );

		    bancoDadosReplicator.tell(update, getSelf());
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
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

}
