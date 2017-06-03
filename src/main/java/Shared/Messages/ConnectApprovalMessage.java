package Shared.Messages;

import akka.actor.ActorRef;

public class ConnectApprovalMessage extends Message{
    private ActorRef serverUserActor;

    public ConnectApprovalMessage(ActorRef serverUserActor){
        this.serverUserActor = serverUserActor;
    }

    public ActorRef getServerUserActor(){
        return serverUserActor;
    }

}
