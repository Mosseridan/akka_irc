package Shared.Messages;

public class OutgoingBanMessage extends Message{
    public String userName;
    public String sender;
    public String channel;

    public OutgoingBanMessage(String userName,String sender, String channel){
        this.userName = userName;
        this.sender = sender;
        this.channel = channel;
    }
}
