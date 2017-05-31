package Shared.Messages;

public class IncomingKickMessage extends Message{
    public String userName;
    public String sender;
    public String channel;

    public IncomingKickMessage(String userName, String sender, String channel){
        this.userName = userName;
        this.sender = sender;
        this.channel = channel;
    }
}
