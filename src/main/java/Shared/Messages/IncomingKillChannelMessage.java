package Shared.Messages;

public class IncomingKillChannelMessage extends Message {
    private String senderName;
    private String channelName;

    public IncomingKillChannelMessage(String senderName, String channelName){
        this.senderName = senderName;
        this.channelName = channelName;
    }

    public String getSenderName(){ return senderName; }

    public String getChannelName(){ return channelName; }
}
