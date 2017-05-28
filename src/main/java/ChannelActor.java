import akka.actor.AbstractActor;

import Shared.Messages.*;
import akka.routing.ActorRefRoutee;
import akka.routing.BroadcastRoutingLogic;
import akka.routing.RoundRobinRoutingLogic;
import akka.routing.Router;

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
                    broadcastMessage("{" + broadMsg.sentFrom + "}: " + broadMsg.text);
                })
                .match(LeaveChannelMessage.class, leaveChMsg -> {
                    router = router.removeRoutee(sender());
                    broadcastMessage("User " + leaveChMsg.leavingUserName + " has left.");

                    // arbitrarily select another owner
                    if (leaveChMsg.userModeOfLeavingUser == UserMode.OWNER) {
                        IncomingPromoteDemoteMessage incPrmDemMsg = new IncomingPromoteDemoteMessage();
                        incPrmDemMsg.newUserMode = UserMode.OWNER;
                        router.routees().head().send(incPrmDemMsg, self());
                    }
                })
                .match(UserListInChannelMessage.class, uLstChMsg -> {
                    //router.routees()

                })
                .match(UserListInChannelMessage.class, ulChMsg -> {
                    router.route(ulChMsg, sender());
                })
                .match(ChangeTitleMessage.class, chTlMsg -> {
                    title = chTlMsg.newTitle;
                    broadcastMessage("Channel title changed.");
                })
                .match(KickMessage.class, kckMsg -> { // used also for ban
                    router = router.removeRoutee(kckMsg.userActorToKick);

                    broadcastMessage("User " + kckMsg.userNameToKick + " was kicked.");

                }).build();
    }

    private void broadcastMessage(String message) {
        IncomingBroadcastTextMessage incBrdTxtMsg = new IncomingBroadcastTextMessage();
        incBrdTxtMsg.text = "<" + channelName + (title != null ? ": ~" + title : "") + "> " + message;

        router.route(incBrdTxtMsg, self());


    }
}
