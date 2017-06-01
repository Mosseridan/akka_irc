import Shared.Messages.*;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;

public class ServerUserChannelActor extends AbstractActor {

    String userName;
    ActorRef clientUserActor;
    ActorRef serverUserActor;
    String channelName;
    ActorRef channelRef;
    UserMode userMode;
//    UserMode previousUserMode;

    final String serverUserPath = "/user/Server/ServerUser";
    final String channelCreatorPath = "/user/Server/ChannelCreator/";


    public ServerUserChannelActor(String userName, ActorRef clientUserActor, String channelName) {
        this.userName = userName;
        this.clientUserActor = clientUserActor;
        this.serverUserActor = null;
        this.channelName = channelName;
        this.channelRef = null;
        userMode = UserMode.OUT;
//        previousUserMode = UserMode.OUT;
    }

    @Override
    public void preStart() {
        serverUserActor = getContext().parent();
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
        .match(JoinMessage.class, msg -> {
            if (channelRef == null) {
                if (userMode == UserMode.BANNED) { // Create / Join the channel
                    tellClientSystem("Did not join channel \"" + msg.channel + "\". You are banned from this channel.");
                } else {
                    ActorSelection channelSel = getContext().actorSelection(channelCreatorPath + "/" + msg.channel);
                    ActorRef channelToJoin = HelperFunctions.getActorRefBySelection(channelSel);
                    if (channelToJoin == null) { // channel does not exist
                        // get the ChannelCreator actor
                        ActorSelection channelCreatorSel = getContext().actorSelection(channelCreatorPath);
                        ActorRef channelCreator = HelperFunctions.getActorRefBySelection(channelCreatorSel);
                        // ask ChannelCreator to join it
                        channelCreator.tell(msg, self());
                    } else { // channel already exist, ask it directly
                        channelToJoin.tell(msg, self());
                    }
                }
            } else if(userMode == UserMode.OUT){
                channelRef.tell(msg, self());
            } else {
                tellClientSystem("Did not join channel \"" + msg.channel + "\". You are already in this channel.");
            }
        })
        .match(JoinApprovalMessage.class, msg -> {
            channelRef = msg.channelRef;
            userMode = msg.mode;
            userName =msg.userName;
            clientUserActor.tell(msg, self());
        })
        .match(UserJoinedMessage.class, msg->{
            clientUserActor.tell(msg,self());
        })
        .match(LeaveMessage.class, msg -> {
            //ActorSelection sel = getContext().actorSelection(channelCreatorPath + "/" + channelName);
            //ActorRef channelToLeave = HelperFunctions.getActorRefBySelection(sel);
            msg.userMode = userMode;
            if (channelRef != null) {
                channelRef.tell(msg, self());
                userMode = UserMode.OUT;
                msg.userName = userName;
                clientUserActor.tell(msg,self());
            } else {
                tellClientSystem("Did not leave channel \"" + msg.channel + "\". You are not in this channel.");
            }
        })
        .match(UserLeftMessage.class, msg -> {
            clientUserActor.forward(msg,getContext());
        })
        .match(OutgoingBroadcastMessage.class, msg -> {
            if (channelRef != null && userMode != UserMode.OUT) {
                if (userMode == UserMode.USER || userMode == UserMode.VOICE || userMode == UserMode.OPERATOR || userMode == UserMode.OWNER) {
                    //ActorSelection sel = getContext().actorSelection(channelCreatorPath + "/" + channelName);
                    //ActorRef channelToBroadcast = HelperFunctions.getActorRefBySelection(sel);
                    msg.sender = userName; // add UserMode prefix
                    channelRef.tell(msg, self());
                } else if (userMode == UserMode.BANNED) {
                    tellClientSystem("Did not send \"" + msg.message + "\" to channel \"" +  msg.channel + "\". You are banned from this channel.");
                } else {
                    tellClientSystem("Something went wrong!");
                }
            } else {
                tellClientSystem("Did not send \"" + msg.message + "\" to channel \"" +  msg.channel + "\". You are not in this channel.");
            }
        })
        .match(IncomingBroadcastMessage.class, msg -> { // Broadcast from the channel
            tellClient(msg.message);
        })
        .match(ChangeTitleMessage.class, msg -> {
            if (channelRef != null && userMode != UserMode.OUT) {
                if (userMode == UserMode.VOICE || userMode == UserMode.OPERATOR || userMode == UserMode.OWNER) {
                    channelRef.tell(msg, self());
                } else if (userMode == UserMode.USER) {
                    tellClientSystem("Did not change title to \"" + msg.newTitle + "\" in channel \"" + msg.channel + "\". You do not have sufficient rights.");
                } else if (userMode == UserMode.BANNED) {
                    tellClientSystem("Did not change title to \"" + msg.newTitle + "\" in channel \"" + msg.channel + "\". You are banned from this channel.");
                } else {
                    tellClientSystem("Something went wrong!");
                }
            } else {
                tellClientSystem("Did not change title to \"" + msg.newTitle + "\" in channel \"" + msg.channel + "\". You are not in this channel.");
            }
        })
         //Outgoing Messages
        .match(OutgoingKickMessage.class, msg -> {
            if (channelRef != null && userMode != UserMode.OUT) {
                if (userMode == UserMode.OPERATOR || userMode == UserMode.OWNER) {
                    ActorSelection sel = getContext().actorSelection(serverUserPath + msg.userName + "/" + channelName);
                    ActorRef userChannel = HelperFunctions.getActorRefBySelection(sel);
                    if (userChannel != null) { // user exists
                        userChannel.forward(new IncomingKickMessage(msg.userName, userName, channelName), getContext());
                    } else { // cannot kick if not in channel
                        tellClientSystem("Did not kick user \"" + msg.userName + "\" from channel \"" + msg.channel + "\".He is not in this channel.");
                    }
                } else if (userMode == UserMode.USER || userMode == UserMode.VOICE) {
                    tellClientSystem("Did not kick user \"" + msg.userName + "\" from channel \"" + msg.channel + "\". You do not have sufficient rights.");
                } else if (userMode == UserMode.BANNED) {
                    tellClientSystem("Did not kick user \"" + msg.userName + "\" from channel \"" + msg.channel + "\". You are banned from this channel.");
                } else{
                    tellClientSystem("Something went wrong!");
                }
            } else{
                tellClientSystem("Did not kick user \"" + msg.userName + "\" from channel \"" + msg.channel + "\". You are not in this channel.");
            }
        })
        .match(OutgoingBanMessage.class, msg -> {
            if (channelRef != null && userMode != UserMode.OUT) {
                if (userMode == UserMode.OPERATOR || userMode == UserMode.OWNER) {
                    ActorSelection sel = getContext().actorSelection(serverUserPath + msg.userName + "/" + channelName);
                    ActorRef userChannel = HelperFunctions.getActorRefBySelection(sel);
                    if (userChannel != null) { // user exists
                        userChannel.forward(new IncomingBanMessage(msg.userName, userName, channelName), getContext());
                    } else { // cannot ban if not in channel
                        tellClientSystem("Did not ban user \"" + msg.userName + "\" from channel \"" + msg.channel + "\".He is not in this channel.");
                    }
                } else if (userMode == UserMode.USER || userMode == UserMode.VOICE) {
                    tellClientSystem("Did not ban user \"" + msg.userName + "\" from channel \"" + msg.channel + "\". You do not have sufficient rights.");
                } else if (userMode == UserMode.BANNED) {
                    tellClientSystem("Did not ban user \"" + msg.userName + "\" from channel \"" + msg.channel + "\". You are banned from this channel.");
                } else {
                    tellClientSystem("Something went wrong!");
                }
            } else{
                tellClientSystem("Did not ban user \"" + msg.userName + "\" from channel \"" + msg.channel + "\". You are not in this channel.");
            }
        })
        .match(OutgoingAddVoicedMessage.class, msg -> {
            if (channelRef != null && userMode != UserMode.OUT) {
                if (userMode == UserMode.OPERATOR || userMode == UserMode.OWNER) {
                    ActorSelection sel = getContext().actorSelection(serverUserPath + msg.userName + "/" + channelName);
                    ActorRef userChannel = HelperFunctions.getActorRefBySelection(sel);
                    if (userChannel != null) { // user exists
                        userChannel.forward(new IncomingAddOperatorMessage(msg.userName, userName, channelName), getContext());
                    } else { // cannot ban if not in channel
                        tellClientSystem("Did not make user \"" + msg.userName + "\" voiced, in channel \"" + msg.channel + "\".He is not in this channel.");
                    }
                } else if (userMode == UserMode.USER || userMode == UserMode.VOICE) {
                    tellClientSystem("Did not make user \"" + msg.userName + "\" voiced, in channel \"" + msg.channel + "\". You do not have sufficient rights.");
                } else if (userMode == UserMode.BANNED) {
                    tellClientSystem("Did not make user \"" + msg.userName + "\" voiced, in channel \"" + msg.channel +  "\". You are banned from this channel.");
                } else {
                    tellClientSystem("Something went wrong!");
                }
            } else{
                tellClientSystem("Did not make user \"" + msg.userName + "\" voiced, in channel \"" + msg.channel + "\". You are not in this channel.");
            }
        })
        .match(OutgoingRemoveVoicedMessage.class, msg -> {
            if (channelRef != null && userMode != UserMode.OUT) {
                if (userMode == UserMode.OPERATOR || userMode == UserMode.OWNER) {
                    ActorSelection sel = getContext().actorSelection(serverUserPath + msg.userName + "/" + channelName);
                    ActorRef userChannel = HelperFunctions.getActorRefBySelection(sel);
                    if (userChannel != null) { // user exists
                        userChannel.forward(new IncomingRemoveVoicedMessage(msg.userName, userName, channelName), getContext());
                    } else { // cannot ban if not in channel
                        tellClientSystem("Did not remove voiced rights from user \"" + msg.userName + "\" in channel \"" + msg.channel + "\".He is not in this channel.");
                    }
                } else if (userMode == UserMode.USER || userMode == UserMode.VOICE) {
                    tellClientSystem("Did not remove voiced rights from user \"" + msg.userName + "\" in channel \"" + msg.channel + "\". You do not have sufficient rights.");
                } else if (userMode == UserMode.BANNED) {
                    tellClientSystem("Did not remove voiced rights from user \"" + msg.userName + "\" in channel \"" + msg.channel + "\". You are banned from this channel.");
                } else {
                    tellClientSystem("Something went wrong!");
                }
            } else{
                tellClientSystem("Did not remove voiced rights from user \"" + msg.userName + "\" in channel \"" + msg.channel + "\". You are not in this channel.");
            }
        })
        .match(OutgoingAddOperatorMessage.class, msg -> {
            if (channelRef != null && userMode != UserMode.OUT) {
                if (userMode == UserMode.OPERATOR || userMode == UserMode.OWNER) {
                    ActorSelection sel = getContext().actorSelection(serverUserPath + msg.userName + "/" + channelName);
                    ActorRef userChannel = HelperFunctions.getActorRefBySelection(sel);
                    if (userChannel != null) { // user exists
                        userChannel.forward(new IncomingAddOperatorMessage(msg.userName, userName, channelName), getContext());
                    } else { // cannot ban if not in channel
                        tellClientSystem("Did not make user \"" + msg.userName + "\" operator, in channel \"" + msg.channel + "\".He is not in this channel.");
                    }
                } else if (userMode == UserMode.USER || userMode == UserMode.VOICE) {
                    tellClientSystem("Did not make user \"" + msg.userName + "\" operator, in channel \"" + msg.channel + "\". You do not have sufficient rights.");
                } else if (userMode == UserMode.BANNED) {
                    tellClientSystem("Did not make user \"" + msg.userName + "\" operator, in channel \"" + msg.channel +  "\". You are banned from this channel.");
                } else {
                    tellClientSystem("Something went wrong!");
                }
            } else{
                tellClientSystem("Did not make user \"" + msg.userName + "\" operator, in channel \"" + msg.channel +  "\". You are not in this channel.");
            }
        })
        .match(OutgoingRemoveOperatorMessage.class, msg -> {
            if (channelRef != null && userMode != UserMode.OUT) {
                if (userMode == UserMode.OPERATOR || userMode == UserMode.OWNER) {
                    ActorSelection sel = getContext().actorSelection(serverUserPath + msg.userName + "/" + channelName);
                    ActorRef userChannel = HelperFunctions.getActorRefBySelection(sel);
                    if (userChannel != null) { // user exists
                        userChannel.forward(new IncomingRemoveOperatorMessage(msg.userName, userName, channelName), getContext());
                    } else { // cannot ban if not in channel
                        tellClientSystem("Didnot  remove operator rights from user \"" + msg.userName + "\" in channel \"" + msg.channel + "\".He is not in this channel.");
                    }
                } else if (userMode == UserMode.USER || userMode == UserMode.VOICE) {
                    tellClientSystem("Did not remove operator rights from user \"" + msg.userName + "\" in channel \"" + msg.channel + "\". You do not have sufficient rights.");
                } else if (userMode == UserMode.BANNED) {
                    tellClientSystem("Did not remove operator rights from user \"" + msg.userName + "\" in channel \"" + msg.channel + "\". You are banned from this channel.");
                } else {
                    tellClientSystem("Something went wrong!");
                }
            } else{
                tellClientSystem("Did not remove operator rights from user \"" + msg.userName + "\" in channel \"" + msg.channel + "\". You are not in this channel.");
            }
        })

        //Incoming Messages
        .match(IncomingKickMessage.class, msg -> {
          if (channelRef != null && userMode != UserMode.OUT) {
              if (userMode == UserMode.USER || userMode == UserMode.VOICE || userMode == UserMode.OPERATOR) {
                  String oldUserName = userName;
                  userName = msg.userName; //reset UserMode prefix in userName
                  msg.userName = oldUserName; // add UserMode prefix to userName
                  userMode = UserMode.OUT;
                  clientUserActor.tell(msg,self());
                  channelRef.tell(msg, self());
              } else if (userMode == UserMode.BANNED) {
                  sender().forward("Did not kick user \"" + msg.userName + "\" from channel \"" + msg.channel + "\". He is already banned from this channel.",getContext());
              }else if (userMode == userMode.OWNER){
                  sender().forward("Did not kick user \"" + msg.userName + "\" from channel \"" + msg.channel + "\". Cannot kick channel owner.",getContext());
              }else{
                  sender().forward("Something went wrong!",getContext());
              }
          }else{
              sender().forward("Did not kick user \"" + userName + "\" from channel \"" + msg.channel + "\". He is not in this channel.",getContext());
          }
        })
        .match(IncomingBanMessage.class, msg -> {
            if (channelRef != null && userMode != UserMode.OUT) {
                if (userMode == UserMode.USER || userMode == UserMode.VOICE || userMode == UserMode.OPERATOR) {
                    String oldUserName = userName;
                    userName = msg.userName; //reset UserMode prefix in userName
                    msg.userName = oldUserName; // add UserMode prefix to userName
                    userMode = UserMode.BANNED;
                    clientUserActor.tell(msg,self());
                    channelRef.tell(msg, self());
                } else if (userMode == UserMode.BANNED) {
                    sender().forward("Did not ban user \"" + msg.userName + "\" from channel \"" + msg.channel + "\". He is already banned from this channel..",getContext());
                }else if (userMode == userMode.OWNER){
                    sender().forward("Did not ban user \"" + msg.userName + "\" from channel \"" + msg.channel + "\". Cannot kick channel owner.",getContext());
                }else{
                    sender().forward("Something went wrong!",getContext());
                }
            }else{
                sender().forward("Did not kick user \"" + userName + "\" from channel \"" + msg.channel + "\". He is not in this channel.",getContext());
            }
        })
        .match(IncomingAddVoicedMessage.class, msg -> {
            if (channelRef != null && userMode != UserMode.OUT) {
                if (userMode == UserMode.USER){
                    msg.oldUserName = userName;
                    userName = msg.newUserName;
                    userMode = UserMode.VOICE;
                    clientUserActor.tell(msg,self());
                    channelRef.forward(msg, getContext());
                } else if (userMode == UserMode.VOICE || userMode == UserMode.OPERATOR || userMode == userMode.OWNER) {
                    sender().forward("Did not make user \"" + userName + "\" voiced, in channel \"" + msg.channel + "\". He is has this right.",getContext());
                } else if (userMode == UserMode.BANNED) {
                    sender().forward("Did not make user \"" + userName + "\" voiced, in channel \"" + msg.channel + "\". He is already banned from this channel.",getContext());
                }else{
                    sender().forward("Something went wrong!",getContext());
                }
            }else{
                sender().forward("Did not make user \"" + userName + "\" voiced, in channel \"" + msg.channel +  "\". He is not in this channel.",getContext());
            }
        })
        .match(IncomingRemoveVoicedMessage.class, msg -> {
            if (channelRef != null && userMode != UserMode.OUT) {
                if (userMode == UserMode.VOICE){
                    userName = msg.newUserName;
                    userMode = UserMode.USER;
                    clientUserActor.tell(msg,self());
                    channelRef.forward(msg, getContext());
                } else if (userMode == UserMode.USER || userMode == UserMode.OPERATOR || userMode == UserMode.OWNER) {
                    sender().forward("Did not remove voiced rights from user \"" + userName + "\" in channel \"" + msg.channel + "\". He is not voiced.",getContext());
                } else if (userMode == UserMode.BANNED) {
                    sender().forward("Did not remove voiced rights from user \"" + userName + "\" in channel \"" + msg.channel + "\". He is already banned from this channel.",getContext());
                } else{
                    sender().forward("Something went wrong!",getContext());
                }
            } else{
                sender().forward("Did not remove voiced rights from user \"" + userName + "\" in channel \"" + msg.channel + "\". He is not in this channel.",getContext());
            }
        })
        .match(IncomingAddOperatorMessage.class, msg -> {
            if (channelRef != null && userMode != UserMode.OUT) {
                if (userMode == UserMode.USER || userMode == UserMode.VOICE){
                    msg.oldUserName = userName;
                    userName = msg.newUserName;
                    userMode = UserMode.OPERATOR;
                    clientUserActor.tell(msg,self());
                    channelRef.forward(msg, getContext());
                } else if (userMode == UserMode.OPERATOR || userMode == userMode.OWNER) {
                    sender().forward("Did not make user \"" + userName + "\" operator, in channel \"" + msg.channel + "\". He is has this right.",getContext());
                } else if (userMode == UserMode.BANNED) {
                    sender().forward("Did not make user \"" + userName + "\" operator, in channel \"" + msg.channel + "\". He is already banned from this channel.",getContext());
                }else{
                    sender().forward("Something went wrong!",getContext());
                }
            }else{
                sender().forward("Did not make user \"" + userName + "\" operator, in channel \"" + msg.channel +  "\". He is not in this channel.",getContext());
            }
        })
        .match(IncomingRemoveOperatorMessage.class, msg -> {
            if (channelRef != null && userMode != UserMode.OUT) {
                if (userMode == UserMode.OPERATOR){
                    userName = msg.newUserName;
                    userMode = UserMode.USER;
                    clientUserActor.tell(msg,self());
                    channelRef.forward(msg, getContext());
                } else if (userMode == UserMode.USER || userMode == UserMode.VOICE || userMode == UserMode.OWNER) {
                    sender().forward("Did not remove operator rights from user \"" + userName + "\" in channel \"" + msg.channel + "\". He is not an operator.",getContext());
                } else if (userMode == UserMode.BANNED) {
                    sender().forward("Did not remove operator rights from user \"" + userName + "\" in channel \"" + msg.channel + "\". He is already banned from this channel.",getContext());
                } else{
                    sender().forward("Something went wrong!",getContext());
                }
            } else{
                sender().forward("Did not remove operator rights from user \"" + userName + "\" in channel \"" + msg.channel + "\". He is not in this channel.",getContext());
            }
        })

        //Mutator Messages
        .match(SetUserListMessage.class, msg -> {
            clientUserActor.forward(msg,getContext());
        })
        .match(GetContentMessage.class, msg -> {
            if (channelRef != null && userMode != UserMode.OUT && userMode != UserMode.BANNED) {
                channelRef.forward(msg,getContext());
            } else{
                tellClientSystem("Could not get content for channel \"" + msg.channel + "\". You are not in this channel.");
            }
        })
        .match(SetContentMessage.class, msg -> {
            clientUserActor.forward(msg,getContext());
        })
        .match(BecomeOwnerMessage.class, msg ->{
            userMode = UserMode.OWNER;
            if(userName.startsWith("+") || userName.startsWith("@")){
                userName = "$" + userName.substring(1);
            }else {
                userName = "$" + userName;
            }
            msg.userName = userName;
            ActorSelection sel = getContext().actorSelection(channelCreatorPath + "/" + channelName);
            ActorRef userChannel = HelperFunctions.getActorRefBySelection(sel);
            if(userChannel != null)
                userChannel.forward(msg,getContext());
        })
        .match(KillChannelMessage.class, msg -> {
            if (userMode == UserMode.OWNER) {
                msg.killer = userName;
                if(channelRef != null)
                    channelRef.tell(msg, self());
            }
        })
        .match(ExitMessage.class, msg -> {
            channelRef.tell(new LeaveMessage(userName, userMode, channelName), self());
            self().tell(akka.actor.PoisonPill.getInstance(), self());
        })
        .build();
    }


    private void tellClient(String message) {
        clientUserActor.tell(new TextMessage(channelName,message), self());
    }
    private void tellClientSystem(String message) {
        tellClient("SYSTEM: " + message);
    }


}
