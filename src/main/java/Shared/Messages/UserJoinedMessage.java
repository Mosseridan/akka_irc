package Shared.Messages;

public class UserJoinedMessage extends Message{
    public String userName;
    public String channel;

    public UserJoinedMessage(String userName, String channel){
        this.userName = userName;
        this.channel = channel;
    }
}
