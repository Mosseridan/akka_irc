package Shared.Messages;

import java.time.LocalTime;

public class OutgoingBroadcastMessage extends Message {
    public String sender;
    public String channel;
    public String message;

    public OutgoingBroadcastMessage(String sender, String channel, String message){
        this.sender = sender;
        this.channel = channel;
        this.message = message;
    }

}
