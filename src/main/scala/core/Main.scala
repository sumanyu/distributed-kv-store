package core

import akka.actor.ActorSystem

object Main extends App {
  implicit val system = ActorSystem("KVStore")



}