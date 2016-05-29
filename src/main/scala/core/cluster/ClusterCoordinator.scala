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

  var primaryOpt: Option[(Address, ActorRef)] = None
  var membersToReplica = Map[Address, ActorRef]()

  def receive = {

    case MemberUp(member) =>
      log.warning("Member is Up: {}", member.address)
      members += member

    case UnreachableMember(member) =>
      log.warning("Member detected as unreachable: {}", member)

    case ReachableMember(member) =>
      log.warning("Member came back from the dead as reachable: {}", member)

    case MemberExited(member) =>
      log.warning("Member is Exiting: {}", member.address)

    case MemberRemoved(member, previousStatus) =>
      log.warning("Member is Removed: {} after {}", member.address, previousStatus)



      members -= member

    case _: MemberEvent => // ignore

    case Join(address) =>
      if (primaryOpt.isEmpty) {
        primaryOpt = Some((address, sender()))
        primaryOpt foreach { _._2 ! JoinedPrimary }
      } else {
        sender() ! JoinedSecondary
      }

    case GetPrimary => sender ! primaryOpt
  }
}

object ClusterCoordinator {

  case object GetPrimary

  case class Join(address: Address)

  case object JoinedPrimary
  case object JoinedSecondary

  case class Replicas(replicas: Set[ActorRef])

  def props(): Props = {
    Props(classOf[ClusterCoordinator])
  }
}

