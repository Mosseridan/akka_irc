package Shared.Messages;

import java.time.LocalTime;

public class OutgoingPrivateMessage extends Message {
    public String userName;
    public String sender;
    public String message;

    public OutgoingPrivateMessage(String userName, String sender, String message){
        this.userName = userName;
        this.sender = sender;
        this.message = message;
    }
}
