package Shared.Messages;

import java.awt.*;
import java.time.LocalTime;

public class TextMessage extends Message{
    public String channel;
    public String message;

    public TextMessage(String channel, String message){
        this.channel = channel;
        this.message = message;
    }
}
