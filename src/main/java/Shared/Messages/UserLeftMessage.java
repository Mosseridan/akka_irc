
package Shared.Messages;

public class UserLeftMessage extends Message{
    public String userName;
    public String channel;

    public UserLeftMessage(String userName, String channel){
        this.userName = userName;
        this.channel = channel;
    }
}
