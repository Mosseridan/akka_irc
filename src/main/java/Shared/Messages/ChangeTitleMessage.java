package Shared.Messages;

public class ChangeTitleMessage extends Message {
    public String newTitle;
    public String channel;

    public ChangeTitleMessage(String newTitle, String channel){
        this.newTitle = newTitle;
        this.channel = channel;
    }
}
