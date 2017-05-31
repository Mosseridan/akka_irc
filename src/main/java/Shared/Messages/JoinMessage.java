package Shared.Messages;

public class JoinMessage extends Message {
    public String userName;
    public String channel;

    public JoinMessage(String userName, String channel){
        this.userName = userName;
        this.channel = channel;
    }
}
