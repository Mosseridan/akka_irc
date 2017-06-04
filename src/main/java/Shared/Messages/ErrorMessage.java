package Shared.Messages;

public class ErrorMessage extends Message{
    private String message;

    public ErrorMessage(String action, String reason){
        this.message = "*** Error: Could not " + action + ". " + reason + ".";
    }

    public ErrorMessage(String action){
        this.message = "*** Error: Could not " + action + ".";
    }
    public String getMessage(){
        return message;
    }

    @Override
    public String toString(){
        return("ErrorMessage(message: "+message+")");
    }
}
