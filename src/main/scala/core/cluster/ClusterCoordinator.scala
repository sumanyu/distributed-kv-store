package core.cluster

import akka.actor._
import akka.cluster.ClusterEvent._
import akka.cluster.{Cluster, Member}
import core.cluster.ClusterCoordinator._

//Performs leader election / split brain strategy
class ClusterCoordinator(quorumSize: Int) extends Actor with ActorLogging {

  val cluster = Cluster(context.system)
  var members: Set[Member] = Set.empty[Member]

  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberEvent],
      classOf[UnreachableMember], classOf[ReachableMember])
  }

  override def postStop(): Unit = cluster.unsubscribe(self)

  var primaryOpt: Option[Primary] = None
  var replicas = Map[Address, ActorRef]()

  def receive = {

    case Join(memberAddress) =>
      log.info("Replica is requesting to join the cluster: {}", memberAddress)

      if (primaryOpt.isEmpty) {
        log.info("Adding replica as primary: {}", memberAddress)
        setPrimary(memberAddress, sender())
      } else {
        log.info("Adding replica as secondary: {}", memberAddress)
        setSecondary(memberAddress)
      }

    case Leave(memberAddress) =>
      log.warning("Replica is Removed: {}", memberAddress)
      memberRemoved(memberAddress)

    case MemberRemoved(member, previousStatus) =>
      log.warning("Member is Removed: {} after {}", member.address, previousStatus)
      memberRemoved(member.address)
      members -= member

    case GetPrimary => sender ! primaryOpt.map(_.ref)

    case MemberUp(member) =>
      log.warning("Member is Up: {}", member.address)
      members += member

    case UnreachableMember(member) =>
      log.warning("Member detected as unreachable: {}", member)

    case ReachableMember(member) =>
      log.warning("Member came back from the dead as reachable: {}", member)

    case MemberExited(member) =>
      log.warning("Member is Exiting: {}", member.address)

    case _: MemberEvent => // ignore
  }

  def primaryIsDown(address: Address, memberAddress: Address) = address == memberAddress

  def electNewPrimary() = {
    primaryOpt = None
    replicas.headOption foreach { case ((address, ref)) =>
      replicas -= address
      setPrimary(address, ref)
    }
  }

  def setPrimary(address: Address, ref: ActorRef) = {
    primaryOpt = Some(Primary(address, ref))
    primaryOpt foreach { _.ref ! JoinedPrimary }
    primaryOpt foreach { _.ref ! InitializeReplicas(replicas.values) }
  }

  def setSecondary(memberAddress: Address) = {
    sender() ! JoinedSecondary
    replicas += memberAddress -> sender()
    primaryOpt foreach { _.ref ! AddReplica(sender()) }
  }

  def removeSecondary(address: Address) = {
    val ref = replicas(address)
    replicas -= address
    primaryOpt foreach { _.ref ! RemoveReplica(ref) }
  }

  def memberRemoved(memberAddress: Address) = {
    primaryOpt match {
      case Some(Primary(address, _)) if primaryIsDown(address, memberAddress) =>
        log.debug("Primary is down. Electing new primary...")
        electNewPrimary()
      case Some(Primary(address, _)) =>
        log.debug("Removing secondary...")
        removeSecondary(memberAddress)
      case None =>
    }
  }
}

object ClusterCoordinator {

  case class Primary(address: Address, ref: ActorRef)

  case object GetPrimary

  case class Join(address: Address)
  case class Leave(address: Address)

  case object JoinedPrimary
  case object JoinedSecondary

  case class InitializeReplicas(replicas: Iterable[ActorRef])
  case class AddReplica(replica: ActorRef)
  case class RemoveReplica(replica: ActorRef)

  def props(): Props = {
    Props(classOf[ClusterCoordinator])
  }
}

