package core.cluster

import akka.actor.SupervisorStrategy.{Resume, Stop}
import akka.actor._

trait SupervisionStrategy extends ActorLogging { self: Actor =>
  override val supervisorStrategy =
    AllForOneStrategy() {
      case e: ActorInitializationException =>
        log.error(e, "Escalating to parent")
        Stop
      case e: ActorKilledException =>
        Stop
      case e: Exception => {
        log.error(e, s"Resume actor")
        Resume
      }
    }
}
