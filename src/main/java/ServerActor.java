
import Shared.Messages.ConnectMessage;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;

public class ServerActor extends AbstractActor {

    final String channelCreatorPath = "/user/Server/ChannelCreator";

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ConnectMessage.class, connMsg -> {


                    // Create ServerUserActor in the system

                    ActorRef serverUserActor = getContext().actorOf(Props.create(ServerUserActor.class, connMsg.userName, sender()), "ServerUser" + connMsg.userName);
                    ActorRef clientUserActor = sender();

                    connMsg.serverUserActor = serverUserActor;
                    clientUserActor.tell(connMsg, self());
                })
                .build();
    }

    @Override
    public void preStart() {
//        router = new Router(new BroadcastRoutingLogic());
//        channelsAndUsersTree = new TreeItem<>();
//        allUsersBranch = makeBranch("All Users",channelsAndUsersTree);
        getContext().actorOf(Props.create(ChannelCreator.class),  "ChannelCreator");
    }
//
//    // Create Tree Branche with the given title and parent
//    public TreeItem<String> makeBranch(String title, TreeItem<String> parent){
//        TreeItem<String> item = new TreeItem<>(title);
//        item.setExpanded(true);
//        parent.getChildren().add(item);
//        return item;
//    }
}
