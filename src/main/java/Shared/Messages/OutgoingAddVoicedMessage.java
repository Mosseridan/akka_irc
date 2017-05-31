package Shared.Messages;


public class OutgoingAddVoicedMessage extends Message{
    public String userName;
    public String sender;
    public String channel;

    public OutgoingAddVoicedMessage(String userName,String sender, String channel){
        this.userName = userName;
        this.sender = sender;
        this.channel = channel;
    }
}
