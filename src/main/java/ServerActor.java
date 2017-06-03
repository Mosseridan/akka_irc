import Shared.Messages.*;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;

public class ServerActor extends AbstractActor {

    final String channelCreatorPath = "/user/Server/ChannelCreator";
    ActorRef channelCreator;

    @Override
    public void preStart() {
        channelCreator = getContext().actorOf(Props.create(ChannelCreator.class),  "ChannelCreator");
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(ConnectMessage.class, msg -> {
                // Create ServerUserActor in the system
                ActorRef serverUserActor = getContext().actorOf(Props.create(ServerUserActor.class, msg.getUserName(), sender(), channelCreator), "ServerUser" + msg.getUserName());
                sender().tell(new ConnectApprovalMessage(serverUserActor), getSelf());
            })
            .build();
    }

}
