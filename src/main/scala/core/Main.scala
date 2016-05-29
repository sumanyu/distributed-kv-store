package core

import akka.actor.{Props, PoisonPill, ActorSystem}
import akka.cluster.singleton.{ClusterSingletonProxySettings, ClusterSingletonProxy, ClusterSingletonManagerSettings, ClusterSingletonManager}
import core.cluster._

object Main extends App {
  implicit val system = ActorSystem("KVStore")

  system.actorOf(ClusterSingletonManager.props(
    singletonProps = Props(classOf[ClusterCoordinator]),
    terminationMessage = PoisonPill,
    settings = ClusterSingletonManagerSettings(system)),
    name = "clusterCoordinator")

  val proxy = system.actorOf(ClusterSingletonProxy.props(
    singletonManagerPath = "/user/clusterCoordinator",
    settings = ClusterSingletonProxySettings(system)),
    name = "clusterCoordinatorProxy")

  val replica = system.actorOf(Replica.props(proxy))
}