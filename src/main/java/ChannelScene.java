import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;

import java.util.List;

public class ChannelScene {
    public String channel;
    public Scene scene;


    ChannelScene(String channel,Scene scene){
        this.channel = channel;
        this.scene = scene;
    }

    public String toString(){
        return channel;
    }
}
