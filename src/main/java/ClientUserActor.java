import Shared.Messages.*;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;

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
            // ExitMessage
            .match(ExitMessage.class, this::receiveExit)
            // For any unhandled message
            .matchAny(this::receiveUnhandled)
            .build();
    }


    // IncomingPrivateMessage
    private void receiveIncomingPrivate(IncomingPrivateMessage msg) {
        //TODO: ADD NEW WINDOW SUPPORT!
        chatWindow.printText(msg.getTime()+"~PM~ <"+msg.getSenderName()+"> "+msg.getMessage());
    }

    // IncomingBroadcastMessage
    private void receiveIncomingBroadcast(IncomingBroadcastMessage msg) {
        if(msg.getChannelName() == currentChannelName) {
            chatWindow.printText(msg.getTime()+"<"+msg.getSenderName()+"> "+msg.getMessage());
        }
    }

    // AnnouncementMessage
    private void receiveAnnouncement(AnnouncementMessage msg) {
        if(msg.getChannelName() == currentChannelName) {
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
    private void receiveJoinApproval(JoinApprovalMessage msg) {
        currentChannelName = msg.getChannelName();
        chatWindow.addChannel(msg.getChannelName());
        serverUserActor.tell(new GetAllUserNamesMessage(userName,currentChannelName),getSelf());
        chatWindow.printText("*** joins: "+msg.getUserName());
        chatWindow.setTitle("User: "+userName+", Channel: "+msg.getChannelName());
    }

    // UserJoinedMessage
    private void receiveUserJoined(UserJoinedMessage msg) {
        if(msg.getChannelName() == currentChannelName) {
            chatWindow.printText(msg.getTime() + "*** joins: " + msg.getUserName());
            chatWindow.addUser(msg.getUserName());
        }
    }

    // UserLeftMessage
    private void receiveUserLeft(UserLeftMessage msg) {
        if(msg.getChannelName() == currentChannelName) {
            chatWindow.printText(msg.getTime() + "*** parts: " + msg.getUserName());
            chatWindow.removeUser(msg.getUserName());
        }
    }

    // IncomingKillChannelMessage
    private void receiveIncomingKillChannel(IncomingKillChannelMessage msg){
        if(msg.getChannelName() == currentChannelName) {
            chatWindow.printText(msg.getTime() + "*** channel "+msg.getChannelName()+" was disband by "+msg.getSenderName());
            currentChannelName = null;
        }
        chatWindow.removeChannel(msg.getChannelName());
    }

    // IncomingKickMessage
    private void receiveIncomingKick(IncomingKickMessage msg){
        if(msg.getChannelName() == currentChannelName){
            chatWindow.printText("*** " + msg.getUserName() + " kicked by " + msg.getSenderName());
            currentChannelName = null;
        }
        chatWindow.removeChannel(msg.getChannelName());
    }

    // IncomingBanMessage
    private void receiveIncomingBan(IncomingBanMessage msg){
        if(msg.getChannelName() == currentChannelName){
            chatWindow.printText("*** " + msg.getUserName() + " banned by " + msg.getSenderName());
            currentChannelName = null;
        }
        chatWindow.removeChannel(msg.getChannelName());
    }

    // AddUserNameMessage
    private void receiveAddUserName(AddUserNameMessage msg) {
        if(msg.getChannelName() == currentChannelName){
           chatWindow.addUser(msg.getUserName());
        }
    }

    // ExitMessage
    private void receiveExit(ExitMessage msg){
        serverUserActor.tell(msg,getSelf());
       getContext().stop(getSelf());
    }

    // GUIMessage
    private void receiveGUI(GUIMessage msg) {
        String text = msg.getText();
        if (text == "") return;
        String[] cmdArr = text.split(" ");
        if (!verifyFormat(cmdArr)) return;

        switch (cmdArr[0]) {
            case "/w":
                serverUserActor.tell(new OutgoingPrivateMessage(cmdArr[1], userName, text.split(cmdArr[1], 2)[1]), getSelf());
                break;
            case "/join":
                serverUserActor.tell(new JoinMessage(userName, cmdArr[1]), getSelf());
                break;
            case "/leave":
                if (currentChannelName == cmdArr[1]) {
                    chatWindow.printText(msg.getTime()+"*** parts: " + userName);
                    currentChannelName = null;
                }
                chatWindow.removeChannel(cmdArr[1]);
                serverUserActor.tell(new LeaveMessage(userName, cmdArr[1]), getSelf());
                break;
            case "/title":
                serverUserActor.tell(new ChangeTitleMessage(userName, cmdArr[1], cmdArr[2]), getSelf());
                break;
            case "/kick":
                serverUserActor.tell(new OutgoingKickMessage(cmdArr[1], userName, cmdArr[2]), getSelf());
                break;
            case "/ban":
                serverUserActor.tell(new OutgoingBanMessage(cmdArr[1], userName, cmdArr[2]), getSelf());
                break;
            case "/add":
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
                    chatWindow.printText(msg.getTime()+ "*** Error: Could not switch to channel "+cmdArr[1]+"you are not in this channel.");
                } else {
                    chatWindow.clearContext();
                    chatWindow.currentChannelName = cmdArr[1];
                    currentChannelName = cmdArr[1];
                    serverUserChannelActor.tell(new GetAllUserNamesMessage(userName,cmdArr[1]),getSelf());
                }
                break;
            default:
                serverUserActor.tell(new OutgoingBroadcastMessage(userName, currentChannelName, text), getSelf());
        }
    }

    boolean verifyFormat(String[] cmdArr) {
        if (cmdArr.length == 2 && (cmdArr[0] == "/join" || cmdArr[0] == "/leave" || cmdArr[0] == "/disband")) {
            return true;
        } else if (cmdArr.length == 3 && (cmdArr[0] == "/kick" || cmdArr[0] == "/ban" || cmdArr[0] == "/title")) {
            return true;
        } else if (cmdArr.length == 4 && (cmdArr[0] == "/add" || cmdArr[0] == "/remove") && (cmdArr[2] == "v" || cmdArr[2] == "op")) {
            return true;
        } else if (cmdArr.length >= 3 && (cmdArr[0] == "/w" || cmdArr[0] == "/to")) {
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
