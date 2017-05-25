package Shared.Messages;

import akka.actor.ActorRef;

public class OutgoingPromoteDemoteMessage extends Message {
    public UserMode userMode;
    public String promotedDemotedUser;
    public ActorRef promotedDemotedUserActor;
    public String channel;

}
