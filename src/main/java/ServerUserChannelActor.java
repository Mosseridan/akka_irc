import Shared.Messages.*;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.japi.pf.ReceiveBuilder;

public class ServerUserChannelActor extends AbstractActor {
    private AbstractActor.Receive user;
    private AbstractActor.Receive voiced;
    private AbstractActor.Receive operator;
    private AbstractActor.Receive owner;
    private AbstractActor.Receive banned;

    String userName;
    ActorRef clientUserActor;
    ActorRef serverUserActor;
    String channelName;
    ActorRef channelActor;

    final String serverUserPath = "/user/Server/ServerUser";
    final String channelCreatorPath = "/user/Server/ChannelCreator/";


    public ServerUserChannelActor(String userName, ActorRef clientUserActor, ActorRef serverUserActor, String channelName) {
        this.userName = userName;
        this.clientUserActor = clientUserActor;
        this.serverUserActor = serverUserActor;
        this.channelName = channelName;
        this.channelActor = null;

        ReceiveBuilder userBuilder = ReceiveBuilder.create();
        ReceiveBuilder voicedBuilder = ReceiveBuilder.create();
        ReceiveBuilder operatorBuilder = ReceiveBuilder.create();
        ReceiveBuilder ownerBuilder = ReceiveBuilder.create();
        ReceiveBuilder bannedBuilder = ReceiveBuilder.create();

        /** OUTGOING MESSAGES **/
        //OutgoingBroadcastMessage
        userBuilder.match(OutgoingBroadcastMessage.class, this::receiveOutgoingBroadcast);
        voicedBuilder.match(OutgoingBroadcastMessage.class, this::receiveOutgoingBroadcast);
        operatorBuilder.match(OutgoingBroadcastMessage.class, this::receiveOutgoingBroadcast);
        ownerBuilder.match(OutgoingBroadcastMessage.class, this::receiveOutgoingBroadcast);

        // LeaveMessage
        userBuilder.match(LeaveMessage.class, this::receiveLeave);
        voicedBuilder.match(LeaveMessage.class, this::receiveLeave);
        operatorBuilder.match(LeaveMessage.class, this::receiveLeave);
        ownerBuilder.match(LeaveMessage.class, this::receiveLeaveOwner);

        // OutgoingKillChannelMessage
        ownerBuilder.match(OutgoingKillChannelMessage.class, this::receiveOutgoingKillChannel);

        // OutgoingKickMessage
        operatorBuilder.match(OutgoingKickMessage.class, this::receiveOutgoingKick);
        ownerBuilder.match(OutgoingKickMessage.class, this::receiveOutgoingKick);

        // OutgoingBanMessage
        operatorBuilder.match(OutgoingBanMessage.class, this::receiveOutgoingBan);
        ownerBuilder.match(OutgoingBanMessage.class, this::receiveOutgoingBan);

        // OutgoingAddVoicedMessage
        operatorBuilder.match(OutgoingAddVoicedMessage.class, this::receiveOutgoingAddVoiced);
        ownerBuilder.match(OutgoingAddVoicedMessage.class, this::receiveOutgoingAddVoiced);

        // OutgoingAddOperatorMessage
        operatorBuilder.match(OutgoingAddOperatorMessage.class, this::receiveOutgoingAddOperator);
        ownerBuilder.match(OutgoingAddOperatorMessage.class, this::receiveOutgoingAddOperator);

        // OutgoingRemoveVoicedMessage
        operatorBuilder.match(OutgoingRemoveVoicedMessage.class, this::receiveOutgoingRemoveVoiced);
        ownerBuilder.match(OutgoingRemoveVoicedMessage.class, this::receiveOutgoingRemoveVoiced);

        // OutgoingRemoveOperatorMessage
        operatorBuilder.match(OutgoingRemoveOperatorMessage.class, this::receiveOutgoingRemoveOperator);
        ownerBuilder.match(OutgoingRemoveOperatorMessage.class, this::receiveOutgoingRemoveOperator);

        // ChangeTitleMessage
        voicedBuilder.match(ChangeTitleMessage.class, this::receiveChangeTitle);
        operatorBuilder.match(ChangeTitleMessage.class, this::receiveChangeTitle);
        ownerBuilder.match(ChangeTitleMessage.class, this::receiveChangeTitle);

        // GetAllUserNamesMessage
        userBuilder.match(GetAllUserNamesMessage.class, this::receiveGetAllUserNames);
        voicedBuilder.match(GetAllUserNamesMessage.class, this::receiveGetAllUserNames);
        operatorBuilder.match(GetAllUserNamesMessage.class, this::receiveGetAllUserNames);
        ownerBuilder.match(GetAllUserNamesMessage.class, this::receiveGetAllUserNames);

        /** INCOMING MESSAGES **/
        // JoinApprovalMessage
        userBuilder.match(JoinApprovalMessage.class, this::receiveJoinApproval);
        ownerBuilder.match(JoinApprovalMessage.class, this::receiveJoinApproval);

        // UserJoinedMessage
        userBuilder.match(UserJoinedMessage.class, this::receiveUserJoined);
        voicedBuilder.match(UserJoinedMessage.class, this::receiveUserJoined);
        operatorBuilder.match(UserJoinedMessage.class, this::receiveUserJoined);
        ownerBuilder.match(UserJoinedMessage.class, this::receiveUserJoined);

        // UserLeftMessage
        userBuilder.match(UserLeftMessage.class, this::receiveUserLeft);
        voicedBuilder.match(UserLeftMessage.class, this::receiveUserLeft);
        operatorBuilder.match(UserLeftMessage.class, this::receiveUserLeft);
        ownerBuilder.match(UserLeftMessage.class, this::receiveUserLeft);

        // IncomingKillChannelMessage
        userBuilder.match(IncomingKillChannelMessage.class, this::receiveIncomingKillChannel);
        voicedBuilder.match(IncomingKillChannelMessage.class, this::receiveIncomingKillChannel);
        operatorBuilder.match(IncomingKillChannelMessage.class, this::receiveIncomingKillChannel);
        ownerBuilder.match(IncomingKillChannelMessage.class, this::receiveIncomingKillChannel);
        bannedBuilder.match(IncomingKillChannelMessage.class, msg->getContext().stop(getSelf()));

        // IncomingBroadcastMessage
        userBuilder.match(IncomingBroadcastMessage.class, this::receiveIncomingBroadcast);
        voicedBuilder.match(IncomingBroadcastMessage.class, this::receiveIncomingBroadcast);
        operatorBuilder.match(IncomingBroadcastMessage.class, this::receiveIncomingBroadcast);
        ownerBuilder.match(IncomingBroadcastMessage.class, this::receiveIncomingBroadcast);

        // IncomingKickMessage
        userBuilder.match(IncomingKickMessage.class, this::receiveIncomingKick);
        voicedBuilder.match(IncomingKickMessage.class, this::receiveIncomingKick);
        operatorBuilder.match(IncomingKickMessage.class, this::receiveIncomingKick);

        // IncomingBanMessage
        userBuilder.match(IncomingBanMessage.class, this::receiveIncomingBan);
        voicedBuilder.match(IncomingBanMessage.class, this::receiveIncomingBan);
        operatorBuilder.match(IncomingBanMessage.class, this::receiveIncomingBan);

        // IncomingAddVoicedMessage
        userBuilder.match(IncomingAddVoicedMessage.class, this::receiveIncomingAddVoiced);

        // IncomingAddOperatorMessage
        userBuilder.match(IncomingAddOperatorMessage.class, this::receiveIncomingAddOperator);
        voicedBuilder.match(IncomingAddOperatorMessage.class, this::receiveIncomingAddOperator);

        // IncomingRemoveVoicedMessage
        voicedBuilder.match(IncomingRemoveVoicedMessage.class, this::receiveIncomingRemoveVoiced);

        // IncomingRemoveOperatorMessage
        operatorBuilder.match(IncomingRemoveOperatorMessage.class, this::receiveIncomingRemoveOperator);

        // BecomeOwnerMessage
        userBuilder.match(BecomeOwnerMessage.class, this::receiveBecomeOwner);
        voicedBuilder.match(BecomeOwnerMessage.class, this::receiveBecomeOwner);
        operatorBuilder.match(BecomeOwnerMessage.class, this::receiveBecomeOwner);

        // AddUserNameMessage
        userBuilder.match(AddUserNameMessage.class, this::receiveAddUserName);
        voicedBuilder.match(AddUserNameMessage.class, this::receiveAddUserName);
        operatorBuilder.match(AddUserNameMessage.class, this::receiveAddUserName);
        ownerBuilder.match(AddUserNameMessage.class, this::receiveAddUserName);

        // GetUserNameMessage
        userBuilder.match(GetUserNameMessage.class, this::receiveGetUserName);
        voicedBuilder.match(GetUserNameMessage.class, this::receiveGetUserName);
        operatorBuilder.match(GetUserNameMessage.class, this::receiveGetUserName);
        ownerBuilder.match(GetUserNameMessage.class, this::receiveGetUserName);
        //bannedBuilder.match(GetUserNameMessage.class, this::receiveGetUserName);

        // ErrorMessage
        userBuilder.match(ErrorMessage.class, this::receiveError);
        voicedBuilder.match(ErrorMessage.class, this::receiveError);
        operatorBuilder.match(ErrorMessage.class, this::receiveError);
        ownerBuilder.match(ErrorMessage.class, this::receiveError);
        bannedBuilder.match(ErrorMessage.class, this::receiveError);

        // AnnouncementMessage
        userBuilder.match(AnnouncementMessage.class, this::receiveAnnouncement);
        voicedBuilder.match(AnnouncementMessage.class, this::receiveAnnouncement);
        operatorBuilder.match(AnnouncementMessage.class, this::receiveAnnouncement);
        ownerBuilder.match(AnnouncementMessage.class, this::receiveAnnouncement);
        //bannedBuilder.match(AnnouncementMessage.class, this::receiveAnnouncement);

        // ChangeUserNameMessage
        userBuilder.match(ChangeUserNameMessage.class, this::receiveChangeUserName);
        voicedBuilder.match(ChangeUserNameMessage.class, this::receiveChangeUserName);
        operatorBuilder.match(ChangeUserNameMessage.class, this::receiveChangeUserName);
        ownerBuilder.match(ChangeUserNameMessage.class, this::receiveChangeUserName);


        // For any unhandled message
        userBuilder.matchAny(this::receiveUnhandled);
        voicedBuilder.matchAny(this::receiveUnhandled);
        operatorBuilder.matchAny(this::receiveUnhandled);
        ownerBuilder.matchAny(this::receiveUnhandled);
        bannedBuilder.matchAny(msg -> getSender().tell(
                new ErrorMessage("send "+msg.toString(),
                "You are banned from this channel"), getSelf()));


        user = userBuilder.build();
        voiced = voicedBuilder.build();
        operator = operatorBuilder.build();
        owner = ownerBuilder.build();
        banned = bannedBuilder.build();
    }

