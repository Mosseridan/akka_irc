package Shared.Messages;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.io.Tcp;

public class ConnectMessage extends Message {
    public String userName;
    public ActorRef serverUserActor;
    //public ActorRef clientUserActor;

    public ConnectMessage(String userName){
        this.userName = userName;
        this.serverUserActor = null;
    }

    public ConnectMessage(String userName, ActorRef serverUserActor){
        this.userName = userName;
        this.serverUserActor = serverUserActor;
    }
}
