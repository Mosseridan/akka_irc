import Shared.Messages.*;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;

public class ClientUserActor extends AbstractActor {
    String userName;
    ActorRef serverUserActor;
    ChatWindow chatWindow;
    String currentChannelName;
    ActorRef userChannelActor;

    public ClientUserActor(String userName, ChatWindow chatWindow) {
        this.userName = userName;
        this.chatWindow = chatWindow;
    }


    @Override
    public void preStart() {
        ActorSelection server = getContext()
                .actorSelection("akka.tcp://IRC@127.0.0.1:2552/user/Server");
        // Send a connect request to the server
        server.tell(new ConnectMessage(userName), self());
    }



    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(GUIMessage.class, msg -> {
                    handleTextCommand(msg.text);
                })
                .match(String.class, msg -> {
                    chatWindow.printText(msg);
                })
                .match(TextMessage.class, msg -> {
                    if(msg.channel == null || msg.channel.equals(currentChannelName))
                         chatWindow.printText(msg.message);
                })
                .match(ConnectMessage.class, msg -> {
                    serverUserActor = msg.serverUserActor;
                    if (serverUserActor != null) {
                        chatWindow.printText("<" + userName + "> Connected successfully.");
                    }else {
                        chatWindow.printText("Something went wrong while connecting.");
                    }
                })
                .match(JoinApprovalMessage.class, msg -> {
                    currentChannelName = msg.channelName;
                    userChannelActor = msg.channelRef;
                    chatWindow.addChannel(msg.channelName,msg.channelUsers);
                    if (msg.mode == UserMode.OWNER) {
                        chatWindow.printText("*** Created channel: " + msg.channelName);
                    }
                    chatWindow.printText("*** joins: " + msg.userName);
                    //chatWindow.setUserList(msg.channelName, msg.channelUsers);
                    chatWindow.setTitle("User: " + userName + ", Channel: " + msg.channelName);
                })
                .match(LeaveMessage.class, msg -> {
                    chatWindow.printText("*** parts: " + msg.userName);
                    currentChannelName = null;
                    userChannelActor = null;
                    chatWindow.removeChannel(msg.channel);
                })
                .match(IncomingKickMessage.class, msg -> {
                    if(msg.channel == null || msg.channel.equals(currentChannelName)){
                        chatWindow.printText("*** " + msg.userName + " kicked by " + msg.sender);
                        currentChannelName = null;
                        userChannelActor = null;
                    }
                    chatWindow.removeChannel(msg.channel);
                })
                .match(IncomingBanMessage.class, msg -> {
                    if(msg.channel == null || msg.channel.equals(currentChannelName)){
                        chatWindow.printText("*** " + msg.userName + " banned by " + msg.sender);
                        currentChannelName = null;
                        userChannelActor = null;
                    }
                    chatWindow.removeChannel(msg.channel);
                })
                .match(GetContentMessage.class, msg ->{
                    serverUserActor.forward(msg,getContext());
                })
                .match(SetContentMessage.class, msg -> {
                    chatWindow.setTitle("User: " + userName + "Channel: " + msg.channel);
                    chatWindow.setUserList(msg.userList);
                    chatWindow.setChatBox(msg.conversation);
                })
                .match(UserJoinedMessage.class, msg -> {
                    if(msg.channel == currentChannelName) {
                        chatWindow.addUser(msg.userName);
                        chatWindow.printText("*** joins: " + msg.userName);
                    }
                })
                .match(UserLeftMessage.class, msg -> {
                    if(msg.channel == currentChannelName) {
                        chatWindow.removeUser(msg.userName);
                        chatWindow.printText("*** parts: " + msg.userName);
                    }
                })
                .match(ExitMessage.class, msg -> {
                    serverUserActor.tell(msg,self());
                    self().tell(akka.actor.PoisonPill.getInstance(), self());
                })
                .build();
    }



    boolean verifyFormat(String[] arr) {
        boolean isFormatValid = false;

        if (arr[0].equals("/join") || arr[0].equals("/leave") || arr[0].equals("/disband")) {
            if (arr.length == 2) {
                isFormatValid = true;
            }
        } else if (arr[0].equals("/w") || arr[0].equals("/to")) {
            if (arr.length >= 3) {
                isFormatValid = true;
            }
        } else if (arr[0].equals("/kick") || arr[0].equals("/ban") || arr[0].equals("/title")) {
            if (arr.length == 3) {
                isFormatValid = true;
            }
        } else if (arr[0].equals("/add") || arr[0].equals("/remove")) {
            if (arr.length == 4) {
                if (arr[2].equals("v") || arr[2].equals("op")) {
                    isFormatValid = true;
                } else {
                    isFormatValid = false;
                }
            }
        }else if (!arr[0].startsWith("/")){// just text
            isFormatValid = true;
        }
        return isFormatValid;
    }

    private void handleTextCommand(String text) {
        if(text.equals("")) return;
        String[] cmdArr = text.split(" ");
        String cmd = cmdArr[0];
        if(!verifyFormat(cmdArr)) return;
        if (cmd.equals("/w")) { // normal user
            serverUserActor.forward(new OutgoingPrivateMessage(cmdArr[1], userName, text.split(cmdArr[1], 2)[1]), getContext());
        } else if (cmd.equals("/join")) {
            serverUserActor.forward(new JoinMessage(userName,cmdArr[1]), getContext());

        } else if (cmd.equals("/leave")) {
            serverUserActor.forward(new LeaveMessage(userName ,cmdArr[1]), getContext());

        } else if (cmd.equals("/title")) { // Voiced user
            serverUserActor.forward(new ChangeTitleMessage(cmdArr[2],cmdArr[1]), getContext());

        } else if (cmd.equals("/kick")) { // Channel operator
            serverUserActor.forward(new OutgoingKickMessage(cmdArr[1],userName,cmdArr[2]), getContext());

        } else if (cmd.equals("/ban")) {
            serverUserActor.forward(new OutgoingBanMessage(cmdArr[1],userName,cmdArr[2]), getContext());

        } else if (cmd.equals("/add")) {
            if (cmdArr[2].equals("v")) {
                serverUserActor.forward(new OutgoingAddVoicedMessage(cmdArr[3], userName,cmdArr[1]), getContext());
            } else if (cmdArr[2].equals("op")) {
                serverUserActor.forward(new OutgoingAddOperatorMessage(cmdArr[3], userName,cmdArr[1]), getContext());

            } else {
                chatWindow.invalidSyntax(text);
            }

        } else if (cmd.equals("/remove")) {
            if (cmdArr[2].equals("v")) {
                serverUserActor.forward(new OutgoingRemoveVoicedMessage(cmdArr[3], userName,cmdArr[1]), getContext());
            } else if (cmdArr[2].equals("op")) {
                serverUserActor.forward(new OutgoingRemoveOperatorMessage(cmdArr[3], userName,cmdArr[1]), getContext());
            } else {
                chatWindow.invalidSyntax(text);
            }

        } else if (cmd.equals("/to")) { // /to CHANNEL "Message"
            // send to a specific channel! BONUS FEATURE
            // also changes the current channel to the last one,
            // so further messages to the same one would not precede with "/to CHANNEL MESSAGE" format
           // currentChannelName = cmdArr[1];
        } else if (cmd.equals("/disband")) {
            serverUserActor.tell(new KillChannelMessage(cmdArr[1],userName), self());
        } else { // broadcast text message
            serverUserActor.forward(new OutgoingBroadcastMessage(userName, currentChannelName, text), getContext());
        }
    }
}
