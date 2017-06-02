package Shared.Messages;

import java.time.LocalTime;

public class OutgoingAddOperatorMessage extends Message{
    public String userName;
    public String sender;
    public String channel;


    public OutgoingAddOperatorMessage(String userName,String sender, String channel){
        this.userName = userName;
        this.sender = sender;
        this.channel = channel;
    }
}
