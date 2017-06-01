package Shared.Messages;

import java.util.List;

public class SetContentMessage extends Message{
    public String channel;
    public List<String> userList;
    public String conversation;

    public SetContentMessage( String channel, List<String> userList, String conversation) {
        this.channel = channel;
        this.userList = userList;
        this.conversation = conversation;
    }
}
