import akka.actor.AbstractActor;

import Shared.Messages.*;
import akka.routing.ActorRefRoutee;
import akka.routing.BroadcastRoutingLogic;
import akka.routing.Router;

import java.util.ArrayList;
import java.util.List;

public class ChannelActor extends AbstractActor {

    private String channelName;
    private String title;
    Router router;
    public List<String> userList;
    private String conversasion;

    public ChannelActor(String channelName) {
        this.channelName = channelName;
        title = null;
        router = new Router(new BroadcastRoutingLogic());
        userList = new ArrayList<String>();
        conversasion = "";
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(JoinMessage.class, msg -> {
                    getContext().watch(sender());
                    UserMode mode = UserMode.USER;
                    String userName = msg.userName;
                    if(router.routees().isEmpty()){
                        mode = UserMode.OWNER;
                        userName = "$" + userName;
                    }
                    userList.add(userName);
                    router.route(new UserJoinedMessage(userName, channelName), self());
                    router.route(new UserJoinedMessage(userName, channelName), self());
                    sender().tell(new JoinApprovalMessage(userName,mode,channelName,self(),userList), self());
                    router = router.addRoutee(new ActorRefRoutee(sender()));
                    record("*** joins: " + userName);
                })
                .match(LeaveMessage.class, msg -> {
                    router = router.removeRoutee(sender());
                    userList.remove(msg.userName);
                    router.route(new UserLeftMessage(msg.userName, channelName),self());
                    record("*** parts: " + msg.userName);

                    //router.route(new UserLeftMessage(msg.userName, channelName),self());
                    //broadcastMessage();
                    // arbitrarily select another owner
                    if (router.routees().isEmpty()){
                        //Todo: kill channel actor
                    }
                    else if (msg.userMode == UserMode.OWNER) {
                        router.routees().head().send(new BecomeOwnerMessage(channelName), self());
                        broadcastMessage("*** " + msg.userName + "became owener");
                        router.route(new SetUserListMessage(channelName,userList),self());
                    }
                })
                .match(OutgoingBroadcastMessage.class, msg -> {
                    broadcastMessage("<" + msg.sender + "> " + msg.message);
                })
                .match(ChangeTitleMessage.class, msg -> {
                    title = msg.newTitle;
                    broadcastMessage("*** Channel title changed to " + title);
                })
               .match(IncomingKickMessage.class, msg -> {
                    router = router.removeRoutee(sender());
                    userList.remove(msg.userName);
                    broadcastMessage("*** " + msg.userName + " kicked by " + msg.sender);
                   router.route(new SetUserListMessage(channelName,userList),self());
               })
                .match(IncomingBanMessage.class, msg -> {
                    router = router.removeRoutee(sender());
                    userList.remove(msg.userName);
                    broadcastMessage("*** " + msg.userName + " banned by " + msg.sender);
                    router.route(new SetUserListMessage(channelName,userList),self());
                })
                .match(IncomingAddVoicedMessage.class, msg -> {
                    userList.set(userList.indexOf(msg.oldUserName),msg.newUserName);
                    broadcastMessage("*** " + msg.newUserName + " voiced by " + msg.sender);
                    router.route(new SetUserListMessage(channelName,userList),self());

                })
                .match(IncomingRemoveVoicedMessage.class, msg -> {
                    userList.set(userList.indexOf(msg.oldUserName),msg.newUserName);
                    broadcastMessage("*** " + msg.newUserName + " was unvoiced by " + msg.sender);
                    router.route(new SetUserListMessage(channelName,userList),self());
                })
                .match(IncomingAddOperatorMessage.class, msg -> {
                    userList.set(userList.indexOf(msg.oldUserName),msg.newUserName);
                    broadcastMessage("*** " + msg.newUserName + " apointed operator by " + msg.sender);
                    router.route(new SetUserListMessage(channelName,userList),self());
                })
                .match(IncomingRemoveOperatorMessage.class, msg -> {
                    userList.set(userList.indexOf(msg.oldUserName),msg.newUserName);
                    broadcastMessage("*** " + msg.newUserName + " removed from channel operators by " + msg.sender);
                    router.route(new SetUserListMessage(channelName,userList),self());
                })
                .match(GetContentMessage.class, msg->{
                    sender().forward(new SetContentMessage(channelName, userList, conversasion),getContext());
                })
                .build();
    }

    private void broadcastMessage(String message) {
//        IncomingBroadcastTextMessage incBrdTxtMsg = new IncomingBroadcastTextMessage();
//        incBrdTxtMsg.text = "<" + channelName + (title != null ? ": ~" + title : "") + "> " + message;
        router.route(new IncomingBroadcastMessage(channelName, message), self());
        record(message);
    }

    private void record(String message){
        if(conversasion.length() > 2048)
            conversasion = conversasion.substring(1024);
        conversasion = conversasion + message +"\n";
    }
}
