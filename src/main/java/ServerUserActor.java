
import Shared.Messages.*;
import akka.actor.*;

import java.util.Optional;

public class ServerUserActor extends AbstractActor {

    final String serverUserPath = "/user/Server/ServerUser";

    String userName;
    ActorRef clientUserActor;
    ActorRef channelCreator;


    public ServerUserActor(String username, ActorRef clientUserActor, ActorRef channelCreator) {
        this.userName = username;
        this.clientUserActor = clientUserActor;
        this.channelCreator = channelCreator;
    }

    @Override
    public void preStart() {
        getContext().watch(clientUserActor);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            /** OUTGOING MESSAGES **/
            // ConnectMessage
            .match(OutgoingPrivateMessage.class, this::receiveOutgoingPrivate)
            //OutgoingBroadcastMessage
            .match(OutgoingBroadcastMessage.class, this::receiveOutgoingBroadcast)
            //JoinMessage
            .match(JoinMessage.class, this::receiveJoin)
            // LeaveMessage
            .match(LeaveMessage.class, this::receiveLeave)
            // OutgoingKillChannelMessage
            .match(OutgoingKillChannelMessage.class, this::receiveKillChannel)
            // OutgoingKickMessage
            .match(OutgoingKickMessage.class, this::receiveOutgoingKick)
            // OutgoingBanMessage
            .match(OutgoingBanMessage.class, this::receiveOutgoingBan)
            // OutgoingAddVoicedMessage
            .match(OutgoingAddVoicedMessage.class, this::receiveOutgoingAddVoiced)
            // OutgoingAddOperatorMessage
            .match(OutgoingAddOperatorMessage.class, this::receiveOutgoingAddOperator)
            // OutgoingRemoveVoicedMessage
            .match(OutgoingRemoveVoicedMessage.class, this::receiveOutgoingRemoveVoiced)
            // OutgoingRemoveOperatorMessage
            .match(OutgoingRemoveOperatorMessage.class, this::receiveOutgoingRemoveOperator)
            // ChangeTitleMessage
            .match(ChangeTitleMessage.class, this::receiveChangeTitle)
            // GetContentMessage
            .match(GetContentMessage.class, this::receiveGetAllUserNames)
            /** INCOMING MESSAGES **/
            .match(IncomingPrivateMessage.class, this::receiveIncomingPrivate)
            // ErrorMessage
            .match(ErrorMessage.class, this::receiveError)
            // AnnouncementMessage
            .match(AnnouncementMessage.class, this::receiveAnnouncement)
            // Terminated message
            .match(Terminated.class, this::receiveTerminated)
            // For any unhandled message
            .matchAny(this::receiveUnhandled)
            .build();
    }



/** OUTGOING MESSAGES **/

    // OutgoingPrivateMessage
    private void receiveOutgoingPrivate(OutgoingPrivateMessage msg) {
        ActorRef serverUserActor = getServerUserActorRef(msg.getUserName());
        if (serverUserActor != null) {
            serverUserActor.tell(new IncomingPrivateMessage(msg.getUserName(),msg.getSenderName(),msg.getMessage()), getSelf());
        } else {
            getSender().tell(new ErrorMessage(
                    "send Private Message \""+msg.getMessage()+"\" to user \""+msg.getUserName()+"\"",
                    "User \"" +msg.getUserName()+"\" does not exist"), getSelf());
        }
    }

    // OutgoingBroadcastMessage
    private void receiveOutgoingBroadcast(OutgoingBroadcastMessage msg) {
        Optional<ActorRef> ServerUserChannelActor = getServerUserChannelActorRef(msg.getChannelName());
        if (ServerUserChannelActor.isPresent()) {
            ServerUserChannelActor.get().forward(msg, getContext());
        } else { // not in this channel
            getSender().tell(new ErrorMessage(
                    "send Broadcast Message \""+msg.getMessage()+"\" to channel \""+msg.getChannelName()+"\"",
                    "You are not in this channel."), getSelf());
        }
    }

