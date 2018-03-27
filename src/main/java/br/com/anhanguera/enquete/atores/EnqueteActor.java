package br.com.anhanguera.enquete.atores;

import java.io.Serializable;
import java.util.Date;
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
    private final Key<ORSet<Enquete>> enquetesKey = ORSetKey.create("enquetes_key");

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
                    //responderPara.tell(updateResponse, getSelf());
                    
                    bancoDadosReplicator.tell(new Replicator.Get<>(enquetesKey, estrategiaLeitura, Optional.of(sender())), getSelf());
                })
				.build();
	}
	
	
	private boolean eUmResponseGetEnquetes(Replicator.GetResponse response){
        return response.key().equals(enquetesKey)
                && (response.getRequest().orElse(null) instanceof ActorRef);
    }

    private void recebeuGetSucesso(Replicator.GetSuccess<ORSet<String>> response){
        log().info("Valores no banco de dados: {}", response.dataValue().getElements());

    }
	
	private void cadastrarEnquete(CadastrarEnquete envelope) {
		//logica de negocio para cadastrar uma enquete
		
		responderPara = getSender();
		
		Enquete enquete = envelope.enquete;
		enquete.setId(new Date().getTime());

		Replicator.Update<ORSet<Enquete>> update = new Replicator.Update<ORSet<Enquete>>(
				enquetesKey,
                ORSet.create(),
                estrategiaEscrita,
                atual -> atual.add(no, enquete)
        );

	    bancoDadosReplicator.tell(update, getSelf());
		
	}
	
	public static class CadastrarEnquete implements Serializable{
		
		private static final long serialVersionUID = 1L;
		public final Enquete enquete;
		
		public CadastrarEnquete(Enquete enquete){
			this.enquete = enquete;
		}
		
	}

}
