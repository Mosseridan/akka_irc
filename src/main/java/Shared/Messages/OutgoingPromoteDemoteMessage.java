package Shared.Messages;


public class OutgoingPromoteDemoteMessage extends Message {
    public UserMode newUserMode;
    public String userToPromoteDemote;
    public String sender;
    public String channel;

    public OutgoingPromoteDemoteMessage(UserMode newUserMode, String userToPromoteDemote, String sender, String channel){
        this.newUserMode = newUserMode;
        this.userToPromoteDemote = userToPromoteDemote;
        this.sender = sender;
        this.channel = channel;
    }
}
