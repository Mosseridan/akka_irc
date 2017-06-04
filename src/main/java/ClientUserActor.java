import Shared.Messages.*;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class ClientUserActor extends AbstractActor {
    String userName;
    String serverName;
    ActorRef serverUserActor;
    ChatWindow chatWindow;
    String currentChannelName;


    public ClientUserActor(String userName, String serverName, ChatWindow chatWindow) {
        this.userName = userName;
        this.serverName = serverName;
        this.chatWindow = chatWindow;
    }


    @Override
    public void preStart() {
        ActorSelection server = getContext()
                .actorSelection("akka.tcp://IRC@"+serverName+":2552/user/Server");

        // Send a connect request to the server
        server.tell(new ConnectMessage(userName), getSelf());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            // GUIMessage
            .match(GUIMessage.class, this::receiveGUI)
            // IncomingPrivateMessage
            .match(IncomingPrivateMessage.class, this::receiveIncomingPrivate)
            // IncomingBroadcastMessage
            .match(IncomingBroadcastMessage.class,this::receiveIncomingBroadcast)
            // AnnouncementMessage
            .match(AnnouncementMessage.class,this::receiveAnnouncement)
            // ErrorMessage
            .match(ErrorMessage.class,this::receiveError)
            // ConnectApprovalMessage
            .match(ConnectApprovalMessage.class, this::receiveConnectApproval)
            // JoinApprovalMessage
            .match(JoinApprovalMessage.class, this::receiveJoinApproval)
            // UserJoinedMessage
            .match(UserJoinedMessage.class, this::receiveUserJoined)
            // UserLeftMessage
            .match(UserLeftMessage.class, this::receiveUserLeft)
            // IncomingKillChannelMessage
            .match(IncomingKillChannelMessage.class, this::receiveIncomingKillChannel)
            // IncomingKickMessage
            .match(IncomingKickMessage.class, this::receiveIncomingKick)
            // IncomingBanMessage
            .match(IncomingBanMessage.class, this::receiveIncomingBan)
            // AddUserNameMessage
            .match(AddUserNameMessage.class, this::receiveAddUserName)
            // ChangeUserNameMessage
            .match(ChangeUserNameMessage.class, this::receiveChangeUserName)
            // GetContentMessage
            .match(GetContentMessage.class, msg->serverUserActor.tell(msg,getSelf()))
            //TitleChangedMessage
            .match(TitleChangedMessage.class, this::receiveTitleChaged)
            // ExitMessage
            .match(ExitMessage.class, this::receiveExit)
            // For any unhandled message
            .matchAny(this::receiveUnhandled)
            .build();
    }


    // IncomingPrivateMessage
    private void receiveIncomingPrivate(IncomingPrivateMessage msg) {
        chatWindow.printText(new TextFlow(new Text(msg.getTime()+"~PM~ <"),chatWindow.userNameToText(msg.getSenderName()),new Text("> "+msg.getMessage())));
    }

    // IncomingBroadcastMessage
    private void receiveIncomingBroadcast(IncomingBroadcastMessage msg) {
        if(msg.getChannelName().equals(currentChannelName)) {
            chatWindow.printText(new TextFlow(new Text(msg.getTime()+"<"),chatWindow.userNameToText(msg.getSenderName()),new Text("> "+msg.getMessage())));
        }
    }

    // AnnouncementMessage
    private void receiveAnnouncement(AnnouncementMessage msg) {
        if(msg.getChannelName().equals(currentChannelName)) {
            chatWindow.printText(msg.getTime()+msg.getMessage());
        }
    }

    // ErrorMessage
    private void receiveError(ErrorMessage msg) {
        chatWindow.printText(msg.getTime()+msg.getMessage());
    }

    // ConnectApprovalMessage
    private void receiveConnectApproval(ConnectApprovalMessage msg) {
        serverUserActor = msg.getServerUserActor();
         if (serverUserActor != null) {
            chatWindow.printText(msg.getTime() + "*** " + userName + " Connected successfully.");
        } else {
            chatWindow.printText(msg.getTime()+"Something went wrong while connecting.");
        }
    }

    // JoinApprovalMessage
    private void receiveJoinApproval(JoinApprovalMessage msg) { //TODO add title
        currentChannelName = msg.getChannelName();
        chatWindow.addChannel(msg.getChannelName());
        //serverUserActor.tell(new GetContentMessage(userName,currentChannelName),getSelf());
        chatWindow.printAlert(msg.getTime(),msg.getUserName(),"joins");
        //chatWindow.setTitle("UserName: "+userName+", ChannelName: "+msg.getChannelName());
    }

    // UserJoinedMessage
    private void receiveUserJoined(UserJoinedMessage msg) {
        if(msg.getChannelName().equals(currentChannelName)) {
            chatWindow.printAlert(msg.getTime(),msg.getUserName(),"joins");
            chatWindow.addUser(msg.getUserName());
        }
    }

    // UserLeftMessage
    private void receiveUserLeft(UserLeftMessage msg) {
        if(msg.getChannelName().equals(currentChannelName)) {
            chatWindow.printAlert(msg.getTime(),msg.getUserName(),"parts");
            chatWindow.removeUser(msg.getUserName());
        }
    }

    // IncomingKillChannelMessage
    private void receiveIncomingKillChannel(IncomingKillChannelMessage msg){
        if(msg.getChannelName().equals(currentChannelName)) {
            chatWindow.printThisByThat(msg.getTime(),"channel "+msg.getChannelName(),"was disband",msg.getSenderName());
            currentChannelName = null;
        }
        chatWindow.removeChannel(msg.getChannelName());
    }

    // IncomingKickMessage
    private void receiveIncomingKick(IncomingKickMessage msg){
        if(msg.getChannelName().equals(currentChannelName)){
            chatWindow.printThisByThat(msg.getTime(),msg.getUserName(),"kicked",msg.getSenderName());
            currentChannelName = null;
        }
        chatWindow.removeChannel(msg.getChannelName());
    }

    // IncomingBanMessage
    private void receiveIncomingBan(IncomingBanMessage msg){
        chatWindow.clearContext();
        if(msg.getChannelName().equals(currentChannelName)){
            chatWindow.printThisByThat(msg.getTime(),msg.getUserName(),"banned",msg.getSenderName());
            currentChannelName = null;
        }
        chatWindow.removeChannel(msg.getChannelName());
    }

    // AddUserNameMessage
    private void receiveAddUserName(AddUserNameMessage msg) {
        if(msg.getChannelName().equals(currentChannelName)){
           chatWindow.addUser(msg.getUserName());
        }
    }

    // ChangeUserNameMessage
    private void receiveChangeUserName(ChangeUserNameMessage msg){
        if(msg.getChannelName().equals(currentChannelName)){
            chatWindow.removeUser(msg.getOldUserName());
            chatWindow.addUser(msg.getNewUserName());
        }
    }

    // TitleChangedMessage
    private void receiveTitleChaged(TitleChangedMessage msg){
        if(msg.getChannelName().equals(currentChannelName) && msg.getTitle() != null)
            chatWindow.setTitle("ChannelName: "+msg.getChannelName()+"    Title: "+msg.getTitle());
    }

    // ExitMessage
    private void receiveExit(ExitMessage msg){
        //serverUserActor.tell(msg,getSelf());
        System.out.println("$$$ in clientUserActor userName: "+userName+" closing client");
        getContext().stop(getSelf());
    }

    // GUIMessage
    private void receiveGUI(GUIMessage msg) {
        String text = msg.getText();
        if (text.equals("")) return;
        String[] cmdArr = text.split(" ");
        if (!verifyFormat(cmdArr)) {
            chatWindow.invalidSyntax(msg.getText());
            return;
        }

        switch (cmdArr[0]) {
            case "/w":
                serverUserActor.tell(new OutgoingPrivateMessage(cmdArr[1], userName, text.split(cmdArr[1], 2)[1]), getSelf());
                break;
            case "/join":
                serverUserActor.tell(new JoinMessage(userName, cmdArr[1]), getSelf());
                break;
            case "/leave":
                if (cmdArr[1].equals(currentChannelName)) {
                    chatWindow.printText(msg.getTime()+"*** parts: " + userName);
                    chatWindow.clearContext();
                    currentChannelName = null;
                }
                chatWindow.removeChannel(cmdArr[1]);
                serverUserActor.tell(new LeaveMessage(userName, cmdArr[1]), getSelf());
                break;
            case "/title":
                if(cmdArr[1].equals(userName)){
                    chatWindow.printText(msg.getTime()+"*** kick your self");
                    break;
                }
                serverUserActor.tell(new ChangeTitleMessage(userName, cmdArr[1], cmdArr[2]), getSelf());
                break;
            case "/kick":
                serverUserActor.tell(new OutgoingKickMessage(cmdArr[1], userName, cmdArr[2]), getSelf());
                break;
            case "/ban":
                if(cmdArr[1].equals(userName)){
                    chatWindow.printText(msg.getTime()+"*** Could not ban yourself");
                    break;
                }
                serverUserActor.tell(new OutgoingBanMessage(cmdArr[1], userName, cmdArr[2]), getSelf());
                break;
            case "/add":
                if(cmdArr[1].equals(userName)){
                    chatWindow.printText(msg.getTime()+"*** add rights to your self");
                    break;
                }
                switch (cmdArr[2]) {
                    case "v":
                        serverUserActor.tell(new OutgoingAddVoicedMessage(cmdArr[3], userName, cmdArr[1]), getSelf());
                        break;
                    case "op":
                        serverUserActor.tell(new OutgoingAddOperatorMessage(cmdArr[3], userName, cmdArr[1]), getSelf());
                        break;
                    default:
                        chatWindow.invalidSyntax(text);
                }
                break;
            case "/remove":
                if(cmdArr[1].equals(userName)){
                    chatWindow.printText(msg.getTime()+"*** remove rights from your self");
                    break;
                }
                switch (cmdArr[2]) {

                    case "v":
                        serverUserActor.tell(new OutgoingRemoveVoicedMessage(cmdArr[3], userName, cmdArr[1]), getSelf());
                        break;
                    case "op":
                        serverUserActor.tell(new OutgoingRemoveOperatorMessage(cmdArr[3], userName, cmdArr[1]), getSelf());
                        break;
                    default:
                        chatWindow.invalidSyntax(text);
                }
                break;
            case "/disband":
                serverUserActor.tell(new OutgoingKillChannelMessage(userName, cmdArr[1]), getSelf());
                break;
            case "/to": // "/to <channelName> <message>" switches to the specified channel and sends the given message in it BONUS!
                //ActorSelection sel = getContext().actorSelection("/user/Server/ServerUser"+userName+"/"+cmdArr[1]);
                ActorSelection sel = getContext().actorSelection(serverUserActor.path()+"/"+cmdArr[1]);
                ActorRef serverUserChannelActor = HelperFunctions.getActorRefBySelection(sel);

                if(serverUserChannelActor == null){
                    chatWindow.printText(msg.getTime()+ "*** Could not switch to channel "+cmdArr[1]+"you are not in this channel.");
                } else {
                    currentChannelName = cmdArr[1];
                    chatWindow.switchChannel(cmdArr[1]);
                    if(cmdArr.length > 2)
                        serverUserActor.tell(new OutgoingBroadcastMessage(userName, cmdArr[1], text.split(cmdArr[1], 2)[1]), getSelf());
                }
                break;
            default:
                serverUserActor.tell(new OutgoingBroadcastMessage(userName, currentChannelName, text), getSelf());
        }
    }

    boolean verifyFormat(String[] cmdArr) {
        if (cmdArr.length == 2 && (cmdArr[0].equals("/join") || cmdArr[0].equals("/leave") || cmdArr[0].equals("/disband") || cmdArr[0].equals("/to"))) {
            return true;
        } else if (cmdArr.length == 3 && (cmdArr[0].equals("/kick") || cmdArr[0].equals("/ban") || cmdArr[0].equals("/title"))) {
            return true;
        } else if (cmdArr.length == 4 && (cmdArr[0].equals("/add") || cmdArr[0].equals("/remove") && (cmdArr[2].equals("v") || cmdArr[2].equals("op")))) {
            return true;
        } else if (cmdArr.length >= 3 && (cmdArr[0].equals("/w") || cmdArr[0].equals("/to"))) {
            return true;
        }else if (!cmdArr[0].startsWith("/")){// just text
            return true;
        }else {
            return false;
        }
    }

    // For any unhandled message
    private void receiveUnhandled(Object o) {
        getSender().tell(new ErrorMessage("Send "+o.toString(),"This message is invalid in the current getContext"),getSelf());
    }

}
