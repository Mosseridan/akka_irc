package Shared.Messages;

import akka.actor.ActorRef;

public class JoinApprovalMessage extends Message{
    public ActorRef joinedChannel;
    public UserMode mode;
    public String joinedChannelName;
}
