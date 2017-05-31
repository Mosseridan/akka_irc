import Shared.Messages.*;
import akka.actor.*;

public class ServerUserActor extends AbstractActor {



    String userName;
    ActorRef clientUserActor;
    final String serverUserPath = "/user/Server/ServerUser";
    final String channelCreatorPath = "/user/Server/ChannelCreator";

    public ServerUserActor(String username) {
        this.userName = username;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(OutgoingPrivateMessage.class, txtMsg -> { // send a message to another actor
                    ActorSelection serverActorSel = getContext().actorSelection(serverUserPath + txtMsg.sendTo);
                    ActorRef serverUserActor = HelperFunctions.getActorRefBySelection(serverActorSel);

                    if (serverUserActor != null) {

                        IncomingPrivateMessage incomingTxtMsg = new IncomingPrivateMessage();
                        incomingTxtMsg.sentFrom = userName;
                        incomingTxtMsg.text = txtMsg.text;

                        serverUserActor.tell(incomingTxtMsg, self());
                    } else {
                        tellClientSystem("User " +  txtMsg.sendTo + " does not exist");
                    }
                })
                .match(IncomingPrivateMessage.class, incTxtMsg -> {
                    tellClient("{" + incTxtMsg.sentFrom + "}: "+ incTxtMsg.text);
                })

                .match(JoinMessage.class, joinMsg -> {

                    // get child by channel name
                    ActorSelection sel = getContext().actorSelection(serverUserPath + userName + "/" + joinMsg.channelName);
                    ActorRef userChannel = HelperFunctions.getActorRefBySelection(sel);

                    // create the child if it doesn't exist
                    if (userChannel == null)  {
                        userChannel = getContext().actorOf(Props.create(ServerUserChannelActor.class, userName, joinMsg.channelName, clientUserActor), joinMsg.channelName);
                    }

                    // try joining the channel
                    userChannel.forward(joinMsg, getContext());
                })
                .match(LeaveChannelMessage.class, leaveChMsg -> {
                    ActorSelection sel = getContext().actorSelection(serverUserPath + userName + "/" + leaveChMsg.channelToLeave);
                    ActorRef userChannel = HelperFunctions.getActorRefBySelection(sel);

                    leaveChMsg.leavingUserName = userName;
                    userChannel.forward(leaveChMsg, getContext());

                    //userChannel.tell(akka.actor.PoisonPill.getInstance(), self());
                    //getContext().stop(userChannel);
                })
                .match(OutgoingBroadcastMessage.class, outBrdMsg -> {
                    ActorSelection sel = getContext().actorSelection(serverUserPath + userName + "/" + outBrdMsg.toChannel);
                    ActorRef userChannel = HelperFunctions.getActorRefBySelection(sel);

                    if (userChannel != null) {
                        outBrdMsg.sentFrom = userName;
                        userChannel.tell(outBrdMsg, self());
                    } else { // not in this channel
                        tellClientSystem("Not in channel, can't send.");
                    }
                })
                .match(ChangeTitleMessage.class, chTlMsg -> {
                    ActorSelection sel = getContext().actorSelection(serverUserPath + userName + "/" + chTlMsg.channelForTitleChange);
                    ActorRef userChannel = HelperFunctions.getActorRefBySelection(sel);

                    userChannel.forward(chTlMsg, getContext());
                })
                .match(GetChannelListMessage.class, getChLstMsg -> {
                    ActorSelection sel = getContext().getSystem().actorSelection(channelCreatorPath);
                    ActorRef channelCreator = HelperFunctions.getActorRefBySelection(sel);
                    // get list
                    channelCreator.forward(getChLstMsg, getContext());
                })
                .match(GetUserListInChannelMessage.class, getUlChMsg -> {
                    ActorSelection sel = getContext().actorSelection(channelCreatorPath + "/" + getUlChMsg.channelName);
                    ActorRef channel = HelperFunctions.getActorRefBySelection(sel);

                    channel.forward(getUlChMsg, getContext());
                })
                .match(ConnectMessage.class, connMsg -> {
                    clientUserActor = connMsg.clientUserActor;
                })
                .match(OutgoingPromoteDemoteMessage.class, prmDemUsrMsg -> {
                    ActorSelection sel = getContext().actorSelection(prmDemUsrMsg.channel);
                    ActorRef userChannel = HelperFunctions.getActorRefBySelection(sel);

                    if (userChannel != null) {
                        userChannel.forward(prmDemUsrMsg, getContext());
                    } else {
                        tellClientSystem("Such channel does not exist");
                    }
                })
                .match(GotKickedMessage.class, gotKckMsg -> {
                    sender().tell(akka.actor.PoisonPill.getInstance(), self());
                })
                .match(KillChannelMessage.class, klChMsg -> {
                    ActorSelection sel = getContext().actorSelection(klChMsg.channelName);
                    ActorRef userChannel = HelperFunctions.getActorRefBySelection(sel);

                    klChMsg.killer = userName;

                    if (userChannel != null) {
                        userChannel.forward(klChMsg, getContext());
                    } else {
                        tellClientSystem("Cannot disband channel that does not exist");
                    }
                })
                .build();
    }

    private void tellClient(String message) {
        clientUserActor.tell(message, self());
    }

    private void tellClientSystem(String message) {
        tellClient("SYSTEM: " + message);
    }

}
