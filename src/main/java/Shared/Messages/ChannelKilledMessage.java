
package Shared.Messages;

public class ChannelKilledMessage extends Message {
    public String channelName;
    public String killer;

    public ChannelKilledMessage(String channelName, String killer){
        this.channelName = channelName;
        this.killer = killer;
    }
}
