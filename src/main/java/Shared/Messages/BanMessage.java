package Shared.Messages;

import akka.actor.ActorRef;

public class BanMessage extends Message {
    public String userNameToBan;
    public ActorRef userActorToBan;
    public String channel;
}
