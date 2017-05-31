package Shared.Messages;

import akka.actor.ActorRef;

import java.util.List;

public class JoinApprovalMessage extends Message{
    public String userName;
    public UserMode mode;
    public String channelName;
    public ActorRef channelRef;
    public List<String> channelUsers;

    public JoinApprovalMessage(String userName, UserMode mode,String channelName, ActorRef channelRef, List<String> channelUsers){
        this.userName = userName;
        this.mode = mode;
        this.channelName = channelName;
        this.channelRef = channelRef;
        this.channelUsers = channelUsers;
    }
}
