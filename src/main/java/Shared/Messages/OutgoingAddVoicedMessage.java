package Shared.Messages;

public class OutgoingAddVoicedMessage extends Message{
    private String userName;
    private String senderName;
    private String channelName;

    public OutgoingAddVoicedMessage(String userName,String senderName, String channelName){
        this.userName = userName;
        this.senderName = senderName;
        this.channelName = channelName;
    }

    public String getUserName(){ return userName; }

    public String getSenderName(){ return senderName; }

    public String getChannelName(){ return  channelName; }

    @Override
    public String toString(){
        return("OutgoingAddVoicedMessage(userName: "+userName+", senderName: "+senderName+", channelName: "+channelName+")");
    }
}
