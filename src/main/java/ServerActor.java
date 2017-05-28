import Shared.Messages.ChannelListMessage;
import Shared.Messages.ConnectMessage;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;

public class ServerActor extends AbstractActor {

    final String channelCreatorPath = "/user/Server/ChannelCreator";
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ConnectMessage.class, connMsg -> {

                    ConnectMessage connectMessage = new ConnectMessage();
                    // Create ServerUserActor in the system

                    ActorRef serverUserActor = getContext().actorOf(Props.create(ServerUserActor.class, connMsg.userName), "ServerUser" + connMsg.userName);

                    connectMessage.serverUserActor = serverUserActor;
                    ActorRef clientUserActor = sender();
                    clientUserActor.tell(connectMessage, self());

                    connectMessage.clientUserActor = clientUserActor;
                    serverUserActor.tell(connectMessage, self());

                    // tell the channel creator to send the user the channel list
                    ActorSelection sel = getContext().actorSelection(channelCreatorPath);
                    ActorRef channelCreator = HelperFunctions.getActorRefBySelection(sel);

                    ChannelListMessage chLstMsg = new ChannelListMessage();
                    channelCreator.forward(chLstMsg, getContext());
                })
                .build();
    }

    @Override
    public void preStart() {
        getContext().actorOf(Props.create(ChannelCreator.class),  "ChannelCreator");
    }
}
