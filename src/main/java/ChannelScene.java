import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;

import java.util.List;

public class ChannelScene {
    public String channel;
    public String chatBox;
    public List<String> userList;


    ChannelScene(String channel, String chatBox, List<String> userList){
        this.channel = channel;
        this.chatBox = chatBox;
        this.userList = userList;
    }

    public String toString(){
        return channel;
    }
}
