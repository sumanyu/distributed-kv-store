package core

import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props}
import akka.cluster.Cluster
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings, ClusterSingletonProxy, ClusterSingletonProxySettings}
import akka.http.scaladsl.Http
import akka.pattern.ask
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import core.cluster.ClusterCoordinator.GetPrimary
import core.cluster.Replica.{Get, Put}
import core.cluster._
import core.http.HttpRoute

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Try

object Main extends App with ShutdownHook with HttpRoute {

  val config = ConfigFactory.load()

  implicit val system = ActorSystem("KVStore", config)
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val quorumSize = config.getConfig("akka.cluster").getInt("quorum-size")

  system.actorOf(ClusterSingletonManager.props(
    singletonProps = Props(classOf[ClusterCoordinator], quorumSize),
    terminationMessage = PoisonPill,
    settings = ClusterSingletonManagerSettings(system)),
    name = "clusterCoordinator")

  val proxy = system.actorOf(ClusterSingletonProxy.props(
    singletonManagerPath = "/user/clusterCoordinator",
    settings = ClusterSingletonProxySettings(system)),
    name = "clusterCoordinatorProxy")

  val replica = system.actorOf(Replica.props(proxy))

  val host = config.getString("http.interface")
  val port = config.getInt("http.port")
  val bindingFuture = Http().bindAndHandle(route, host, port)

  testing()

  addShutdownHook(system)

  def primaryRef: Future[Option[ActorRef]] = {
    (proxy ? GetPrimary).mapTo[Option[ActorRef]]
  }

  def testing() = {
    implicit def intToString(x: Int): String = x.toString

    def putValues = {
      for {
        opt <- (proxy ? GetPrimary).mapTo[Option[ActorRef]]
        primary <- opt
        i <- 0 to 1000
      } {
        primary ! Put(i, i*i)
      }
    }

    def getValues = {
      system.scheduler.scheduleOnce(5.seconds) {
        for {
          opt <- (proxy ? GetPrimary).mapTo[Option[ActorRef]]
          primary <- opt
          i <- 0 to 1000
          v <- (primary ? Get(i)).mapTo[String]
        } {
          println(v)
        }
      }
    }

    putValues
    getValues
  }
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