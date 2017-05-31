package Shared.Messages;

public class IncomingPrivateMessage extends Message {
    public String sender;
    public String message;

    public IncomingPrivateMessage(String sender, String message){
        this.sender = sender;
        this.message = message;
    }
}

