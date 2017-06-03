package Shared.Messages;

public class JoinMessage extends Message {
    private String userName;
    private String channelName;

    public JoinMessage(String userName, String channelName){
        this.userName = userName;
        this.channelName = channelName;
    }

    public String getUserName(){ return userName; }

    public String getChannelName(){
        return channelName;
    }
}
