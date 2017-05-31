package Shared.Messages;

import javafx.scene.control.TreeItem;

import java.util.List;

public class SetChannelListMessage extends Message {
//    public TreeItem<String> channels;
public List<String> channels;
    public SetChannelListMessage(List<String> channels){
        this.channels = channels;
    }
}
