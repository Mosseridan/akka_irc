package Shared.Messages;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;

public class ConnectMessage extends Message {
    public String userName;
    public ActorRef serverUserActor;
    public ActorRef clientUserActor;
}
