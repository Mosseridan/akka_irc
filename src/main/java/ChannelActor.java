import Shared.Messages.*;
import akka.actor.AbstractActor;
import akka.actor.Terminated;
import akka.routing.ActorRefRoutee;
import akka.routing.BroadcastRoutingLogic;
import akka.routing.Routee;
import akka.routing.Router;

public class ChannelActor extends AbstractActor {

    private String channelName;
    private String title;
    Router router;

    public ChannelActor(String channelName) {
        this.channelName = channelName;
        title = null;
        router = new Router(new BroadcastRoutingLogic());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            /** OUTGOIN MESSAGES **/
            // LeaveMessage
            .match(LeaveMessage.class, this::receiveLeave)
            // ChangeTitleMessage
            .match(ChangeTitleMessage.class, this::receiveChangeTitle)
            // GetAllUserNamesMessage
            .match(GetAllUserNamesMessage.class, this::receiveGetAllUserNames)
            /** INCOMING MESSAGES **/
            // JoinMessage
            .match(JoinMessage.class, this::receiveJoin)
            // JoinApprovalMessage
            .match(IncomingKillChannelMessage.class, this::receiveIncomingKillChannel)
            // AnnouncementMessage
            .match(AnnouncementMessage.class, this::receiveAnnouncement)
            // IncomingBroadcastMessage
            .match(IncomingBroadcastMessage.class, this::receiveIncomingBroadcast)
            // IncomingKickMessage
            .match(IncomingKickMessage.class, this::receiveIncomingKick)
            // IncomingBanMessage
            .match(IncomingBanMessage.class, this::receiveIncomingBan)
            // IncomingAddVoicedMessage
            .match(IncomingAddVoicedMessage.class, this::receiveIncomingAddVoiced)
            // IncomingAddOperatorMessage
            .match(IncomingAddOperatorMessage.class, this::receiveIncomingAddOperator)
            // IncomingRemoveVoicedMessage
            .match(IncomingRemoveVoicedMessage.class, this::receiveIncomingRemoveVoiced)
            // IncomingRemoveOperatorMessage
            .match(IncomingRemoveOperatorMessage.class, this::receiveIncomingRemoveOperator)
            // ApointOwnerMessage
            .match(ApointOwnerMessage.class,this::receiveApointOwner)
            // ChangeUserNameMessage
            .match(ChangeUserNameMessage.class,this::receiveChangeUserName)
            // Terminated message
            .match(Terminated.class, this::receiveTerminated)
            // For any unhandled message
            .matchAny(this::receiveUnhandled)
            .build();
    }


    /** OUTGOING MESSAGES **/
    // LeaveMessage
    private void receiveLeave(LeaveMessage msg){
        router = router.removeRoutee(getSender());
        router.route(new UserLeftMessage(msg.getUserName(), channelName),getSelf());
        // arbitrarily select another owner
        if (router.routees().isEmpty()) {
            getContext().stop(getSelf());
        }
    }

    // ChangeTitleMessage
    private void receiveChangeTitle(ChangeTitleMessage msg) {
        title = msg.getTitle();
        router.route(new AnnouncementMessage(msg.getChannelName(),msg.getSenderName()+" changed the channel title changed to " + title),getSender());
    }

    // GetAllUserNamesMessage
    private void  receiveGetAllUserNames(GetAllUserNamesMessage msg) {
        router.route(new GetUserNameMessage(msg.getSenderName(),channelName),getSender());
    }


    /** INCOMING MESSAGES **/
    // JoinMessage
    private  void receiveJoin(JoinMessage msg) {
            getContext().watch(getSender());
            getSender().tell(new JoinApprovalMessage(msg.getUserName(),channelName,getSelf()), getSelf());
            router.route(new UserJoinedMessage(msg.getUserName(), channelName), getSelf());
            router = router.addRoutee(new ActorRefRoutee(getSender()));
    }

    // IncomingKillChannelMessage
    private void receiveIncomingKillChannel(IncomingKillChannelMessage msg) {
        router.route(msg,getSelf());
        getContext().stop(getSelf());
    }

    // AnnouncementMessage
    private void receiveAnnouncement(AnnouncementMessage msg) {
        router.route(msg,getSender());
    }

    // IncomingBroadcastMessage
    private void receiveIncomingBroadcast(IncomingBroadcastMessage msg) {
        router.route(msg,getSender());
    }

    // IncomingKickMessage
    private void receiveIncomingKick(IncomingKickMessage msg) {
        router = router.removeRoutee(getSender());
        router.route(new AnnouncementMessage(channelName,msg.getUserName()+" kicked by "+msg.getSenderName()),getSelf());
    }

    // IncomingBanMessage
    private void receiveIncomingBan(IncomingBanMessage msg){
        router.route(new AnnouncementMessage(channelName,msg.getUserName()+" banned by "+msg.getSenderName()),getSelf());
    }

    // IncomingAddVoicedMessage
    private void receiveIncomingAddVoiced(IncomingAddVoicedMessage msg){
        router.route(new AnnouncementMessage(channelName,msg.getUserName()+" voiced by "+msg.getSenderName()),getSelf());
    }
    // IncomingAddOperatorMessage
    private void receiveIncomingAddOperator(IncomingAddOperatorMessage msg){
        router.route(new AnnouncementMessage(channelName,msg.getUserName()+" appointed operator by "+msg.getSenderName()),getSelf());

    }

    // IncomingRemoveVoicedMessage
    private void receiveIncomingRemoveVoiced(IncomingRemoveVoicedMessage msg){
        router.route(new AnnouncementMessage(channelName,msg.getUserName()+" voiced rights where revoked by "+msg.getSenderName()),getSelf());

    }

    // IncomingRemoveOperatorMessage
    private void receiveIncomingRemoveOperator(IncomingRemoveOperatorMessage msg) {
        router.route(new AnnouncementMessage(channelName,msg.getUserName()+" operator rights where revoked by "+msg.getSenderName()),getSelf());
    }

    //ApointOwnerMessage
    private void receiveApointOwner(ApointOwnerMessage msg){
        router.routees().head().send(new BecomeOwnerMessage(),getSelf());
    }

    //ChangeUserNameMessage
    private void receiveChangeUserName(ChangeUserNameMessage msg){
        router.route(msg,getSender());
    }

    // For any unhandled message
    private void receiveUnhandled(Object o) {
        getSender().tell(new ErrorMessage("Send "+o.toString(),"This message is invalid in the current getContext"),getSelf());
    }

    // Terminated Message
    private void receiveTerminated (Terminated msg){
        System.out.println("$$$ in channelActor "+channelName+" received Terminated: "+msg.toString());
        router = router.removeRoutee(msg.actor());
    }

//    private void record(String message){
//        if(conversasion.length() > 2048)
//            conversasion = conversasion.substring(1024);
//        conversasion = conversasion + message +"\n";
//    }
}
