package Shared.Messages;

;import java.io.Serializable;
import java.time.LocalTime;

public class Message implements Serializable {
    public String time;

     public Message(){
         this.time = "["+ LocalTime.now().toString()+"] ";
     }
}

