import Shared.Messages.*;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;

public class ServerUserChannelActor extends AbstractActor {

    String userName;
    String channelName;
    ActorRef clientUserActor;
    ActorRef channel;
    ActorRef serverUserActor;
    private UserMode userMode;

    final String serverUserPath = "/user/Server/ServerUser";
    final String channelCreatorPath = "/user/Server/ChannelCreator/";


    public ServerUserChannelActor(String userName, String channelName, ActorRef clientUserActor) {
        this.userName = userName;
        this.clientUserActor = clientUserActor;
        this.channelName = channelName;
        userMode = UserMode.USER;
    }


    @Override
    public Receive createReceive() {
        return receiveBuilder().match(JoinMessage.class, joinMsg -> {

            if (userMode == UserMode.BANNED) { // Create / Join the channel
                tellClient("Banned, cannot join channel");
            } else {
                joinMsg.userName = userName;
                ActorSelection channelSel = getContext().actorSelection(channelCreatorPath + "/" + joinMsg.channelName);
                ActorRef channel = HelperFunctions.getActorRefBySelection(channelSel);

                if (channel == null) { // channel does not exist
                    // get the ChannelCreator actor
                    ActorSelection channelCreatorSel = getContext().actorSelection(channelCreatorPath);
                    ActorRef channelCreator = HelperFunctions.getActorRefBySelection(channelCreatorSel);

                    // ask ChannelCreator to join it
                    channelCreator.tell(joinMsg, self());
                } else { // channel already exist, ask it directly
                    channel.tell(joinMsg, self());
                }
            }
        })
        .match(JoinApprovalMessage.class, joinAppMsg -> {
            channel = joinAppMsg.joinedChannel;
            userMode = joinAppMsg.mode;
            joinAppMsg.joinedChannelName = channelName;
            joinAppMsg.joinedChannel = null;

            clientUserActor.tell(joinAppMsg, self());
        })
        .match(LeaveChannelMessage.class, leaveChMsg -> {
            ActorSelection sel = getContext().actorSelection(channelCreatorPath + "/" + channelName);
            ActorRef channelToLeave = HelperFunctions.getActorRefBySelection(sel);

            channelToLeave.tell(leaveChMsg, self());

            userMode = UserMode.OUT;

        }).match(OutgoingBroadcastMessage.class, brdMsg -> {
            if (userMode != UserMode.BANNED && userMode != UserMode.OUT) {
                ActorSelection sel = getContext().actorSelection(channelCreatorPath + "/" + channelName);
                ActorRef channelToBroadcast = HelperFunctions.getActorRefBySelection(sel);

                channelToBroadcast.tell(brdMsg, self());
            } else if (userMode == UserMode.BANNED) {
                tellClientSystem("Banned, cannot send messages to channel.");
            } else if (userMode == UserMode.OUT) {
                tellClientSystem("Out of channel, needs to join first. Cannot send.");
            }


        }).match(IncomingBroadcastTextMessage.class, incBrdTxtMsg -> { // Broadcast from another user / the channel
            if (incBrdTxtMsg.sentFrom == null) { // sent from channel
                tellClient(incBrdTxtMsg.text);
            } else { // sent from a specific user
                tellClient("{" + incBrdTxtMsg.sentFrom + "}: "+ incBrdTxtMsg.text);
            }

        }).match(OutgoingPromoteDemoteMessage.class, prmDemUsrMsg -> {
            if (userMode == UserMode.OWNER || userMode == UserMode.OPERATOR) {

                // get the userChannel for mode changing
                ActorSelection sel = getContext().actorSelection(serverUserPath + prmDemUsrMsg.promotedDemotedUser + "/" + channelName);
                ActorRef userChannelToPromDem = HelperFunctions.getActorRefBySelection(sel);

                if (userChannelToPromDem != null) {
                    // craft a message for it
                    IncomingPromoteDemoteMessage incPrmDemMsg = new IncomingPromoteDemoteMessage();
                    incPrmDemMsg.newUserMode = prmDemUsrMsg.userMode;

                    // tell it to change its mode
                    userChannelToPromDem.tell(incPrmDemMsg, self());
                } else {
                    tellClientSystem("Requested user for state changing is not in channel");
                }
            } else if (userMode == UserMode.BANNED || userMode == UserMode.OUT) {
                tellClientSystem("Out of channel, cannot change others mode");
            } else { // can't promote/demote no one
                tellClientSystem("You don't have the sufficient privileges to change others mode");
            }

        })
        .match(IncomingPromoteDemoteMessage.class, incPrmDemMsg -> {
            if (userMode != UserMode.OWNER && userMode != UserMode.OPERATOR) {

                UserMode prevUserMode = userMode;
                userMode = incPrmDemMsg.newUserMode;

                String message = null;
                if (userMode == UserMode.BANNED) {
                    getKickedFromChannel();
                    message = "Banned.";
                } else if (userMode == UserMode.OPERATOR) {
                    message = "Status changed to OPERATOR.";
                } else if (userMode == UserMode.USER) {
                    message = "Status changed to USER.";
                } else if (userMode == UserMode.VOICE) {
                    message = "Status changed to VOICED.";
                } else if (userMode == UserMode.OWNER) {
                    message = "Status changed to OWNER.";
                } else if (userMode == UserMode.OUT) {
                    getKickedFromChannel();
                    message = "Kicked.";
                }
                tellClient(message);
            }
        }).build();
    }

    private void getKickedFromChannel() {
        KickMessage kckMsg = new KickMessage();
        kckMsg.userNameToKick = userName;
        kckMsg.channel = channelName;
        kckMsg.userActorToKick = self();

        ActorSelection sel = getContext().actorSelection(channelCreatorPath + "/" + channelName);
        ActorRef channelToKickFrom = HelperFunctions.getActorRefBySelection(sel);

        channelToKickFrom.tell(kckMsg, self());
    }


    private void tellClient(String message) {
        clientUserActor.tell("<" + channelName + "> " + message, self());
    }
    private void tellClientSystem(String message) {
        tellClient("SYSTEM: " + message);
    }

    @Override
    public void preStart() {
        serverUserActor = getContext().parent();
    }
}
