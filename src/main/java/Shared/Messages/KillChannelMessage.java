package Shared.Messages;

public class KillChannelMessage extends Message {
    public String channelName;
    public String killer;

    public KillChannelMessage(String channelName, String killer){
        this.channelName = channelName;
        this.killer = killer;
    }


    public KillChannelMessage(String channelName){
        this.channelName = channelName;
        this.killer = null;
    }
}
