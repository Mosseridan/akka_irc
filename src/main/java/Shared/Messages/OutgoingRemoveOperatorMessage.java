package Shared.Messages;

public class OutgoingRemoveOperatorMessage extends Message{
    public String userName;
    public String senderName;
    public String channelName;


    public OutgoingRemoveOperatorMessage(String userName,String senderName, String channelName){
        this.userName = userName;
        this.senderName = senderName;
        this.channelName = channelName;
    }

    public String getUserName(){ return userName; }

    public String getSenderName(){ return senderName; }

    public String getChannelName(){ return  channelName; }
}