    // JoinMessage
    private void receiveJoin(JoinMessage msg) {
        // create a ServerUserChannelActor for the requested channel if it dose not already exist.
        if (getServerUserChannelActorRef(msg.getChannelName()).isPresent()) {
            getSender().tell(new ErrorMessage(
                            "join channel",
                            "You are already in this channel"), getSelf());
        } else {
            getContext().actorOf(
                Props.create(ServerUserChannelActor.class, // actor type
                        msg.getUserName(),      // userName
                        clientUserActor,        // clientUserActor
                        getSelf(),              // serverUserActor
                        msg.getChannelName()),  // channelName
                msg.getChannelName()); // actor name

        }
    }

    // LeaveMessage
    private void receiveLeave(LeaveMessage msg){
        Optional<ActorRef> serverUserChannelActor = getServerUserChannelActorRef(msg.getChannelName());
        if(serverUserChannelActor.isPresent()) {
//            ActorRef actor = getActorRef(serverUserPath+msg.getUserName()+"/"+msg.getChannelName());
//            ActorRef actor2 = serverUserChannelActor.get();
//            actor.forward(msg, getContext());
//            actor2.tell(new LeaveMessage(userName,msg.getChannelName()),getSelf());
            serverUserChannelActor.get().forward(msg, getContext());
        } else {
            getSender().tell(new ErrorMessage(
                    "leave channel \""+msg.getChannelName()+"\"",
                    "You are not in this channel"), getSelf());
        }
    }

    // OutgoingKillChannelMessage
    private void receiveKillChannel(OutgoingKillChannelMessage msg) {
        Optional<ActorRef> ServerUserChannelActor = getServerUserChannelActorRef(msg.getChannelName());
        if(ServerUserChannelActor.isPresent()) {
            ServerUserChannelActor.get().forward(msg, getContext());
        } else {
            getSender().tell(new ErrorMessage(
                    "kill channel \""+msg.getChannelName()+"\"",
                    "You are not in this channel"), getSelf());
        }
    }

    // OutgoingKickMessage
    private void receiveOutgoingKick(OutgoingKickMessage msg) {
        Optional<ActorRef> ServerUserChannelActor = getServerUserChannelActorRef(msg.getChannelName());
        if(ServerUserChannelActor.isPresent()) {
            ServerUserChannelActor.get().forward(msg, getContext());
        } else {
            getSender().tell(new ErrorMessage(
                    "kick user "+msg.getUserName()+"\" from channel \""+msg.getChannelName()+"\"",
                    "You are not in this channel"), getSelf());
        }
    }

    // OutgoingBanMessage
    private void receiveOutgoingBan(OutgoingBanMessage msg) {
        Optional<ActorRef> ServerUserChannelActor = getServerUserChannelActorRef(msg.getChannelName());
        if(ServerUserChannelActor.isPresent()) {
            ServerUserChannelActor.get().forward(msg, getContext());
        } else {
            getSender().tell(new ErrorMessage(
                    "ban user " + msg.getUserName()+"\" from channel \""+msg.getChannelName()+"\"",
                    "You are not in this channel"), getSelf());
        }
    }

    // OutgoingAddVoicedMessage
    private void receiveOutgoingAddVoiced(OutgoingAddVoicedMessage msg) {
        Optional<ActorRef> ServerUserChannelActor = getServerUserChannelActorRef(msg.getChannelName());
        if(ServerUserChannelActor.isPresent()) {
            ServerUserChannelActor.get().forward(msg, getContext());
        } else {
            getSender().tell(new ErrorMessage(
                    "make user "+msg.getUserName()+"\" voiced in channel \""+msg.getChannelName()+"\"",
                    "You are not in this channel"), getSelf());
        }
    }

    // OutgoingAddOperatorMessage
    private void receiveOutgoingAddOperator(OutgoingAddOperatorMessage msg) {
        Optional<ActorRef> ServerUserChannelActor = getServerUserChannelActorRef(msg.getChannelName());
        if(ServerUserChannelActor.isPresent()) {
            ServerUserChannelActor.get().forward(msg, getContext());
        } else {
            getSender().tell(new ErrorMessage(
                    "make user "+msg.getUserName()+"\" an operator in channel \""+msg.getChannelName()+"\"",
                    "You are not in this channel"), getSelf());
        }
    }

