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

//        })
//
//        .match(GetChannelListMessage.class, chLstMsg -> {
//            // Preferring that ChannelCreator will send all the users the channels list,
//            // instead of making all the channels send its name to every user.
//            Iterable<ActorRef> children = getContext().getChildren();
//            List<String> channelNames = new LinkedList<>();
//            children.forEach(childChannel -> channelNames.add(childChannel.toString()));
//
//            SetChannelListMessage setChLstMsg = new SetChannelListMessage();
//            setChLstMsg.channels = channelNames;
//            sender().tell(setChLstMsg, self());

        }).build();
    }
}
