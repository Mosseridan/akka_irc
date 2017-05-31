package Shared.Messages;

public class IncomingRemoveVoicedMessage {
    public String oldUserName;
    public String newUserName;
    public String sender;
    public String channel;

    public IncomingRemoveVoicedMessage(String userName, String sender, String channel){
        this.oldUserName = "+" + userName;
        this.newUserName = userName;
        this.sender = sender;
        this.channel = channel;
    }
}
