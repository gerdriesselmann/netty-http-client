import sbt._

ThisBuild / scalaVersion := "2.13.3"
ThisBuild / organization := "net.gerd-riesselmann"

lazy val httpclient = (project in file("."))
	.settings(
		name := "HttpClient",
		libraryDependencies += "io.netty" % "netty-buffer" % "4.1.52.Final",
		libraryDependencies += "io.netty" % "netty-codec" % "4.1.52.Final",
		libraryDependencies += "io.netty" % "netty-codec-http" % "4.1.52.Final",
		libraryDependencies += "io.netty" % "netty-common" % "4.1.52.Final",
		libraryDependencies += "io.netty" % "netty-handler" % "4.1.52.Final",
		libraryDependencies += "io.netty" % "netty-transport" % "4.1.52.Final",
		libraryDependencies += "io.netty" % "netty-codec-socks" % "4.1.52.Final",
		libraryDependencies += "io.netty" % "netty-handler-proxy" % "4.1.52.Final",
		libraryDependencies += "io.netty" % "netty-resolver-dns" % "4.1.52.Final",
		libraryDependencies += "io.netty" % "netty-transport-native-epoll" % "4.1.52.Final"
	)