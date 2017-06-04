package Shared.Messages;

public class OutgoingKillChannelMessage extends Message {
    private String senderName;
    private String channelName;

    public OutgoingKillChannelMessage(String senderName, String channelName){
        this.senderName = senderName;
        this.channelName = channelName;
    }

    public String getSenderName(){ return senderName; }

    public String getChannelName(){ return channelName; }

    @Override
    public String toString(){
        return("OutgoingKillChannelMessage(senderName: "+senderName+", channelName: "+channelName+")");
    }
}
