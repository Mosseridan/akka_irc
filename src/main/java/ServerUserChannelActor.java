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
            joinChannel(joinMsg);
        })
        .match(JoinApprovalMessage.class, joinAppMsg -> {
            //channel = joinAppMsg.joinedChannel;
            userMode = joinAppMsg.mode;
            joinAppMsg.joinedChannelName = channelName;
            joinAppMsg.joinedChannel = null;

            clientUserActor.tell(joinAppMsg, self());
        })
        .match(LeaveChannelMessage.class, leaveChMsg -> {
            if (userMode == UserMode.BANNED)
            {
                tellClientSystem("Banned, cannot leave channel");
            } else {
                ActorSelection sel = getContext().actorSelection(channelCreatorPath + "/" + channelName);
                ActorRef channelToLeave = HelperFunctions.getActorRefBySelection(sel);
                leaveChMsg.userModeOfLeavingUser = userMode;
                if (channelToLeave != null) {
                    channelToLeave.tell(leaveChMsg, self());
                    tellClient("You have left the channel");
                    //userMode = UserMode.OUT;
                } else {
                    tellClientSystem("Cannot leave a channel that you are not in it");
                }
            }

        }).match(OutgoingBroadcastMessage.class, brdMsg -> {
            if (userMode == UserMode.BANNED) {
                tellClientSystem("Banned, cannot send messages to channel.");
            } else {
                ActorSelection sel = getContext().actorSelection(channelCreatorPath + "/" + channelName);
                ActorRef channelToBroadcast = HelperFunctions.getActorRefBySelection(sel);
                brdMsg.sentFrom = ((userMode == UserMode.OWNER || userMode == UserMode.OPERATOR)
                        ? "@"
                        : (userMode == UserMode.VOICE ? "+" : "")) + brdMsg.sentFrom;
                channelToBroadcast.tell(brdMsg, self());
            }

        }).match(ChangeTitleMessage.class, chTlMsg -> {
            if (userMode == UserMode.VOICE || userMode == UserMode.OPERATOR || userMode == UserMode.OWNER) {
                ActorSelection sel = getContext().actorSelection(channelCreatorPath + "/" + channelName);
                ActorRef channelToBroadcast = HelperFunctions.getActorRefBySelection(sel);

                channelToBroadcast.tell(chTlMsg, self());
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
            } else if (userMode == UserMode.BANNED) {
                tellClientSystem("Out of channel, cannot change others mode");
            } else { // can't promote/demote no one
                tellClientSystem("You don't have the sufficient privileges to change others mode");
            }

        })
        .match(IncomingPromoteDemoteMessage.class, incPrmDemMsg -> {
            if (userMode != UserMode.OWNER && userMode != UserMode.OPERATOR) {

                if (userMode == UserMode.BANNED)
                    return;
                }

                userMode = incPrmDemMsg.newUserMode;
                OutgoingBroadcastMessage outBrdMsg = new OutgoingBroadcastMessage();
                outBrdMsg.sentFrom = null;

                if (userMode == UserMode.BANNED) {
                    getRemovedFromChannel();

                    tellClient("Banned from {" + channelName + "}.");
                    outBrdMsg.text = "User {" + userName + "} was banned.";
                } else if (userMode == UserMode.OPERATOR) {
                    //tellClient("Status changed to OPERATOR.");
                    outBrdMsg.text = "User {" + userName + "} was changed to OPERATOR.";
                } else if (userMode == UserMode.USER) {
                    //tellClient("Status changed to USER.");
                    outBrdMsg.text = "User {" + userName + "} was changed to USER.";
                } else if (userMode == UserMode.VOICE) {
                    //tellClient("Status changed to VOICED.");
                    outBrdMsg.text = "User {" + userName + "} was changed to VOICED";
                } else if (userMode == UserMode.OWNER) {
                    //tellClient("Status changed to OWNER.");
                    outBrdMsg.text = "User {" + userName + "} was changed to OWNER.";
                } else if (incPrmDemMsg.newUserMode == UserMode.OUT) {
                    getRemovedFromChannel();

                    tellClient("Kicked from {" + channelName + "}.");
                    outBrdMsg.text = "User " + userName + " was kicked.";
                }


                ActorSelection sel = getContext().actorSelection(channelCreatorPath + "/" + channelName);
                ActorRef channel = HelperFunctions.getActorRefBySelection(sel);

                channel.tell(outBrdMsg, self());

                if (incPrmDemMsg.newUserMode == UserMode.OUT) {
                    GotKickedMessage gotKickedMessage = new GotKickedMessage();
                    getContext().parent().tell(gotKickedMessage, self());
                }
        }).match(SetUserListInChannelMessage.class, setUlChMsg -> {
            // tell the client my channel name
            SetUserListInChannelMessage setUlLstChMsg = new SetUserListInChannelMessage();
            setUlLstChMsg.user = userName;
            //ulChMsg.channelName = channelName;
            sender().tell(setUlChMsg, self());
        }).match(SetChannelListMessage.class, setUlChMsg -> {
            clientUserActor.tell(setUlChMsg, self());
            sender().tell(setUlChMsg, self());
        })
        .match(KillChannelMessage.class, klChMsg -> {
            if (userMode == UserMode.OWNER) {
                ActorSelection sel = getContext().actorSelection(channelCreatorPath + "/" + channelName);
                ActorRef channel = HelperFunctions.getActorRefBySelection(sel);

                channel.tell(klChMsg, self());
            }
        }).build();
    }

    private void joinChannel(JoinMessage joinMsg) {
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
            } else { // channel already exist
                //if (channelToJoin != channel) { // ask it directly to join
                    channelToJoin.tell(joinMsg, self());
                //} else {
                   // tellClientSystem("Already in the channel.");
                //}
            }
        }
    }


    private void getRemovedFromChannel() {
        ActorSelection sel = getContext().actorSelection(channelCreatorPath + "/" + channelName);
        ActorRef channel = HelperFunctions.getActorRefBySelection(sel);

        RemoveFromChannelMessage rmMsg = new RemoveFromChannelMessage();
        channel.tell(rmMsg, self());
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
