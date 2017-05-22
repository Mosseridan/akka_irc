package Shared.Messages;

public class UpgradeModeMessage{
    public UserMode userMode;
    public String upgradedUser;

    public UserMode textToUserMode(String textualUserMode) {
        UserMode userMode = null;
        if (textualUserMode.toLowerCase().equals("v")) {
            userMode = UserMode.VOICE;
        } else if (textualUserMode.toLowerCase().equals("op")) {
            userMode = UserMode.OPERATOR;
        /*} else if (textualUserMode.toLowerCase().equals("voice")) {
            userMode = UserMode.VOICE
        }  else if (textualUserMode.toLowerCase().equals("voice")) {
            userMode = UserMode.VOICE
        }*/

        }
        return userMode;
    }
}
