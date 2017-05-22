import Shared.Messages.*;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.pattern.Patterns;
import akka.routing.Broadcast;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;

import scala.concurrent.duration.Duration;
import java.util.concurrent.TimeUnit;

public class ClientUserActor extends AbstractActor {
    String userName;
    ActorRef serverUserActor;


    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ConnectMessage.class, connMessage -> {

                }
        ).build();
    }

    @Override
    public void preStart() {

        // Initialize username
        //userName =

        ActorSelection server = getContext()
                .actorSelection("akka.tcp://IRCServer@127.0.0.1:3553/user/Server");

        // Send a connect request to the server
        ConnectMessage connMsg = new ConnectMessage();
        connMsg.userName = userName;
        final Timeout timeout = new Timeout(Duration.create(5, TimeUnit.SECONDS));
        Future<Object> ft = Patterns.ask(server, connMsg, timeout);
        try {
            // Wait for a reply from the server
            ConnectMessage serverReplyconnMsg = (ConnectMessage)Await.result(ft, timeout.duration());
            serverUserActor = serverReplyconnMsg.serverUserActor;

        } catch (Exception e) {
            // Show text message of "try again"
        }


    }

    boolean verifyFormat(String[] arr) {
        boolean isFormatValid = false;
        switch (arr[0]) {
            case "/w":
                if (arr.length == 3) {
                    isFormatValid = true;
                }
                break;

            case "/join":

                break;

            default: // just text
                isFormatValid = true;
                break;
        }
        return isFormatValid;
    }

    private void handleTextCommand(String text) {
        String[] cmdArr = text.split(" ");
        String cmd = cmdArr[0];
        verifyFormat(cmdArr);
        if (cmd.equals("/w")) { // normal user

            OutgoingTextMessage txtMsg = new OutgoingTextMessage();
            txtMsg.sendTo = cmdArr[1];
            txtMsg.text = cmdArr[2];

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

        } else if (cmd.equals("/ban")) {

        } else if (cmd.equals("/add")) {

        } else if (cmd.equals("/remove")) {

        } else { // broadcast text message
            BroadcastMessage brdMsg = new BroadcastMessage();
            brdMsg.text = text;

            serverUserActor.tell(brdMsg, self());
        }
    }

    public void SendPrivateMessage(String to, String messageText) {
        OutgoingTextMessage txtMsg = new OutgoingTextMessage();
        txtMsg.sendTo = to;
        txtMsg.text = messageText;

        serverUserActor.tell(txtMsg, serverUserActor);
    }

    public void sendCommand(String command) {
        serverUserActor.tell(command, sender());
    }

    public void joinChannel(String channelName) {
        //serverUserActor.

    }

}
