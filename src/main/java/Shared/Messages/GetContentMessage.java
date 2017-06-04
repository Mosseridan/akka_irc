package Shared.Messages;

public class GetContentMessage extends Message {
    private String senderName;
    private String channelName;

    public GetContentMessage(String senderName, String channelName){
        this.senderName = senderName;
        this.channelName = channelName;
    }

    public String getSenderName(){
        return senderName;
    }

    public String getChannelName(){
        return channelName;
    }

    @Override
    public String toString(){
        return("GetContentMessage(senderName: "+senderName+", channelName: "+channelName+")");
    }
}
