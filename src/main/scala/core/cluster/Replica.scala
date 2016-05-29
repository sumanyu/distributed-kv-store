package core.cluster

import akka.actor.{ActorRef, Props, Actor}
import akka.cluster.Cluster
import core.api.KVStore
import core.cluster.ClusterCoordinator.{JoinedSecondary, JoinedPrimary, Join}
import core.cluster.Replica._

class Replica(clusterCoordinatorProxy: ActorRef, kVStore: StringKVStore) extends Actor {

  val cluster = Cluster(context.system)

  override def preStart() = {
    clusterCoordinatorProxy ! Join(cluster.selfAddress)
  }

  def receive = {
    case JoinedPrimary => context.become(primary)
    case JoinedSecondary => context.become(secondary)
  }

  val primary: Receive = {
    case Get(key) => sender ! kVStore.get(key)
    case Put(key, value) => sender ! kVStore.put(key, value)
    case Delete(key) => sender ! kVStore.delete(key)
    case Contains(key) => sender ! kVStore.contains(key)
  }

  val secondary: Receive = {
    _ =>
  }
}

object Replica {

  type KeyType = String
  type ValueType = String
  type StringKVStore = KVStore[KeyType, ValueType] //for simplicity

  sealed trait KVOperations
  case class Get(key: KeyType)
  case class Put(key: KeyType, value: ValueType)
  case class Delete(key: KeyType)
  case class Contains(key: KeyType)

  def props(clusterCoordinatorProxy: ActorRef): Props = {
    Props(classOf[Replica], clusterCoordinatorProxy)
  }
}