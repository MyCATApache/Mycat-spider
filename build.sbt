name := "mycat-spider"

organization := "org.opencloudb.mycat"

version := "1.0.0-SNAPSHOT"
 
scalaVersion := "2.10.3"
 

credentials += Credentials(Path.userHome / ".sbt" / "sonatype.credentials")

libraryDependencies += "org.slf4j" % "slf4j-log4j12" % "1.7.7"

libraryDependencies += "io.netty" % "netty-all" % "5.0.0.Alpha1"

libraryDependencies += "commons-httpclient" % "commons-httpclient" % "3.1"

libraryDependencies += "org.avaje" % "ebean" % "2.7.3"

libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.34"

libraryDependencies += "net.sourceforge.nekohtml" % "nekohtml" % "1.9.15"

javacOptions ++= Seq("-encoding", "UTF-8")

resolvers ++= Seq(

"Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository",

"Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",

"Akka Repository" at "http://repo.akaka.io/releases/"

)

