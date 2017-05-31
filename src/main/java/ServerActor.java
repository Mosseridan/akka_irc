
import Shared.Messages.ConnectMessage;
import Shared.Messages.IncomingPrivateMessage;
import Shared.Messages.SetChannelListMessage;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.routing.ActorRefRoutee;
import akka.routing.BroadcastRoutingLogic;
import akka.routing.Router;
import javafx.scene.control.TreeItem;

public class ServerActor extends AbstractActor {

    final String channelCreatorPath = "/user/Server/ChannelCreator";
//    Router router;
//    TreeItem<String> allUsersBranch;
//    TreeItem<String> channelsAndUsersTree;

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ConnectMessage.class, connMsg -> {


                    // Create ServerUserActor in the system

                    ActorRef serverUserActor = getContext().actorOf(Props.create(ServerUserActor.class, connMsg.userName, sender()), "ServerUser" + connMsg.userName);
                    ActorRef clientUserActor = sender();

                    connMsg.serverUserActor = serverUserActor;
                    clientUserActor.tell(connMsg, self());

//                    router = router.addRoutee(new ActorRefRoutee(clientUserActor));
//                    //router = router.addRoutee(new ActorRefRoutee(serverUserActor));
//                    makeBranch(connMsg.userName, allUsersBranch);
//                    router.route(new SetChannelListMessage("WTF???"), self());
//                    router.route("WTF?????", self());

//                    // tell the channel creator to send the user the channel list
//                    ActorSelection sel = getContext().actorSelection(channelCreatorPath);
//                    ActorRef channelCreator = HelperFunctions.getActorRefBySelection(sel);
//
//                    GetChannelListMessage chLstMsg = new GetChannelListMessage();
//                    channelCreator.forward(chLstMsg, getContext());
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
