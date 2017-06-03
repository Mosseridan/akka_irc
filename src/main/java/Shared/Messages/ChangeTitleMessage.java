package Shared.Messages;

public class ChangeTitleMessage extends Message {
    private String senderName;
    private String title;
    private String channelName;

    public ChangeTitleMessage(String senderName, String channelName, String title){
        this.senderName = senderName;
        this.title = title;
        this.channelName = channelName;
    }

    public String getSenderName(){ return senderName; }

    public String getTitle(){ return title; }

    public String getChannelName(){ return  channelName; }
}
