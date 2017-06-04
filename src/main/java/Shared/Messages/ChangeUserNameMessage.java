package Shared.Messages;

public class ChangeUserNameMessage extends Message{
    private String oldUserName;
    private String newUserName;
    private String channelName;

    public ChangeUserNameMessage(String oldUserName, String newUserName, String channelName){
        this.oldUserName = oldUserName;
        this.newUserName = newUserName;
        this.channelName = channelName;
    }

    public String getOldUserName() {
        return oldUserName;
    }

    public String getNewUserName() {
        return newUserName;
    }

    public String getChannelName() {
        return channelName;
    }

    @Override
    public String toString(){
        return("ChangeUserNameMessage(oldUserName: "+oldUserName+", newUserName: "+newUserName+", channelName: "+channelName+")");
    }
}
