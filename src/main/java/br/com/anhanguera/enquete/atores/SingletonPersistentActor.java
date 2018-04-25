package br.com.anhanguera.enquete.atores;

import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.persistence.AbstractPersistentActor;
import akka.persistence.SnapshotOffer;
import br.com.anhanguera.enquete.dominio.Enquete;
import scala.util.Left;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


public class SingletonPersistentActor extends AbstractPersistentActor {

    public static Props props() {
        return Props.create(SingletonPersistentActor.class);
    }

    private EnqueteState state = new EnqueteState();

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    @Override
    public Receive createReceiveRecover() {
        return receiveBuilder()
                .match(Inserted.class, state::update)
                .match(SnapshotOffer.class, ss -> state = (EnqueteState) ss.snapshot())
                .build();
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Insert.class, this::inserirEnquete)
                .build();
    }

    private void inserirEnquete(Insert msg){
        final Enquete data = msg.enquete;
        final Inserted evt = new Inserted(data);

        getSender().tell(new Left<>(evt.enquete), getSender());

        persist(evt, (Inserted e) -> {
            state.update(e);
            log.info("---------- Enquete inserida, estado atual"+ state);
            getSender().tell(e, getSelf());
        });
    }

    @Override
    public String persistenceId() {
        return "singleton";
    }

    public static class EnqueteState implements Serializable{
        private static final long serialVersionUID = 1L;

        private Map<Long,Enquete> enquetes = new HashMap<>();

        public EnqueteState(){
        }

        public EnqueteState(Map<Long,Enquete> enquetes){
            this.enquetes = enquetes;
        }

        public EnqueteState copy(){
            return new EnqueteState(new HashMap<>(enquetes));
        }

        public void update(Inserted inserted){
            System.out.println("Castrando enquente: "+inserted.getEnquete());
            try {
                if(inserted.getEnquete().getId() != null
                        && !this.enquetes.containsKey(inserted.getEnquete().getId())) {
                    this.enquetes.put(inserted.getEnquete().getId(), inserted.getEnquete());
                }
            }catch (Exception e){
                e.printStackTrace();
            }

        }

        @Override
        public String toString() {
            return "EnqueteState{" +
                    "enquetes=" + enquetes +
                    '}';
        }
    }


    public static class Insert implements Serializable{
        private Enquete enquete;
        public Insert(Enquete enquete) {
            this.enquete = enquete;
        }
        public Enquete getEnquete() {
            return enquete;
        }
    }

    public static class Inserted implements Serializable{
        private Enquete enquete;
        public Inserted(Enquete enquete) {
            this.enquete = enquete;
        }
        public Enquete getEnquete() {
            return enquete;
        }
    }


}