    @Override
    public Receive createReceive() {
        return user;
    }

    @Override
    public void preStart() {
        channelActor = getActorRef(channelCreatorPath + "/" + channelName);
        if(channelActor == null){ //channel dose not exist -> request to create it from channel creator. he will then forward a the join request to the new channel.
            ActorRef channelCreator = getActorRef(channelCreatorPath);
            channelCreator.tell(new JoinMessage(userName,channelName),getSelf());
            userName = "$" + userName;
            getContext().become(owner);
        } else{ // channel exist -> request to join it;
            channelActor.tell(new JoinMessage(userName,channelName),getSelf());
        }
    }

    /** OUTGOING MESSAGES **/
    // OutgoingBroadcastMessage
    private void receiveOutgoingBroadcast(OutgoingBroadcastMessage msg) {
        channelActor.tell(new IncomingBroadcastMessage(userName,channelName,msg.getMessage()), getSelf());
    }

    // LeaveMessage
    private void receiveLeave(LeaveMessage msg){
        channelActor.tell(new LeaveMessage(userName,channelName),getSelf());
        getContext().stop(getSelf());
    }

    private void receiveLeaveOwner(LeaveMessage msg){
        channelActor.tell(new LeaveMessage(userName,channelName),getSelf());
        channelActor.tell(new ApointOwnerMessage(), self());
        getContext().stop(getSelf());
    }

