package Shared.Messages;

public class GetContentMessage extends Message{
   public String channel;

    public GetContentMessage(String channel){
        this.channel = channel;
    }
}
