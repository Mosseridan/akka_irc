package Shared.Messages;

public class BecomeOwnerMessage {
    public String channel;
    public String userName;

    public BecomeOwnerMessage(String channel){
        this.channel = channel;
    }
}
