package Shared.Messages;

import akka.actor.ActorRef;

public class JoinApprovalMessage extends Message{
    private String userName;
    private String channelName;
    private ActorRef channelActor;

    public JoinApprovalMessage(String userName, String channelName, ActorRef channelActor){
        this.userName = userName;
        this.channelName = channelName;
        this.channelActor = channelActor;
    }

    public String getUserName(){
        return userName;
    }

    public String getChannelName(){
        return channelName;
    }

    public ActorRef getChannelActor(){
        return channelActor;
    }

    @Override
    public String toString(){
        return("JoinApprovalMessage(userName: "+userName+", channelName: "+channelName+", channelActor: "+ channelActor.toString()+")");
    }
}
