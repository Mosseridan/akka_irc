package Shared.Messages;

import java.util.List;

public class UserListInChannelMessage extends Message {
    public String channelName;
    public List<String> users;
}