    // OutgoingKillChannelMessage
    private void receiveOutgoingKillChannel(OutgoingKillChannelMessage msg) {
        channelActor.forward(new IncomingKillChannelMessage(userName,channelName),getContext());
    }

    // OutgoingKickMessage
    private void receiveOutgoingKick(OutgoingKickMessage msg) {
        getServerUserChannelActorRef(msg.getUserName())
            .forward(new IncomingKickMessage(msg.getUserName(),userName,channelName),getContext());
    }

    // OutgoingBanMessage
    private void receiveOutgoingBan(OutgoingBanMessage msg) {
        getServerUserChannelActorRef(msg.getUserName())
            .forward(new IncomingBanMessage(msg.getUserName(),userName,channelName),getContext());
    }

    // OutgoingAddVoicedMessage
    private void receiveOutgoingAddVoiced(OutgoingAddVoicedMessage msg) {
        getServerUserChannelActorRef(msg.getUserName())
            .forward(new IncomingAddVoicedMessage(msg.getUserName(),userName,channelName),getContext());
    }

    // OutgoingAddOperatorMessage
    private void receiveOutgoingAddOperator(OutgoingAddOperatorMessage msg) {
        getServerUserChannelActorRef(msg.getUserName())
                .forward(new IncomingAddOperatorMessage(msg.getUserName(),userName,channelName),getContext());
    }

