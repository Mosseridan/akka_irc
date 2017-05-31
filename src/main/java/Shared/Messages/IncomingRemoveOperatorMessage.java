package Shared.Messages;

public class IncomingRemoveOperatorMessage {
    public String oldUserName;
    public String newUserName;
    public String sender;
    public String channel;

    public IncomingRemoveOperatorMessage(String userName, String sender, String channel){
        this.oldUserName = "@" + userName;
        this.newUserName = userName;
        this.sender = sender;
        this.channel = channel;
    }
}
