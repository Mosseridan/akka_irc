package Shared.Messages;

public class IncomingPromoteDemoteMessage extends Message {
    public UserMode newUserMode;
    public String sender;
    public String channel;

//    public IncomingPromoteDemoteMessage(UserMode newUserMode, String sender, String channel){
//        this.newUserMode = newUserMode;
//        this.sender = sender;
//        this.channel = channel;
//    }
}
