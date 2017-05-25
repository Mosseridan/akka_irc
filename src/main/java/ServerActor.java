import Shared.Messages.ConnectMessage;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;

public class ServerActor extends AbstractActor {

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ConnectMessage.class, connMsg -> {

                    ConnectMessage connectMessage = new ConnectMessage();
                    // Create ServerUserActor in the system

                    ActorRef serverUserActor = getContext().actorOf(Props.create(ServerUserActor.class, connMsg.userName), "ServerUser" + connMsg.userName);

                    connectMessage.serverUserActor = serverUserActor;
                    ActorRef clientUserActor = sender();
                    clientUserActor.tell(connectMessage, self());

                    //connectMessage = new ConnectMessage();
                    connectMessage.clientUserActor = clientUserActor;
                    serverUserActor.tell(connectMessage, self());
                })
                .build();
    }

    @Override
    public void preStart() {
        getContext().actorOf(Props.create(ChannelCreator.class),  "ChannelCreator");
    }
}
