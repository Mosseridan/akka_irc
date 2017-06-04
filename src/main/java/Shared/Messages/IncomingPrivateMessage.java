package Shared.Messages;

public class IncomingPrivateMessage extends Message {
    private String userName;
    private String senderName;
    private String message;

    public IncomingPrivateMessage(String userName, String senderName, String message){
        this.userName = userName;
        this.senderName = senderName;
        this.message = message;
    }

    public String getUserName(){ return userName; }

    public String getSenderName(){ return senderName; }

    public String getMessage(){ return message; }

    @Override
    public String toString(){
        return("IncomingPrivateMessage(userName: "+userName+", senderName: "+senderName+", message: "+message+")");
    }
}

