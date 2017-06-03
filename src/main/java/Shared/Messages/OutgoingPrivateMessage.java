package Shared.Messages;


public class OutgoingPrivateMessage extends Message {
    private String userName;
    private String senderName;
    private String message;

    public OutgoingPrivateMessage(String userName, String senderName, String message){
        this.userName = userName;
        this.senderName = senderName;
        this.message = message;
    }

    public String getUserName(){ return userName; }

    public String getSenderName(){ return senderName; }

    public String getMessage(){ return message; }
}
