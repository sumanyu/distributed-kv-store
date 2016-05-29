package core

import akka.actor.{Props, PoisonPill, ActorSystem}
import akka.cluster.Cluster
import akka.cluster.singleton.{ClusterSingletonProxySettings, ClusterSingletonProxy, ClusterSingletonManagerSettings, ClusterSingletonManager}
import com.typesafe.config.ConfigFactory
import core.cluster._

import scala.concurrent.Await
import scala.util.Try
import scala.concurrent.duration._

object Main extends App with ShutdownHook {
  val config = ConfigFactory.load()
  implicit val system = ActorSystem("KVStore", config)
  val quorumSize = config.getConfig("akka.cluster").getInt("quorum-size")

  system.actorOf(ClusterSingletonManager.props(
    singletonProps = Props(classOf[ClusterCoordinator], 1),
    terminationMessage = PoisonPill,
    settings = ClusterSingletonManagerSettings(system)),
    name = "clusterCoordinator")

  val proxy = system.actorOf(ClusterSingletonProxy.props(
    singletonManagerPath = "/user/clusterCoordinator",
    settings = ClusterSingletonProxySettings(system)),
    name = "clusterCoordinatorProxy")

  val replica = system.actorOf(Replica.props(proxy))

  addShutdownHook(system)
}

trait ShutdownHook {
  def addShutdownHook(system: ActorSystem) = Cluster(system).registerOnMemberRemoved {
    // exit JVM when ActorSystem has been terminated
    system.registerOnTermination(System.exit(0))
    // shut down ActorSystem
    system.terminate()

    // In case ActorSystem shutdown takes longer than 10 seconds,
    // exit the JVM forcefully anyway.
    // We must spawn a separate thread to not block current thread,
    // since that would have blocked the shutdown of the ActorSystem.
    new Thread {
      override def run(): Unit = {
        if (Try(Await.ready(system.whenTerminated, 10.seconds)).isFailure)
          System.exit(-1)
      }
    }.start()
  }
}