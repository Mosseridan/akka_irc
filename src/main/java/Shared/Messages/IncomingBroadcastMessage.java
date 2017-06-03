package Shared.Messages;

public class IncomingBroadcastMessage extends Message {
    private String senderName;
    private String channelName;
    private String message;

    public IncomingBroadcastMessage(String senderName, String cahnnelName, String message){
        this.senderName = senderName;
        this.channelName = channelName;
        this.message = message;
    }

    public String getSenderName(){ return senderName; }

    public String getChannelName(){ return channelName; }

    public String getMessage(){ return message; }
}