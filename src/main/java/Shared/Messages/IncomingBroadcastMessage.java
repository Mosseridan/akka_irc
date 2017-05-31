package Shared.Messages;

public class IncomingBroadcastMessage extends Message {
    public String channel;
    public String message;

    public IncomingBroadcastMessage(String cahnnel, String message){
        this.channel = channel;
        this.message = message;
    }
}