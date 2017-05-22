import Shared.Messages.ConnectMessage;
import Shared.Messages.OutgoingTextMessage;
import Shared.Messages.UserMode;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;

import java.util.HashMap;
import java.util.Map;

public class ServerUserChannelActor extends AbstractActor {

    private UserMode userMode;
    @Override
    public Receive createReceive() {
        return receiveBuilder().match(null,null

        ).match(null, null



    ).build();
    }

    @Override
    public void preStart() {

    }
}
