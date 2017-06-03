package Shared.Messages;

public class AnnouncementMessage extends Message {
   private String channelName;
   private String message;

    public AnnouncementMessage(String channelName, String message){
        this.channelName = channelName;
        this.message = "*** "+message;
    }

    public String getChannelName(){
        return channelName;
    }

    public String getMessage(){ return message; }
}
