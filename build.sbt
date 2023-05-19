name := """task"""
organization := "task"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.10"

libraryDependencies += guice
libraryDependencies += ws
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.17.0"
libraryDependencies += "io.github.galliaproject" %% "gallia-core" % "0.4.0"

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "task.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "task.binders._"
