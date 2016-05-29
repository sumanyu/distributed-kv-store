package core.cluster

import akka.actor.{ActorLogging, ActorRef, Props, Actor}
import akka.cluster.Cluster
import core.api.{HashInMemoryKVStore, KVStore}
import core.cluster.ClusterCoordinator._
import core.cluster.Replica._

class Replica(clusterCoordinatorProxy: ActorRef, kVStore: StringKVStore) extends Actor with ActorLogging {

  val cluster = Cluster(context.system)
  var replicaSet = Set[ActorRef]()

  override def preStart() = {
    clusterCoordinatorProxy ! Join(cluster.selfAddress)
  }

  def receive = {
    case JoinedPrimary => {
      log.info(s"JoinedPrimary at: ${cluster.selfAddress.toString}")
      context.become(primary)
    }
    case JoinedSecondary => {
      log.info(s"JoinedSecondary at: ${cluster.selfAddress.toString}")
      context.become(secondary)
    }
  }

  val primary: Receive = {
    case Get(key) => sender ! kVStore.get(key)
    case Contains(key) => sender ! kVStore.contains(key)

    case Put(key, value) =>
      sender ! kVStore.put(key, value)
      replicaSet.foreach { _ ! Replicate(key, Some(value)) }

    case Delete(key) =>
      sender ! kVStore.delete(key)
      replicaSet.foreach { _ ! Replicate(key, None) }

    case InitializeReplicas(replicas) =>
      sendInitialStateToReplicas(replicas)
      replicaSet = replicas.toSet

    case AddReplica(replica) =>
      sendInitialStateToReplicas(Iterable(replica))
      replicaSet += replica

    case RemoveReplica(replica) =>
      replicaSet -= replica
  }

  def sendInitialStateToReplicas(replicas: Iterable[ActorRef]) = {
    replicas.foreach { replica =>
      replica ! RecoverFromSnapshot(kVStore.state)
    }
  }

  val secondary: Receive = {
    case RecoverFromSnapshot(state) => state.foreach { case (key, value) => kVStore.put(key, value) }
    case Replicate(key, valueOpt) => valueOpt match {
      case Some(value) => kVStore.put(key, value)
      case None => kVStore.delete(key)
    }
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

  case class RecoverFromSnapshot(state: IndexedSeq[(KeyType, ValueType)])

  case class Replicate(key: KeyType, value: Option[ValueType])

  def props(clusterCoordinatorProxy: ActorRef,
            kVStore: StringKVStore = new HashInMemoryKVStore[KeyType, ValueType]): Props = {
    Props(classOf[Replica], clusterCoordinatorProxy, kVStore)
  }
}