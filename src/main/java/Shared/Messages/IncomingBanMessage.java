package Shared.Messages;

public class IncomingBanMessage extends Message{
    public String userName;
    public String sender;
    public String channel;

    public IncomingBanMessage(String userName, String sender, String channel){
        this.userName = userName;
        this.sender = sender;
        this.channel = channel;
    }
}
