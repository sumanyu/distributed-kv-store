package core.http

import akka.actor.ActorRef
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout
import core.cluster.Replica.{Delete, Get}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

trait HttpRoute extends CorsSupport {

  val timeoutPeriod = 3.seconds
  implicit val timeout = Timeout(timeoutPeriod)

  def primaryRef: Future[Option[ActorRef]]

  val route = pathPrefix("v2") {
    corsHandler {
      pathPrefix(Segment) { key =>
        pathEndOrSingleSlash {
          get {
            queryDb { ref =>
              (ref ? Get(key)).mapTo[String]
            }
          } ~ delete {
            queryDb { ref =>
              (ref ? Delete(key)).mapTo[String]
            }
          }
        }
      }
    }
  }

  def queryDb[A](f: ActorRef => Future[String]) = {
    onComplete(primaryRef) {
      case Success(value) =>
        value match {
          case Some(ref) =>
            onComplete(f(ref)) {
              case Success(v) => complete(HttpResponse(status = StatusCodes.OK, entity = v))
              case Failure(ex) => complete(HttpResponse(status = StatusCodes.BadRequest, entity = ex.getMessage))
            }
          case None =>
            complete(HttpResponse(status = StatusCodes.BadRequest, entity = "Primary is not elected yet"))
        }
      case Failure(ex) =>
        complete(HttpResponse(status = StatusCodes.BadRequest, entity = ex.getMessage))
    }
  }
}