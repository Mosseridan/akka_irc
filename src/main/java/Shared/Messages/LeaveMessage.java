package Shared.Messages;

public class LeaveMessage extends Message {
    private String userName;
    private String channelName;

    public LeaveMessage(String userName, String channelName){
        this.userName = userName;
        this.channelName = channelName;
    }

    public String getUserName(){ return userName; }

    public String getChannelName(){ return channelName; }
}
