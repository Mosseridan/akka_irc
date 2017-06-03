package Shared.Messages;

;import java.io.Serializable;
import java.time.LocalTime;

public class Message implements Serializable {
    private String time;

     public Message(){
         this.time = "["+ LocalTime.now().toString()+"] ";
     }

     public String getTime(){
         return time;
     }
}

