package Shared.Messages;

import akka.actor.ActorRef;

public class ConnectMessage extends Message {
    private String userName;

    public ConnectMessage(String userName){
        this.userName = userName;
    }

    public String getUserName(){
        return userName;
    }
}
