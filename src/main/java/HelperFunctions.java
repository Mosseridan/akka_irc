

import akka.actor.*;
import akka.pattern.AskableActorSelection;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

public class HelperFunctions {

    public static ActorRef getActorRefBySelection(ActorSelection sel) {
        AskableActorSelection asker = new AskableActorSelection(sel);

        Timeout timeOut = new Timeout(Duration.create(1, TimeUnit.SECONDS));
        Future<Object> future = asker.ask(new Identify(1), timeOut);
        ActorIdentity identity;

        try {
            identity = (ActorIdentity) Await.result(future, timeOut.duration());
        } catch (Exception e) {
            identity = null;
        }
        // Also check if not alive anymore
        if (identity == null) {
            return null;
        }

        if (identity.ref().isDefined()) {
            return identity.ref().get();
        } else {
            return null;
        }
    }
}
