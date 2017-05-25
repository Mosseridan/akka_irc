package Shared.Messages;

import akka.actor.ActorRef;

public class KickMessage extends Message {

    public String userNameToKick;
    public ActorRef userActorToKick;
    public String channel;
}
