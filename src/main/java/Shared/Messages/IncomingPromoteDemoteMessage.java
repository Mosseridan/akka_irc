package Shared.Messages;

import akka.actor.ActorRef;

public class IncomingPromoteDemoteMessage extends Message {
    public UserMode newUserMode;
}
