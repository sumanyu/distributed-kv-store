organization := "com.sumanyu"

name := "distributed-key-value-store"

version := "0.0.1"

scalaVersion := "2.11.8"

val akka       = "2.4.6"
val akkaStream = "2.0.2"
val spray      = "1.3.3"

resolvers ++= Seq(
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "Sonatype OSS public" at "https://oss.sonatype.org/content/groups/public",
  "Apache Repository"   at "https://repository.apache.org/content/repositories/releases",
  "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"
)

libraryDependencies ++= Seq(
  "com.typesafe.akka"           %% "akka-actor" % akka,
  "com.typesafe.akka"           %% "akka-slf4j" % akka,
  "com.typesafe.akka"           %% "akka-remote" % akka,
  "com.typesafe.akka"           %% "akka-cluster" % akka,
  "com.typesafe.akka"           %% "akka-cluster-tools" % akka,

  "com.typesafe.akka"           %% "akka-stream-experimental"          % akkaStream,
  "com.typesafe.akka"           %% "akka-http-core-experimental"       % akkaStream,
  "com.typesafe.akka"           %% "akka-http-experimental"            % akkaStream,
  "com.typesafe.akka"           %% "akka-http-spray-json-experimental" % akkaStream,

  "com.typesafe.scala-logging"  %% "scala-logging"  % "3.1.0",
  "com.typesafe.akka"           %% "akka-http-spray-json-experimental" % akka
)

