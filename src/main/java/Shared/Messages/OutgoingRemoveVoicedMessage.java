package Shared.Messages;

import java.time.LocalTime;

public class OutgoingRemoveVoicedMessage extends Message{
    public String userName;
    public String sender;
    public String channel;

    public OutgoingRemoveVoicedMessage(String userName,String sender, String channel){
        this.userName = userName;
        this.sender = sender;
        this.channel = channel;

    }
}