    // OutgoingRemoveVoicedMessage
    private void receiveOutgoingRemoveVoiced(OutgoingRemoveVoicedMessage msg) {
        getServerUserChannelActorRef(msg.getUserName())
                .forward(new IncomingRemoveVoicedMessage(msg.getUserName(),userName,channelName),getContext());
    }

    // OutgoingRemoveOperatorMessage
    private void receiveOutgoingRemoveOperator(OutgoingRemoveOperatorMessage msg) {
        getServerUserChannelActorRef(msg.getUserName())
                .forward(new IncomingRemoveOperatorMessage(msg.getUserName(),userName,channelName),getContext());
    }

    // ChangeTitleMessage
    private void receiveChangeTitle(ChangeTitleMessage msg) {
        channelActor.forward(new ChangeTitleMessage(userName,channelName,msg.getTitle()),getContext());
    }

    // GetAllUserNamesMessage
    private void  receiveGetAllUserNames(GetAllUserNamesMessage msg){
        channelActor.forward(msg,getContext());
    }

    /** INCOMING MESSAGES **/
    // JoinApprovalMessage
    private void receiveJoinApproval(JoinApprovalMessage msg) {
        channelActor = msg.getChannelActor();
        clientUserActor.forward(new JoinApprovalMessage(userName,channelName,self()),getContext());
    }

    // UserJoinedMessage
    private void receiveUserJoined(UserJoinedMessage msg){
        clientUserActor.forward(msg,getContext());
    }

    // UserLeftMessage
    private void receiveUserLeft(UserLeftMessage msg){
        clientUserActor.forward(msg,getContext());
    }

    // IncomingKillChannelMessage
    private void receiveIncomingKillChannel(IncomingKillChannelMessage msg){
        clientUserActor.forward(msg,getContext());
        getContext().stop(getSelf());
    }

    // IncomingBroadcastMessage
    private void receiveIncomingBroadcast(IncomingBroadcastMessage msg) {
        clientUserActor.forward(msg,getContext());
    }

    // IncomingKickMessage
    private void receiveIncomingKick(IncomingKickMessage msg){
        IncomingKickMessage response = new IncomingKickMessage(userName,msg.getSenderName(),channelName);
        clientUserActor.forward(response ,getContext());
        channelActor.forward(response,getContext());
        getContext().stop(getSelf());
    }

