package core.http

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration._

trait HttpRoute extends CorsSupport {

  val timeoutPeriod = 3.seconds
  implicit val timeout = Timeout(timeoutPeriod)

  def primaryRef: Future[ActorRef]

  val route = pathPrefix("v2") {
    corsHandler {
      pathPrefix(Segment) { key =>
        pathEndOrSingleSlash {
          get {
            complete("Get")
//            (dbRef ? Get(key)).mapTo[String]
//            complete(HttpResponse(status = OK, entity = HttpEntity(`application/json`, "sdfs")))
          } ~ delete {
            complete("Delete")
//            (dbRef ? Delete(key))
          }
        }
      }
    }
  }
}