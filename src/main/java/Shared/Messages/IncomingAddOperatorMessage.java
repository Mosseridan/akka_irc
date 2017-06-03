package Shared.Messages;

public class IncomingAddOperatorMessage extends Message{
    private String userName;
    private String senderName;
    private String channelName;

    public IncomingAddOperatorMessage(String userName,String senderName, String channelName){
        this.userName = userName;
        this.senderName = senderName;
        this.channelName = channelName;
    }

    public String getUserName(){ return userName; }

    public String getSenderName(){ return senderName; }

    public String getChannelName(){ return  channelName; }
}
