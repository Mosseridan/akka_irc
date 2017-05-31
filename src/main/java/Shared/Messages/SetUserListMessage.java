package Shared.Messages;

import java.util.List;

public class SetUserListMessage extends  Message{
    public String channel;
    public List<String> userList;

    public SetUserListMessage(String channel, List<String> userList){
        this.channel = channel;
        this.userList = userList;
    }
}
