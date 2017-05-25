import akka.actor.AbstractActor;

import Shared.Messages.*;
import akka.routing.ActorRefRoutee;
import akka.routing.BroadcastRoutingLogic;
import akka.routing.RoundRobinRoutingLogic;
import akka.routing.Router;

public class ChannelActor extends AbstractActor {

    private String channelName;
    Router router;

    public ChannelActor(String channelName) {
        this.channelName = channelName;
        router = new Router(new BroadcastRoutingLogic());

    }

    @Override
    public void preStart(){

    }

    public String getChannelName() { return channelName; }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(JoinMessage.class, joinMsg -> {

                    JoinApprovalMessage approvalMessage = new JoinApprovalMessage();

                    // check if not already in channel ?

                    getContext().watch(sender());
                    router = router.addRoutee(new ActorRefRoutee(sender()));
                    approvalMessage.mode = joinMsg.userMode;
                    approvalMessage.joinedChannelName = channelName;

                    sender().tell(approvalMessage, self());

                    // tell the users in the channel that a new one has joined
                    IncomingBroadcastTextMessage incBrdTxtMsg = new IncomingBroadcastTextMessage();
                    incBrdTxtMsg.text = "User " + joinMsg.userName + " has joined.";
                    incBrdTxtMsg.sentFrom = null;
                    router.route(incBrdTxtMsg, self());
                })

                .match(OutgoingBroadcastMessage.class, broadMsg -> {

                    IncomingBroadcastTextMessage incBrdTxtMsg = new IncomingBroadcastTextMessage();
                    incBrdTxtMsg.text = broadMsg.text;
                    incBrdTxtMsg.sentFrom = broadMsg.sentFrom;
                    incBrdTxtMsg.channel = channelName;

                    router.route(incBrdTxtMsg, self());
                })
                .match(LeaveChannelMessage.class, leaveChMsg -> {
                    router = router.removeRoutee(sender());

                    IncomingBroadcastTextMessage incBrdTxtMsg = new IncomingBroadcastTextMessage();
                    incBrdTxtMsg.text = "User " + leaveChMsg.leavingUserName + " has left.";
                    incBrdTxtMsg.sentFrom = null;

                    router.route(incBrdTxtMsg, self());

                })
                .match(UserListInChannelMessage.class, uLstChMsg -> {
                    //router.routees()

                })
                .match(KickMessage.class, kckMsg -> { // used also for ban
                    router = router.removeRoutee(kckMsg.userActorToKick);

                    IncomingBroadcastTextMessage incBrdTxtMsg = new IncomingBroadcastTextMessage();
                    incBrdTxtMsg.text = "User " + kckMsg.userNameToKick + " was kicked.";

                    router.route(incBrdTxtMsg, self());

                }).build();
    }
}
