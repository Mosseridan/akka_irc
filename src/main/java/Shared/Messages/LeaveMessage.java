package Shared.Messages;

public class LeaveMessage extends Message {
    public String channel;
    public String userName;
    public UserMode userMode;

    public LeaveMessage(String userName, String channel){
        this.userName = userName;
        this.userMode = null;
        this.channel = channel;
    }

    public LeaveMessage(String userName, UserMode userMode, String channel){
        this.userName = userName;
        this.userMode = userMode;
        this.channel = channel;
    }
}