    // OutgoingRemoveVoicedMessage
    private void receiveOutgoingRemoveVoiced(OutgoingRemoveVoicedMessage msg) {
        Optional<ActorRef> ServerUserChannelActor = getServerUserChannelActorRef(msg.getChannelName());
        if(ServerUserChannelActor.isPresent()) {
            ServerUserChannelActor.get().forward(msg, getContext());
        } else {
            getSender().tell(new ErrorMessage(
                    "remove voiced rights from user "+msg.getUserName()+"\" in channel \""+msg.getChannelName()+"\"",
                    "You are not in this channel"), getSelf());
        }
    }

    // OutgoingRemoveOperatorMessage
    private void receiveOutgoingRemoveOperator(OutgoingRemoveOperatorMessage msg) {
        Optional<ActorRef> ServerUserChannelActor = getServerUserChannelActorRef(msg.getChannelName());
        if(ServerUserChannelActor.isPresent()) {
            ServerUserChannelActor.get().forward(msg, getContext());
        } else {
            getSender().tell(new ErrorMessage(
                    "remove operator rights from user "+msg.getUserName()+"\" in channel \""+msg.getChannelName()+"\"",
                    "You are not in this channel"), getSelf());
        }
    }

    // ChangeTitleMessage
    private void receiveChangeTitle(ChangeTitleMessage msg) {
        Optional<ActorRef> ServerUserChannelActor = getServerUserChannelActorRef(msg.getChannelName());
        if(ServerUserChannelActor.isPresent()) {
            ServerUserChannelActor.get().forward(msg, getContext());
        } else {
            getSender().tell(new ErrorMessage(
                    "change title for channel \""+msg.getChannelName()+"\" to \""+msg.getTitle()+"\"",
                    "You are not in this channel"), getSelf());
        }
    }

    // GetContentMessage
    private void  receiveGetAllUserNames(GetContentMessage msg){
        Optional<ActorRef> ServerUserChannelActor = getServerUserChannelActorRef(msg.getChannelName());
        if(ServerUserChannelActor.isPresent()) {
            ServerUserChannelActor.get().forward(msg, getContext());
        } else {
            getSender().tell(new ErrorMessage(
                    "get all user names for channel \""+msg.getChannelName()+"\"",
                    "You are not in this channel"), getSelf());
        }
    }

    /** INCOMING MESSAGES **/
    // IncomingPrivateMessage
    private void receiveIncomingPrivate(IncomingPrivateMessage msg) {
        clientUserActor.forward(msg,getContext());
    }

    // ErrorMessage
    private void receiveError(ErrorMessage msg) {
        clientUserActor.forward(msg,getContext());
    }

    // AnnouncementMessage
    private void receiveAnnouncement(AnnouncementMessage msg) {
        clientUserActor.forward(msg,getContext());
    }

    // Terminated Message
    private void receiveTerminated (Terminated msg){
        System.out.println("$$$ in serverUserActor userName: "+userName+" received Terminated: "+msg.toString());
        getContext().stop(getSelf());
    }

    // For any unhandled message
    private void receiveUnhandled(Object o) {
        getSender().tell(new ErrorMessage(
                "send "+o.toString(),
                "This message is invalid in the current getContext"), getSelf());
    }


    // returns an ActorRef for the actor with the given path
    private ActorRef getActorRef(String path){
        ActorSelection sel = getContext().actorSelection(path);
        return(HelperFunctions.getActorRefBySelection(sel));
    }

    // returns an ActorRef for the ServerUserActor with the given name
    private ActorRef getServerUserActorRef(String userName){
        return(getActorRef(serverUserPath + userName));
    }

    //
    private Optional<ActorRef> getServerUserChannelActorRef(String channelName){
        return(getContext().findChild(channelName));
    }

}
