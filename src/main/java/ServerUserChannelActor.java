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
        this.channel = null;
        userMode = UserMode.USER;
    }


    @Override
    public Receive createReceive() {
        return receiveBuilder().match(JoinMessage.class, joinMsg -> {
            if (channel == null) {
                if (userMode == UserMode.BANNED) { // Create / Join the channel
                    tellClient("Banned, cannot join channel");
                } else {
                    joinMsg.userName = userName;
                    ActorSelection channelSel = getContext().actorSelection(channelCreatorPath + "/" + joinMsg.channelName);
                    ActorRef channelToJoin = HelperFunctions.getActorRefBySelection(channelSel);

                    if (channelToJoin == null) { // channel does not exist
                        // get the ChannelCreator actor
                        ActorSelection channelCreatorSel = getContext().actorSelection(channelCreatorPath);
                        ActorRef channelCreator = HelperFunctions.getActorRefBySelection(channelCreatorSel);

                        // ask ChannelCreator to join it
                        channelCreator.tell(joinMsg, self());
                    } else { // channel already exist, ask it directly
                        channelToJoin.tell(joinMsg, self());
                    }
                }
            } else {
                tellClientSystem("Already in the channel.");
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
            //ActorSelection sel = getContext().actorSelection(channelCreatorPath + "/" + channelName);
            //ActorRef channelToLeave = HelperFunctions.getActorRefBySelection(sel);
            leaveChMsg.userModeOfLeavingUser = userMode;
            if (channel != null) {
                channel.tell(leaveChMsg, self());
                userMode = UserMode.OUT;
            } else {
                tellClientSystem("Cannot leave a channel that you are not in it");
            }

        }).match(OutgoingBroadcastMessage.class, brdMsg -> {
            if (channel != null) {
                if (userMode != UserMode.BANNED && userMode != UserMode.OUT) {
                    //ActorSelection sel = getContext().actorSelection(channelCreatorPath + "/" + channelName);
                    //ActorRef channelToBroadcast = HelperFunctions.getActorRefBySelection(sel);
                    brdMsg.sentFrom = ((userMode == UserMode.OWNER || userMode == UserMode.OPERATOR)
                            ? "@"
                            : (userMode == UserMode.VOICE ? "+" : "")) + brdMsg.sentFrom;
                    channel.tell(brdMsg, self());
                } else if (userMode == UserMode.BANNED) {
                    tellClientSystem("Banned, cannot send messages to channel.");
                } else if (userMode == UserMode.OUT) {
                    tellClientSystem("Out of channel, needs to join first. Cannot send.");
                }
            } else {
                tellClientSystem("Must join channel first");
            }


        }).match(ChangeTitleMessage.class, chTlMsg -> {
            if (userMode == UserMode.VOICE || userMode == UserMode.OPERATOR || userMode == UserMode.OWNER) {
                //ActorSelection sel = getContext().actorSelection(channelCreatorPath + "/" + channelName);
                //ActorRef channelToBroadcast = HelperFunctions.getActorRefBySelection(sel);

                channel.tell(chTlMsg, self());
            } else if (userMode == UserMode.OUT) {
                tellClientSystem("Out of channel, cannot change title");
            } else if (userMode == UserMode.USER) {
                tellClientSystem("Ordinary user cannot change title");
            } else if (userMode == UserMode.BANNED) {
                tellClientSystem("Banned, cannot change title");
            }
        })
        .match(IncomingBroadcastTextMessage.class, incBrdTxtMsg -> { // Broadcast from the channel
            tellClient(incBrdTxtMsg.text);

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

                String messageToClient = null;
                String messageToParticipants = null;
                if (userMode == UserMode.BANNED) {
                    getKickedFromChannel();
                    messageToClient = "Banned.";
                    messageToParticipants = "User " + userName + " was banned.";
                } else if (userMode == UserMode.OPERATOR) {
                    messageToClient = "Status changed to OPERATOR.";
                    messageToParticipants = "User " + userName + " was changed to OPERATOR.";
                } else if (userMode == UserMode.USER) {
                    messageToClient = "Status changed to USER.";
                    messageToParticipants = "User " + userName + " was changed to USER.";
                } else if (userMode == UserMode.VOICE) {
                    messageToClient = "Status changed to VOICED.";
                    messageToParticipants = "User " + userName + " was changed to VOICED";
                } else if (userMode == UserMode.OWNER) {
                    messageToClient = "Status changed to OWNER.";
                    messageToParticipants = "User " + userName + " was changed to OWNER.";
                } else if (userMode == UserMode.OUT) {
                    getKickedFromChannel();
                    messageToClient = "Kicked.";
                    messageToParticipants = "User " + userName + " was kicked.";
                }
                OutgoingBroadcastMessage outBrdMsg = new OutgoingBroadcastMessage();
                outBrdMsg.text = messageToParticipants;
                outBrdMsg.sentFrom = userName;

                channel.tell(outBrdMsg, self());
                tellClient(messageToClient);
            }
        }).match(GetUserListInChannelMessage.class, setUlChMsg -> {
            // tell the client my channel name
            SetUserListInChannelMessage setUlLstChMsg = new SetUserListInChannelMessage();
            setUlLstChMsg.user = userName;
            //ulChMsg.channelName = channelName;
            sender().tell(setUlChMsg, self());
        }).build();
    }

    private void getKickedFromChannel() {
        KickMessage kckMsg = new KickMessage();
        kckMsg.userNameToKick = userName;
        kckMsg.channel = channelName;
        kckMsg.userActorToKick = self();

        //ActorSelection sel = getContext().actorSelection(channelCreatorPath + "/" + channelName);
        //ActorRef channelToKickFrom = HelperFunctions.getActorRefBySelection(sel);

        channel.tell(kckMsg, self());

        //
        GotKickedMessage gotKickedMessage = new GotKickedMessage();
        getContext().parent().tell(gotKickedMessage, self());
    }


    private void tellClient(String message) {
        clientUserActor.tell(message, self());
    }
    private void tellClientSystem(String message) {
        tellClient("SYSTEM: " + message);
    }

    @Override
    public void preStart() {
        serverUserActor = getContext().parent();
    }
}
