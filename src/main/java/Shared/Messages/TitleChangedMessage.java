package Shared.Messages;

public class TitleChangedMessage extends Message {
    private String channelName;
    private String title;

    public TitleChangedMessage(String channelName, String title){
        this.channelName = channelName;
        this.title = title;
    }

    public String getChannelName() {
        return channelName;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String toString() {
        return("TitleChangedMessage(channelName: "+channelName+", title: "+title+")");
    }
}
