import Shared.Messages.*;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;

public class ClientUserActor extends AbstractActor {
    String userName;
    ActorRef serverUserActor;
    ChatWindow chatWindow;
    public String currentChannel;

    public ClientUserActor(String userName, ChatWindow chatWindow) {
        this.userName = userName;
        this.chatWindow = chatWindow;
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
                    chatWindow.printText(msg.channel,msg.message);
                })
                .match(ConnectMessage.class, msg -> {
                    serverUserActor = msg.serverUserActor;
                    if (serverUserActor != null) {
                        chatWindow.printText("<" + userName + "> Connected successfully.");
                    }else {
                        chatWindow.printText("Something went wrong while connecting.");
                    }
                })
                .match(JoinApprovalMessage.class, msg -> { //TODO: ADD JOINED CHANNEL INDICATION
                    chatWindow.addChannel(msg.channelName,msg.channelUsers);
                    if (msg.mode == UserMode.OWNER) {
                        chatWindow.printText(msg.channelName, "*** Created channel: " + msg.channelName);
                    }
                    chatWindow.printText(msg.channelName, "*** joins: " + msg.userName);
                    currentChannel = msg.channelName;
                    //chatWindow.setUserList(msg.channelName, msg.channelUsers);
                    chatWindow.setTitle("User: " + userName + ", Channel: " + msg.channelName);
                })
                .match(LeaveMessage.class, msg -> {
                    chatWindow.printText(msg.channel, "*** parts: " + msg.userName);
                    //TODO
                })
                .match(UserJoinedMessage.class, msg -> {
                    chatWindow.addUser(msg.channel,userName);
                    chatWindow.printText(msg.channel, "*** joins: " + msg.userName);
                })
                .match(UserLeftMessage.class, msg-> {
                    chatWindow.removeUser(msg.channel, msg.userName);
                    chatWindow.printText(msg.channel, "*** parts: " + msg.userName);
                })
                .match(SetUserListMessage.class, msg->{
                    chatWindow.setUserList(msg.channel, msg.userList);
                })
//                .match(GetChannelListMessage.class, getChLstMsg -> {
//                    serverUserActor.tell(getChLstMsg, self());
//                })
//                .match(SetChannelListMessage.class, setChLstMsg -> {
//                    System.out.println("!!!! got SetChannelListMessage");
//                    printText("!!!! got SetChannelListMessage");
//                   // printText(setChLstMsg.channels);
//                    //chatWindow.setChannels(setChLstMsg.channels);
//                })
//                .match(GetUserListInChannelMessage.class, getUlstChMsg -> {
//                    System.out.println("! got GetUserListInChannelMessage");
//                    serverUserActor.tell(getUlstChMsg, self());
//                })
//                .match(SetUserListInChannelMessage.class, setULstChMsg -> {//TODO: FIX THIS
//                    System.out.println("! got SetUserListInChannelMessage");
//                    // append to user list text pane (lock)
//                    //chatWindow.textPaneUsersInChannelList
//                    //chatWindow.usersInChannel.add(setULstChMsg.user);
//                    //ulChMsg.users
//                    //ulChMsg
//                })
                .build();
    }

    @Override
    public void preStart() {

        // Initialize username
        //userName =

        ActorSelection server = getContext()
                .actorSelection("akka.tcp://IRC@127.0.0.1:2552/user/Server");

        // Send a connect request to the server
        server.tell(new ConnectMessage(userName), self());

//        final Timeout timeout = new Timeout(Duration.create(5, TimeUnit.SECONDS));
//        Future<Object> ft = Patterns.ask(server, connMsg, timeout);
//        try {
//            // Wait for a reply from the server
//            ConnectMessage serverReplyconnMsg = (ConnectMessage)Await.result(ft, timeout.duration());
//            serverUserActor = serverReplyconnMsg.serverUserActor;
//
//        } catch (Exception e) {
//            // Show text message of "try again"
//        }


    }

    boolean verifyFormat(String[] arr) {
        boolean isFormatValid = false;

        if (arr[0].equals("/join") || arr[0].equals("/leave")) {
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
        } else { // just text
            isFormatValid = true;
        }
        return isFormatValid;
    }

    private void handleTextCommand(String text) {
        String[] cmdArr = text.split(" ");
        String cmd = cmdArr[0];
        verifyFormat(cmdArr);
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
            currentChannel = cmdArr[1];
            serverUserActor.forward(new OutgoingBroadcastMessage(userName, cmdArr[1], text.split(cmdArr[1], 2)[1]), getContext());

        } else { // broadcast text message
            serverUserActor.forward(new OutgoingBroadcastMessage(userName, currentChannel, text), getContext());

        }
    }
}
