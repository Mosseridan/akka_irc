package Shared.Messages;

import akka.actor.ActorRef;

public class JoinApprovalMessage {
    public boolean approved;
    public ActorRef joinedChannel;
    public UserMode mode;
}
