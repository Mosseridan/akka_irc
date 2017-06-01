import Shared.Messages.*;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;

import java.util.LinkedList;
import java.util.List;

public class ChannelCreator extends AbstractActor {

    @Override
    public Receive createReceive() {
        return receiveBuilder()
        .match(JoinMessage.class, msg -> { //TODO: change join logic?

            // create the channel
            ActorRef channelToJoin = getContext().actorOf(Props.create(ChannelActor.class, msg.channel), msg.channel);
                    //.withMailbox("akka.dispatch.UnboundedMailbox"));
            sender().tell(msg,self());
            //channelToJoin.forward(joinMessage, getContext());
        }).build();
    }
}
