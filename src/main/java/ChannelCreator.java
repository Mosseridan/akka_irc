import Shared.Messages.BroadcastMessage;
import Shared.Messages.JoinMessage;
import Shared.Messages.NewChannelCreatedMessage;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;

public class ChannelCreator extends AbstractActor {
    @Override
    public Receive createReceive() {
        return receiveBuilder()
        .match(JoinMessage.class, joinMessage -> {
            ActorSelection sel = getContext().actorSelection(joinMessage.channelName);
            ActorRef channelToJoin = HelperFunctions.getActorRefBySelection(sel);

            if (channelToJoin == null) {
                channelToJoin = getContext().actorOf(Props.create(ChannelActor.class, joinMessage.channelName)
                        .withMailbox("akka.dispatch.UnboundedMailbox"));

            }

            channelToJoin.forward(joinMessage, getContext());
        })
                .build();
    }
}
