package Shared.Messages;

import java.time.LocalTime;

public class OutgoingKickMessage extends Message{
    public String userName;
    public String sender;
    public String channel;

    public OutgoingKickMessage(String userName,String sender, String channel){
        this.userName = userName;
        this.sender = sender;
        this.channel = channel;
    }
}
