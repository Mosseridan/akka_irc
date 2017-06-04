package Shared.Messages;

public class GUIMessage extends Message {
    private String text;

    public GUIMessage(String text) {
        this.text = text;
    }

    public String getText(){ return text; }

    @Override
    public String toString(){
        return("GUIMessage(text: "+text+")");
    }
}
