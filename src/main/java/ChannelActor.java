import akka.actor.AbstractActor;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import Shared.Messages.*;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.routing.ActorRefRoutee;
import akka.routing.RoundRobinRoutingLogic;
import akka.routing.Routee;
import akka.routing.Router;

public class ChannelActor extends AbstractActor {

    private String channelName;
    Router router = new Router(new RoundRobinRoutingLogic());

    public String getChannelName() { return channelName; }

    private LinkedList<ActorRef> onlineUsers;
    private LinkedList<ActorRef> bannedUsers;
    private LinkedList<ActorRef> operators;
    private LinkedList<ActorRef> voicedUsers;
    private ActorRef Owner;
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(JoinMessage.class, joinMsg -> {

                    JoinApprovalMessage approvalMessage = new JoinApprovalMessage();

                    if (!bannedUsers.contains(sender())) {
                        getContext().watch(sender());
                        router.addRoutee(new ActorRefRoutee(sender()));
                    } else { // notify it is banned
                        approvalMessage.approved = false;
                    }

                    sender().tell(approvalMessage, self());
                })
                /*
                .match(Work.class, message -> {
                    router.route(message, getSender());
                })
                .match(Terminated.class, message -> {
                    router = router.removeRoutee(message.actor());
                    ActorRef r = getContext().actorOf(Props.create(Worker.class));
                    getContext().watch(r);
                    router = router.addRoutee(new ActorRefRoutee(r));
                })
                */
                .match(BroadcastMessage.class, broadMsg -> {

                    IncomingBroadcastTextMessage incBrdTxtMsg = new IncomingBroadcastTextMessage();
                    incBrdTxtMsg.text = broadMsg.text;
                    incBrdTxtMsg.sentFrom = broadMsg.sentFrom;
                    incBrdTxtMsg.channel = channelName;
                    router.route(incBrdTxtMsg, self());
                })
                .match(LeaveChannelMessage.class, leaveChMsg -> {
                    router.removeRoutee(sender());
                })
                .match(UserListInChannelMessage.class, uLstChMsg -> {
                    router.routees()

                })
                .match(KickMessage.class, kckMsg -> {
                    //if (getSender())
                    //kckMsg.
                })
                .match(UpgradeModeMessage.class, upModeMsg -> {
                    ActorRef userActor = sender();

                    switch (upModeMsg.userMode) {
                        case USER:

                            break;

                        case VOICE:
                            if (operators.contains(getSender())) {

                            }
                            break;

                        case OPERATOR:

                            break;

                        case OWNER:

                            break;
                    }

                })
                .build();
    }
}
