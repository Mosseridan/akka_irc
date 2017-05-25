import Shared.Messages.*;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class ClientUserActor extends AbstractActor {
    String userName;
    ActorRef serverUserActor;
    ChatWindow chatWindow;
    String currentChannel;

    public ClientUserActor(String userName, ChatWindow chatWindow) {
        this.userName = userName;
        this.chatWindow = chatWindow;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(GUIMessage.class, msg -> {
                    handleTextCommand(msg.text);

                }).match(ConnectMessage.class, connMessage -> {
                    serverUserActor = connMessage.serverUserActor;
                    if (serverUserActor != null) {
                        printText("SYSTEM: {" + userName + "} Connected successfully.");
                    }

                }).match(String.class, str -> printText(str)

                ).match(JoinApprovalMessage.class, joinAppMsg -> {

                    if (joinAppMsg.mode == UserMode.USER) {
                        printText("SYSTEM: Successfully joined channel <" + joinAppMsg.joinedChannelName + ">.");
                    } else if (joinAppMsg.mode == UserMode.OWNER) {
                        printText("SYSTEM: Successfully created channel <" + joinAppMsg.joinedChannelName + ">.");
                    }
                    currentChannel = joinAppMsg.joinedChannelName;
                }).build();
    }

    @Override
    public void preStart() {

        // Initialize username
        //userName =

        ActorSelection server = getContext()
                .actorSelection("akka.tcp://IRC@127.0.0.1:2552/user/Server");

        // Send a connect request to the server
        ConnectMessage connMsg = new ConnectMessage();
        connMsg.userName = userName;

        server.tell(connMsg, self());

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

            OutgoingPrivateMessage txtMsg = new OutgoingPrivateMessage();
            txtMsg.sendTo = cmdArr[1];
            txtMsg.text = text.split(cmdArr[1], 2)[1];

            serverUserActor.tell(txtMsg, self());

        } else if (cmd.equals("/join")) {
            JoinMessage joinMsg = new JoinMessage();
            joinMsg.channelName = cmdArr[1];

            serverUserActor.tell(joinMsg, self());

        } else if (cmd.equals("/leave")) {
            LeaveChannelMessage leaveMsg = new LeaveChannelMessage();
            leaveMsg.channelToLeave = cmdArr[1];

            serverUserActor.tell(leaveMsg, self());

        } else if (cmd.equals("/title")) { // Voiced user

        } else if (cmd.equals("/kick")) { // Channel operator
            OutgoingPromoteDemoteMessage proDemoMsg = new OutgoingPromoteDemoteMessage();
            proDemoMsg.userMode = UserMode.OUT;
            proDemoMsg.promotedDemotedUser = cmdArr[1];
            proDemoMsg.channel = cmdArr[2];

            serverUserActor.tell(proDemoMsg, self());
        } else if (cmd.equals("/ban")) {
            OutgoingPromoteDemoteMessage proDemoMsg = new OutgoingPromoteDemoteMessage();

            proDemoMsg.userMode = UserMode.BANNED;
            proDemoMsg.promotedDemotedUser = cmdArr[1];
            proDemoMsg.channel = cmdArr[2];

            serverUserActor.tell(proDemoMsg, self());
        } else if (cmd.equals("/add")) {
            OutgoingPromoteDemoteMessage proDemoMsg = new OutgoingPromoteDemoteMessage();
            if (cmdArr[2].equals("v")) {
                proDemoMsg.userMode = UserMode.VOICE;
            } else if (cmdArr[2].equals("op")) {
                proDemoMsg.userMode = UserMode.OPERATOR;
            }
            proDemoMsg.channel = cmdArr[1];
            proDemoMsg.promotedDemotedUser = cmdArr[3];

            serverUserActor.tell(proDemoMsg, self());

        } else if (cmd.equals("/remove")) {
            OutgoingPromoteDemoteMessage proDemoMsg = new OutgoingPromoteDemoteMessage();
            proDemoMsg.userMode = UserMode.USER;
            proDemoMsg.channel = cmdArr[1];
            proDemoMsg.promotedDemotedUser = cmdArr[3];

            serverUserActor.tell(proDemoMsg, self());

        } else if (cmd.equals("/to")) { // /to CHANNEL "Message"
            // send to a specific channel! BONUS FEATURE
            // also changes the current channel to the last one,
            // so further messages to the same one would not precede with "/to CHANNEL MESSAGE" format
            OutgoingBroadcastMessage outBrdMsg = new OutgoingBroadcastMessage();
            outBrdMsg.toChannel = cmdArr[1];
            currentChannel = cmdArr[1];
            outBrdMsg.text = text.split(cmdArr[1], 2)[1];

            serverUserActor.tell(outBrdMsg, self());
        } else { // broadcast text message
            OutgoingBroadcastMessage outBrdMsg = new OutgoingBroadcastMessage();
            outBrdMsg.text = text;
            outBrdMsg.toChannel = currentChannel;

            serverUserActor.tell(outBrdMsg, self());
        }
    }

    private void printText(String text) {
        String message = "[" + LocalTime.now().toString() + "] " + text + "\n";
        chatWindow.textAreaOutput.append(message);
    }

}
