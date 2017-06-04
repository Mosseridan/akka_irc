package Shared.Messages;

public class OutgoingBroadcastMessage extends Message {
    private String senderName;
    private String channelName;
    private String message;

    public OutgoingBroadcastMessage(String senderName, String channelName, String message){
        this.senderName = senderName;
        this.channelName = channelName;
        this.message = message;
    }

    public String getSenderName(){ return senderName; }

    public String getChannelName(){ return channelName; }

    public String getMessage(){ return message; }

    @Override
    public String toString(){
        return("OutgoingBroadcastMessage(senderName: "+senderName+", channelName: "+channelName+", message: "+message+")");
    }
}
