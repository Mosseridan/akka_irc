package Shared.Messages;

import java.awt.*;

public class TextMessage extends Message{
    public String channel;
    public String message;

    public TextMessage(String channel, String message){
        this.channel = channel;
        this.message = message;
    }
}
