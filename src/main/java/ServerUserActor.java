import Shared.Messages.*;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.pattern.Patterns;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

public class ServerUserActor extends AbstractActor {



    String userName;
    ActorRef clientUserActor;
    String actorsPath = "akka.tcp://IRC_akka@127.0.0.1:3553/user/";
    String clientUser = "ClientUser";
    ActorSelection channelCreatorSel;
    final int joinChannelTimeoutSeconds = 3;

    @Override
    public Receive createReceive() {
        return receiveBuilder()



                .match(OutgoingTextMessage.class, txtMsg -> { // send a message to another actor
                    ActorSelection serverActorSel = getContext().actorSelection("ServerUser" + txtMsg.sendTo);
                    ActorRef serverActor = HelperFunctions.getActorRefBySelection(serverActorSel);
                    IncomingTextMessage incomingTxtMsg = new IncomingTextMessage();
                    incomingTxtMsg.sentFrom = userName;
                    incomingTxtMsg.text = txtMsg.text;

                    serverActor.tell(incomingTxtMsg, self());
                })
                .match(IncomingTextMessage.class, txtMsg -> { // A Private message from another user
                    // Show PM in UI
                    // clientUserActor

                })
                .match(IncomingBroadcastTextMessage.class, incBrdTxtMsg -> { // Broadcast from another user
                    // Show Broadcast in UI
                    // clientUserActor
                })
                .match(JoinMessage.class, joinMsg -> {
                    ActorRef channelCreator = HelperFunctions.getActorRefBySelection(channelCreatorSel);

                    final Timeout timeout = new Timeout(Duration.create(1, TimeUnit.SECONDS));
                    Future<Object> future = Patterns.ask(channelCreator, joinMsg, timeout);
                    try {
                        JoinApprovalMessage msg = (JoinApprovalMessage) Await.result(future, timeout.duration());
                        if (msg.approved) {
                            // tell the client

                            msg.mode
                        } else {
                            // also tell the client
                        }

                    } catch (Exception e) {

                    }


                })
                .match(JoinApprovalMessage.class, joinAppMsg -> {
                    ActorSelection userActorSel = getContext().getSystem().actorSelection(clientUser + userName);
                    ActorRef clientUser = HelperFunctions.getActorRefBySelection(userActorSel);
                    clientUser.tell(joinAppMsg, self());

                })
                .match(LeaveChannelMessage.class, leaveChMsg -> {
                    ActorSelection sel = getContext().actorSelection("/user/ChannelCreator/" + leaveChMsg.channelToLeave);
                    ActorRef channel = HelperFunctions.getActorRefBySelection(sel);

                    channel.tell(leaveChMsg, self());
                })
                .match(BroadcastMessage.class, brdMsg -> {
                    ActorSelection sel = getContext().actorSelection("/user/ChannelCreator/" + brdMsg.toChannel);
                    ActorRef channel = HelperFunctions.getActorRefBySelection(sel);

                    brdMsg.sentFrom = userName;
                    channel.tell(brdMsg, self());
                })
                .match(ChannelListMessage.class, chLstMsg -> {
                    ActorSelection sel = getContext().getSystem().actorSelection("/user/ChannelCreator");
                    ActorRef channelCreator = HelperFunctions.getActorRefBySelection(sel);
                    channelCreator.
                    // get list
                })
                .match(ConnectMessage.class, connMsg -> {
                    clientUserActor = connMsg.clientUserActor;
                })
                .match(UpgradeModeMessage.class, upModeMessage -> {


                })
                .build();
    }

    @Override
    public void preStart() {
        // set user name
        //userName =
        channelCreatorSel = getContext().actorSelection("ChannelCreator");
    }



    public void promoteAnotherUser(String channelName, String userNameToPromote, String promotionLevel) {
        ActorSelection channelActor = getContext().system().actorSelection(actorsPath + channelName);
        UpgradeModeMessage upgradeModeMessage = new UpgradeModeMessage();
        upgradeModeMessage.upgradedUser = userNameToPromote;
        upgradeModeMessage.userMode = upgradeModeMessage.textToUserMode(promotionLevel);


        channelActor.tell(upgradeModeMessage, self());
    }


}
