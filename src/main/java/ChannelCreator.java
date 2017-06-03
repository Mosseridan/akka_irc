import Shared.Messages.*;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;

public class ChannelCreator extends AbstractActor {

    @Override
    public Receive createReceive() {
        return receiveBuilder()
        .match(JoinMessage.class, msg -> {
            ActorRef newChannel = getContext().actorOf(Props.create(ChannelActor.class, msg.getChannelName()), msg.getChannelName());
            newChannel.forward(msg, getContext());
        })
        .match(OutgoingKillChannelMessage.class, msg -> {
            ActorSelection sel = getContext().actorSelection(msg.getChannelName());
            ActorRef channelToKill = HelperFunctions.getActorRefBySelection(sel);
            channelToKill.forward(akka.actor.PoisonPill.getInstance(), getContext());
        })
        .build();
    }
}
