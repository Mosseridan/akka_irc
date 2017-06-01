import Shared.Messages.*;
import akka.actor.*;

public class ServerUserActor extends AbstractActor {



    String userName;
    ActorRef clientUserActor;
    final String serverUserPath = "/user/Server/ServerUser";
    final String channelCreatorPath = "/user/Server/ChannelCreator";

    public ServerUserActor(String username, ActorRef clientUserActor) {
        this.userName = username;
        this.clientUserActor = clientUserActor;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
        .match(OutgoingPrivateMessage.class, msg -> { // send a message to another actor
            ActorSelection serverActorSel = getContext().actorSelection(serverUserPath + msg.userName);
            ActorRef serverUserActor = HelperFunctions.getActorRefBySelection(serverActorSel);

            if (serverUserActor != null) {
                serverUserActor.tell(new IncomingPrivateMessage(msg.sender,msg.message), self());
            } else {
                tellClientSystem("Did not send \"" + msg.message + "\". User \"" +  msg.userName + "\" does not exist.");
            }
        })
        .match(IncomingPrivateMessage.class, msg -> {
            tellClient("<" + msg.sender + "> " + msg.message);
        })
        .match(JoinMessage.class, msg -> {
            // get child by channel name
            ActorSelection sel = getContext().actorSelection(msg.channel);
            ActorRef userChannel = HelperFunctions.getActorRefBySelection(sel);
            // create the child if it doesn't exist
            if (userChannel == null)  {
                userChannel = getContext().actorOf(Props.create(ServerUserChannelActor.class, msg.userName, clientUserActor, msg.channel), msg.channel);
            }
            // try joining the channel
            userChannel.forward(msg, getContext());
        })
        .match(LeaveMessage.class, msg -> {
            ActorSelection sel = getContext().actorSelection(serverUserPath + userName + "/" + msg.channel);
            ActorRef userChannel = HelperFunctions.getActorRefBySelection(sel);

            if(userChannel != null) {
                userChannel.forward(msg, getContext());
            } else {
                tellClientSystem("Did not leave channel \"" + msg.channel + "\". You are not in this channel");
            }
        })
        .match(OutgoingBroadcastMessage.class, msg -> {
            ActorSelection sel = getContext().actorSelection(serverUserPath + userName + "/" + msg.channel);
            ActorRef userChannel = HelperFunctions.getActorRefBySelection(sel);
            if (userChannel != null) {
                userChannel.forward(msg, getContext());
            } else { // not in this channel
                tellClientSystem("Did not send \"" + msg.message + "\" to channel \"" +  msg.channel + "\".You are not in this channel.");
            }
        })
        .match(ChangeTitleMessage.class, msg -> {
            ActorSelection sel = getContext().actorSelection(serverUserPath + userName + "/" + msg.channel);
            ActorRef userChannel = HelperFunctions.getActorRefBySelection(sel);
            if (userChannel != null) {
                userChannel.forward(msg, getContext());
            } else { // not in this channel
                tellClientSystem("Did not change title to \"" + msg.newTitle + "\" in channel \"" + msg.channel + "\".You are not in this channel.");
            }
        })
        .match(OutgoingKickMessage.class, msg -> {
            ActorSelection sel = getContext().actorSelection(msg.channel);
            ActorRef userChannel = HelperFunctions.getActorRefBySelection(sel);
            if (userChannel != null) { // user exists
                userChannel.forward(msg, getContext());
            } else { // cannot kick if not in channel
                tellClientSystem("Did not kick user \"" + msg.userName + "\" from channel \"" + msg.channel + "\".You are not in this channel.");
            }
        })
        .match(OutgoingBanMessage.class, msg -> {
            ActorSelection sel = getContext().actorSelection(msg.channel);
            ActorRef userChannel = HelperFunctions.getActorRefBySelection(sel);
            if (userChannel != null) { // user exists
                userChannel.forward(msg, getContext());
            } else { // cannot kick if not in channel
                tellClientSystem("Did not Ban user \"" + msg.userName + "\" from channel \"" + msg.channel + "\".You are not in this channel.");
            }
        })
        .match(OutgoingAddVoicedMessage.class, msg -> {
            ActorSelection sel = getContext().actorSelection(msg.channel);
            ActorRef userChannel = HelperFunctions.getActorRefBySelection(sel);
            if (userChannel != null) { // user exists
                userChannel.forward(msg, getContext());
            } else { // cannot kick if not in channel
                tellClientSystem("Did not make user \"" + msg.userName + "\" voiced, in channel \"" + msg.channel + "\".You are not in this channel.");
            }
        })
        .match(OutgoingAddOperatorMessage.class, msg -> {
            ActorSelection sel = getContext().actorSelection(msg.channel);
            ActorRef userChannel = HelperFunctions.getActorRefBySelection(sel);
            if (userChannel != null) { // user exists
                userChannel.forward(msg, getContext());
            } else { // cannot kick if not in channel
                tellClientSystem("Did not make user \"" + msg.userName + "\" operator, in channel \"" + msg.channel + "\".You are not in this channel.");
            }
        })
        .match(OutgoingRemoveVoicedMessage.class, msg -> {
            ActorSelection sel = getContext().actorSelection(msg.channel);
            ActorRef userChannel = HelperFunctions.getActorRefBySelection(sel);
            if (userChannel != null) { // user exists
                userChannel.forward(msg, getContext());
            } else { // cannot kick if not in channel
                tellClientSystem("Did not remove voiced rights from user \"" + msg.userName + "\" in channel \"" + msg.channel + "\".You are not in this channel.");
            }
        })
        .match(OutgoingRemoveOperatorMessage.class, msg -> {
            ActorSelection sel = getContext().actorSelection(msg.channel);
            ActorRef userChannel = HelperFunctions.getActorRefBySelection(sel);
            if (userChannel != null) { // user exists
                userChannel.forward(msg, getContext());
            } else {
                tellClientSystem("Did not remove operator rights from user \"" + msg.userName + "\" in channel \"" + msg.channel + "\".You are not in this channel.");
            }
        })
        .match(GetContentMessage.class,msg -> {
            ActorSelection sel = getContext().actorSelection(msg.channel);
            ActorRef userChannel = HelperFunctions.getActorRefBySelection(sel);
            if (userChannel != null) { // user exists
                userChannel.forward(msg, getContext());
            } else {
                tellClientSystem("Could not get content for channel \"" + msg.channel + "\". You are not in this channel.");
            }
        })
//        .match(OutgoingPromoteDemoteMessage.class, prmDemUsrMsg -> {
//            ActorSelection sel = getContext().actorSelection(prmDemUsrMsg.channel);
//            ActorRef userChannel = HelperFunctions.getActorRefBySelection(sel);
//
//            userChannel.forward(prmDemUsrMsg, getContext());
//        })
//        .match(IncomingKickMessage.class, msg -> {
//            sender().tell(akka.actor.PoisonPill.getInstance(), self());
//        })
//        .match(SetChannelListMessage.class, setChLstMsg -> {
//            clientUserActor.tell(setChLstMsg, self());
//        })
        .build();
    }

    private void tellClient(String message) {
        clientUserActor.tell(message, self());
    }

    private void tellClientSystem(String message) {
        tellClient("SYSTEM: " + message);
    }

}
