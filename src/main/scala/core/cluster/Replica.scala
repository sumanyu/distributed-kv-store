package core.cluster

import akka.actor.{ActorRef, Props, Actor}
import core.cluster.ClusterCoordinator.Join

class Replica(clusterCoordinatorProxy: ActorRef) extends Actor {

  override def preStart() = {
    clusterCoordinatorProxy ! Join
  }

  def receive = {
    case _ =>
  }
}

object Replica {
  def props(clusterCoordinatorProxy: ActorRef): Props = {
    Props(classOf[Replica], clusterCoordinatorProxy)
  }
}