package Shared.Messages;

public class AddUserNameMessage extends Message {
    private String userName;
    private String channelName;

    public AddUserNameMessage(String userName, String channelName){
        this.userName = userName;
        this.channelName = channelName;
    }

    public String getUserName(){ return userName; }

    public String getChannelName(){
        return channelName;
    }
}
