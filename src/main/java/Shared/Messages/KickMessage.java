package Shared.Messages;

import akka.actor.ActorRef;

public class KickMessage extends Message {

    public String userName;
    public ActorRef userActor;
    public String sender;
    public String channel;

    public KickMessage(String userName, ActorRef userActor, String sender,String channel){
        this.userName = userName;
        this.userActor = userActor;
        this.sender = sender;
        this.channel = channel;
    }
}
