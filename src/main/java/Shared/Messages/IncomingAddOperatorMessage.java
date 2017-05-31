package Shared.Messages;

public class IncomingAddOperatorMessage extends Message{
    public String oldUserName;
    public String newUserName;
    public String sender;
    public String channel;

    public IncomingAddOperatorMessage(String userName, String sender, String channel){
        this.oldUserName = userName;
        this.newUserName = "@" + userName;
        this.sender = sender;
        this.channel = channel;
    }
}
