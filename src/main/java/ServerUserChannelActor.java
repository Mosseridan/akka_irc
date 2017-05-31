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
            clientUserActor.tell(msg, self());
        })
        .match(UserJoinedMessage.class, msg->{
            clientUserActor.forward(msg,getContext());
        })
        .match(LeaveMessage.class, msg -> {
            //ActorSelection sel = getContext().actorSelection(channelCreatorPath + "/" + channelName);
            //ActorRef channelToLeave = HelperFunctions.getActorRefBySelection(sel);
            msg.userMode = userMode;
            if (channelRef != null) {
                channelRef.tell(msg, self());
                userMode = UserMode.OUT;
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
        .match(IncomingKickMessage.class, msg -> {
          if (channelRef != null && userMode != UserMode.OUT) {
              if (userMode == UserMode.USER || userMode == UserMode.VOICE || userMode == UserMode.OPERATOR) {
                  String oldUserName = userName;
                  userName = msg.userName; //reset UserMode prefix in userName
                  msg.userName = oldUserName; // add UserMode prefix to userName
                  userMode = UserMode.OUT;
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
        .match(IncomingBanMessage.class, msg -> {
            if (channelRef != null && userMode != UserMode.OUT) {
                if (userMode == UserMode.USER || userMode == UserMode.VOICE || userMode == UserMode.OPERATOR) {
                    String oldUserName = userName;
                    userName = msg.userName; //reset UserMode prefix in userName
                    msg.userName = oldUserName; // add UserMode prefix to userName
                    userMode = UserMode.BANNED;
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
        .match(IncomingAddVoicedMessage.class, msg -> {
            if (channelRef != null && userMode != UserMode.OUT) {
                if (userMode == UserMode.USER){
                    msg.oldUserName = userName;
                    userName = msg.newUserName;
                    userMode = UserMode.VOICE;
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
        .match(IncomingRemoveVoicedMessage.class, msg -> {
            if (channelRef != null && userMode != UserMode.OUT) {
                if (userMode == UserMode.VOICE){
                    userName = msg.newUserName;
                    userMode = UserMode.USER;
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
        .match(IncomingAddOperatorMessage.class, msg -> {
            if (channelRef != null && userMode != UserMode.OUT) {
                if (userMode == UserMode.USER || userMode == UserMode.VOICE){
                    msg.oldUserName = userName;
                    userName = msg.newUserName;
                    userMode = UserMode.OPERATOR;
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
        .match(IncomingRemoveOperatorMessage.class, msg -> {
            if (channelRef != null && userMode != UserMode.OUT) {
                if (userMode == UserMode.OPERATOR){
                    userName = msg.newUserName;
                    userMode = UserMode.USER;
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
        .match(SetUserListMessage.class, msg ->{
            clientUserActor.forward(msg,getContext());
        })
//        .match(OutgoingPromoteDemoteMessage.class, prmDemUsrMsg -> {
//            if (userMode == UserMode.OWNER || userMode == UserMode.OPERATOR) {
//
//                // get the userChannel for mode changing
//                ActorSelection sel = getContext().actorSelection(serverUserPath + prmDemUsrMsg.userToPromoteDemote + "/" + channelName);
//                ActorRef userChannelToPromDem = HelperFunctions.getActorRefBySelection(sel);
//
//                if (userChannelToPromDem != null) {
//                    // tell it to change its mode
//                    userChannelToPromDem.tell(new IncomingPromoteDemoteMessage(prmDemUsrMsg.newUserMode, prmDemUsrMsg.sender, prmDemUsrMsg.channel), self());
//                } else {
//                    tellClientSystem("Requested user for state changing is not in channel");
//                }
//            } else if (userMode == UserMode.BANNED || userMode == UserMode.OUT) {
//                tellClientSystem("Out of channel, cannot change others mode");
//            } else { // can't promote/demote no one
//                tellClientSystem("You don't have the sufficient privileges to change others mode");
//            }
//
//        })        .match(OutgoingPromoteDemoteMessage.class, prmDemUsrMsg -> {
//            if (userMode == UserMode.OWNER || userMode == UserMode.OPERATOR) {
//
//                // get the userChannel for mode changing
//                ActorSelection sel = getContext().actorSelection(serverUserPath + prmDemUsrMsg.userToPromoteDemote + "/" + channelName);
//                ActorRef userChannelToPromDem = HelperFunctions.getActorRefBySelection(sel);
//
//                if (userChannelToPromDem != null) {
//                    // tell it to change its mode
//                    userChannelToPromDem.tell(new IncomingPromoteDemoteMessage(prmDemUsrMsg.newUserMode, prmDemUsrMsg.sender, prmDemUsrMsg.channel), self());
//                } else {
//                    tellClientSystem("Requested user for state changing is not in channel");
//                }
//            } else if (userMode == UserMode.BANNED || userMode == UserMode.OUT) {
//                tellClientSystem("Out of channel, cannot change others mode");
//            } else { // can't promote/demote no one
//                tellClientSystem("You don't have the sufficient privileges to change others mode");
//            }
//
//        })        .match(OutgoingPromoteDemoteMessage.class, prmDemUsrMsg -> { // TODO: continu here!
//            if (userMode == UserMode.OWNER || userMode == UserMode.OPERATOR) {
//
//                // get the userChannel for mode changing
//                ActorSelection sel = getContext().actorSelection(serverUserPath + prmDemUsrMsg.userToPromoteDemote + "/" + channelName);
//                ActorRef userChannelToPromDem = HelperFunctions.getActorRefBySelection(sel);
//
//                if (userChannelToPromDem != null) {
//                    // tell it to change its mode
//                    userChannelToPromDem.tell(new IncomingPromoteDemoteMessage(prmDemUsrMsg.newUserMode, prmDemUsrMsg.sender, prmDemUsrMsg.channel), self());
//                } else {
//                    tellClientSystem("Requested user for state changing is not in channel");
//                }
//            } else if (userMode == UserMode.BANNED || userMode == UserMode.OUT) {
//                tellClientSystem("Out of channel, cannot change others mode");
//            } else { // can't promote/demote no one
//                tellClientSystem("You don't have the sufficient privileges to change others mode");
//            }
//
//        })        .match(OutgoingPromoteDemoteMessage.class, prmDemUsrMsg -> { // TODO: continu here!
//            if (userMode == UserMode.OWNER || userMode == UserMode.OPERATOR) {
//
//                // get the userChannel for mode changing
//                ActorSelection sel = getContext().actorSelection(serverUserPath + prmDemUsrMsg.userToPromoteDemote + "/" + channelName);
//                ActorRef userChannelToPromDem = HelperFunctions.getActorRefBySelection(sel);
//
//                if (userChannelToPromDem != null) {
//                    // tell it to change its mode
//                    userChannelToPromDem.tell(new IncomingPromoteDemoteMessage(prmDemUsrMsg.newUserMode, prmDemUsrMsg.sender, prmDemUsrMsg.channel), self());
//                } else {
//                    tellClientSystem("Requested user for state changing is not in channel");
//                }
//            } else if (userMode == UserMode.BANNED || userMode == UserMode.OUT) {
//                tellClientSystem("Out of channel, cannot change others mode");
//            } else { // can't promote/demote no one
//                tellClientSystem("You don't have the sufficient privileges to change others mode");
//            }
//
//        })
//        .match(IncomingPromoteDemoteMessage.class, incPrmDemMsg -> {
//            if (userMode != UserMode.OWNER && userMode != UserMode.OPERATOR) {
//                UserMode prevUserMode = userMode;
//                userMode = incPrmDemMsg.newUserMode;
//
//                String messageToClient = null;
//                String messageToParticipants = null;
//                if (userMode == UserMode.BANNED) {
//                    getKickedFromChannel(incPrmDemMsg.sender);
//                    messageToClient = "Banned.";
//                    messageToParticipants = "User " + userName + " was banned.";
//                } else if (userMode == UserMode.OPERATOR) {
//                    messageToClient = "Status changed to OPERATOR.";
//                    messageToParticipants = "User " + userName + " was changed to OPERATOR.";
//                } else if (userMode == UserMode.USER) {
//                    messageToClient = "Status changed to USER.";
//                    messageToParticipants = "User " + userName + " was changed to USER.";
//                } else if (userMode == UserMode.VOICE) {
//                    messageToClient = "Status changed to VOICED.";
//                    messageToParticipants = "User " + userName + " was changed to VOICED";
//                } else if (userMode == UserMode.OWNER) {
//                    messageToClient = "Status changed to OWNER.";
//                    messageToParticipants = "User " + userName + " was changed to OWNER.";
//                } else if (userMode == UserMode.OUT) {
//                    getKickedFromChannel(incPrmDemMsg.sender);
//                    messageToClient = "Kicked.";
//                    messageToParticipants = "User " + userName + " was kicked.";
//                }
//                OutgoingBroadcastMessage outBrdMsg = new OutgoingBroadcastMessage();
//                outBrdMsg.text = messageToParticipants;
//                outBrdMsg.sentFrom = userName;
//
//                channel.tell(outBrdMsg, self());
//                tellClient(messageToClient);
//            }
//         })
        .build();
//        }).match(GetUserListInChannelMessage.class, setUlChMsg -> {
//            // tell the client my channel name
//            SetUserListInChannelMessage setUlLstChMsg = new SetUserListInChannelMessage();
//            setUlLstChMsg.user = userName;
//            //ulChMsg.channelName = channelName;
//            sender().tell(setUlChMsg, self());
//        }).build();
    }

//    private void getKickedFromChannel(String sender) {
//        KickMessage kckMsg = new KickMessage();
//        kckMsg.userNameToKick = userName;
//        kckMsg.channel = channelName;
//        kckMsg.userActorToKick = self();
//
//        //ActorSelection sel = getContext().actorSelection(channelCreatorPath + "/" + channelName);
//        //ActorRef channelToKickFrom = HelperFunctions.getActorRefBySelection(sel);
//
//        channelRef.tell(new KickMessage(userName, self(), sender, channelName), self());
//
//        //
//        GotKickedMessage gotKickedMessage = new GotKickedMessage();
//        getContext().parent().tell(gotKickedMessage, self());
//    }


    private void tellClient(String message) {
        clientUserActor.tell(new TextMessage(channelName,message), self());
    }
    private void tellClientSystem(String message) {
        tellClient("SYSTEM: " + message);
    }


}