    // IncomingBanMessage
    private void receiveIncomingBan(IncomingBanMessage msg){
        userName = msg.getUserName();
        IncomingBanMessage response = new IncomingBanMessage(userName,msg.getSenderName(),channelName);
        clientUserActor.forward(response ,getContext());
        channelActor.forward(response,getContext());
        getContext().become(banned);
    }

    // IncomingAddVoicedMessage
    private void receiveIncomingAddVoiced(IncomingAddVoicedMessage msg){
        channelActor.tell(new ChangeUserNameMessage(userName,"+"+msg.getUserName(),channelName),getSelf());
        userName = "+" + msg.getUserName();
        channelActor.forward(new IncomingAddVoicedMessage(userName,msg.getSenderName(),channelName),getContext());

        getContext().become(voiced);
    }

    // IncomingAddOperatorMessage
    private void receiveIncomingAddOperator(IncomingAddOperatorMessage msg){
        channelActor.tell(new ChangeUserNameMessage(userName,"@"+msg.getUserName(),channelName),getSelf());
        userName = "@" + msg.getUserName();
        channelActor.forward(new IncomingAddOperatorMessage(userName,msg.getSenderName(),channelName),getContext());
        getContext().become(operator);
    }

    // IncomingRemoveVoicedMessage
    private void receiveIncomingRemoveVoiced(IncomingRemoveVoicedMessage msg){
        channelActor.tell(new ChangeUserNameMessage(userName,msg.getUserName(),channelName),getSelf());
        userName = msg.getUserName();
        channelActor.forward(new IncomingRemoveVoicedMessage(userName,msg.getSenderName(),channelName),getContext());
        getContext().unbecome();
    }

    // IncomingRemoveOperatorMessage
    private void receiveIncomingRemoveOperator(IncomingRemoveOperatorMessage msg) {
        channelActor.tell(new ChangeUserNameMessage(userName,msg.getUserName(),channelName),getSelf());
        userName = msg.getUserName();
        channelActor.forward(new IncomingRemoveOperatorMessage(userName, msg.getSenderName(), channelName), getContext());
        getContext().unbecome();
    }

    // BecomeOwnerMessage
    private void receiveBecomeOwner(BecomeOwnerMessage msg){
        if(userName.startsWith("+") || userName.startsWith("@")){
            channelActor.tell(new ChangeUserNameMessage(userName,"$" + userName.substring(1),channelName),getSelf());
            userName = "$" + userName.substring(1);
        } else {
            channelActor.tell(new ChangeUserNameMessage(userName,"$" + userName,channelName),getSelf());
            userName = "$" + userName;
        }
        channelActor.tell(new AnnouncementMessage(channelName,userName+" was apointed the new owner of channel "+channelName),getSelf());
        getContext().become(owner);
    }

    // AddUserNameMessage
    private void receiveAddUserName(AddUserNameMessage msg){
        clientUserActor.forward(msg,getContext());
    }

    // GetUserNameMessage
    private void receiveGetUserName(GetUserNameMessage msg){
        getSender().tell(new AddUserNameMessage(userName,channelName),self());
    }

    // ErrorMessage
    private void receiveError(ErrorMessage msg) {
        clientUserActor.forward(msg,getContext());
    }

    // AnnouncementMessage
    private void receiveAnnouncement(AnnouncementMessage msg) {
        clientUserActor.forward(msg,getContext());
    }

    // ChangeUserNameMessage
     private void receiveChangeUserName(ChangeUserNameMessage msg){
        clientUserActor.forward(msg,getContext());
     }

    // For any unhandled message
    private void receiveUnhandled(Object o) {
        getSender().tell(new ErrorMessage("Send "+o.toString(),"This message is invalid in the current getContext"),getSelf());
    }

    // returns an ActorRef for the actor with the given path
    private ActorRef getActorRef(String path){
        ActorSelection sel = getContext().actorSelection(path);
        return(HelperFunctions.getActorRefBySelection(sel));
    }

    // returns an ActorRef for the ServerUserChannelActor with the given name;s
    private ActorRef getServerUserChannelActorRef(String userName){
        return getActorRef(serverUserPath + userName + "/" + channelName);
    }

}