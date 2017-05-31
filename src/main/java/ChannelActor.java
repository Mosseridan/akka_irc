import akka.actor.AbstractActor;

import Shared.Messages.*;
import akka.actor.Terminated;
import akka.routing.*;
import scala.collection.IndexedSeq;
import scala.collection.Traversable;

public class ChannelActor extends AbstractActor {

    private String channelName;
    private String title;
    Router router;

    public ChannelActor(String channelName) {
        this.channelName = channelName;
        router = new Router(new BroadcastRoutingLogic());
        title = null;
    }

    @Override
    public void preStart(){

    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(JoinMessage.class, joinMsg -> {

                    JoinApprovalMessage approvalMessage = new JoinApprovalMessage();

                    // check if not already in channel ?

                    // if only one user, make it admin.
                    if (router.routees().isEmpty()) {
                        approvalMessage.mode = UserMode.OWNER;
                    } else {
                        approvalMessage.mode = UserMode.USER;
                    }

                    getContext().watch(sender());
                    router = router.addRoutee(new ActorRefRoutee(sender()));

                    approvalMessage.joinedChannelName = channelName;
                    approvalMessage.joinedChannel = self();

                    sender().tell(approvalMessage, self());

                    // tell the users in the channel that a new one has joined
                    broadcastMessage("User " + joinMsg.userName + " has joined.");
                })

                .match(OutgoingBroadcastMessage.class, broadMsg -> {
                    if (broadMsg.sentFrom == null) {
                        broadcastMessage(broadMsg.text);
                    } else {
                        broadcastMessage("{" + broadMsg.sentFrom + "}: " + broadMsg.text);
                    }
                })
                .match(KillChannelMessage.class, dsbMsg -> {
                    broadcastMessage("Owner disbands channel. closing");
                    killChannel();
                })
                .match(LeaveChannelMessage.class, leaveChMsg -> {
                    router = router.removeRoutee(sender());

                    if (router.routees().isEmpty()) {

                        broadcastMessage("Channel is now empty. closing");

                        //kill channel
                        killChannel();

                    } else if (leaveChMsg.userModeOfLeavingUser == UserMode.OWNER) {
                        IncomingPromoteDemoteMessage incPrmDemMsg = new IncomingPromoteDemoteMessage();
                        incPrmDemMsg.newUserMode = UserMode.OWNER;

                        Routee promotee = router.routees().head();
                        promotee.send(incPrmDemMsg, self());
                    }

                    sender().tell(akka.actor.PoisonPill.getInstance(), self());
                    broadcastMessage("User " + leaveChMsg.leavingUserName + " has left.");

                })
                .match(GetUserListInChannelMessage.class, ulChMsg -> {
                    router.route(ulChMsg, sender());
                })
                .match(ChangeTitleMessage.class, chTlMsg -> {
                    title = chTlMsg.newTitle;
                    broadcastMessage("Channel title changed.");
                })
                .match(Terminated.class, message -> {
                    //router = router.removeRoutee(message.actor());
                    IncomingPromoteDemoteMessage f = new IncomingPromoteDemoteMessage(); // JUST FOR DEBUG
                    f.newUserMode = UserMode.OWNER; // JUST FOR DEBUG
                })
                .match(RemoveFromChannelMessage.class, rmMsg -> {
                    router = router.removeRoutee(sender());
                }).build();
    }

    private void killChannel() {
        KillChannelMessage klChMsg = new KillChannelMessage();
        klChMsg.channelName = channelName;

        router.route(akka.actor.PoisonPill.getInstance(), self());

        getContext().parent().tell(klChMsg, self());
    }

    private void broadcastMessage(String message) {
        IncomingBroadcastTextMessage incBrdTxtMsg = new IncomingBroadcastTextMessage();
        incBrdTxtMsg.text = "<" + channelName + (title != null ? ": ~" + title : "") + "> " + message;

        router.route(incBrdTxtMsg, self());


    }
}
