import Shared.Messages.*;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;

public class ChannelCreator extends AbstractActor {
    @Override
    public Receive createReceive() {
        return receiveBuilder()
        .match(JoinMessage.class, joinMessage -> {

            // create the channel
            ActorRef channelToJoin = getContext().actorOf(Props.create(ChannelActor.class, joinMessage.channelName), joinMessage.channelName);
                    //.withMailbox("akka.dispatch.UnboundedMailbox"));

            // After creating a new channel, tell it that the user who created it should be the owner
            joinMessage.userMode = UserMode.OWNER;

            channelToJoin.forward(joinMessage, getContext());

        }).build();
    }

    String test;

    @Override
    public void preStart() {
        test = "hi";
    }
}
