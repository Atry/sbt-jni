import sbt._
import sbt.Keys._
import sbt.ScriptedPlugin._

import sbtdoge.CrossPerProjectPlugin

import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys

object SbtJniBuild  extends Build {

  val scalaVersions: Seq[String] = List("2.11.8", "2.12.0-M4")
  val macrosParadiseVersion = "2.1.0"

  val commonSettings = Seq(
    version := "1.1.2-SNAPSHOT",
    organization := "ch.jodersky",
    licenses := Seq(("BSD New", url("http://opensource.org/licenses/BSD-3-Clause"))),
    scalacOptions ++= Seq("-deprecation", "-feature"),
    resolvers += Resolver.sonatypeRepo("releases"),
    ScalariformKeys.preferences := ScalariformKeys.preferences.value
      .setPreference(DoubleIndentClassDeclaration, true)
      .setPreference(DanglingCloseParenthesis, Preserve)
      .setPreference(MultilineScaladocCommentsStartOnFirstLine, true)
      .setPreference(PlaceScaladocAsterisksBeneathSecondAsterisk, true)
  )

  lazy val root = Project(
    id = "root",
    base = file("."),
    aggregate = Seq(
      macros, plugin
    ),
    settings = commonSettings ++ Seq(
      publish := {},
      publishLocal := {},
      // make sbt-pgp happy
      publishTo := Some(Resolver.file("Unused transient repository", target.value / "unusedrepo"))
    ) ++ addCommandAlias("test-plugin", ";+sbt-jni-macros/publishLocal;scripted")
  ).enablePlugins(CrossPerProjectPlugin)

  lazy val macros = Project(
    id = "sbt-jni-macros",
    base = file("macros"),
    settings = commonSettings ++ Seq(
      scalaVersion := scalaVersions.head,
      crossScalaVersions := scalaVersions,
      addCompilerPlugin("org.scalamacros" % "paradise" % macrosParadiseVersion cross CrossVersion.full),
      libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value
    )
  )

  lazy val plugin = Project(
    id = "sbt-jni",
    base = file("plugin"),
    settings = commonSettings ++ scriptedSettings ++ Seq(
      sbtPlugin := true,
      publishMavenStyle := false,
      scalaVersion := "2.10.6",
      crossScalaVersions := Seq(scalaVersion.value),

      libraryDependencies += "org.ow2.asm" % "asm" % "5.0.4",

      // make project settings available to source
      sourceGenerators in Compile += Def.task{
        val src = s"""|/* Generated by sbt */
                      |package ch.jodersky.sbt.jni
                      |
                      |private[jni] object ProjectVersion {
                      |  final val MacrosParadise = "${macrosParadiseVersion}"
                      |  final val Macros = "${version.value}"
                      |}
                      |""".stripMargin
        val file = sourceManaged.value / "ch" / "jodersky" / "sbt" / "jni" / "ProjectVersion.scala"
        IO.write(file, src)
        Seq(file)
      }.taskValue,
      scriptedLaunchOpts := Seq(
        "-Dplugin.version=" + version.value,
        "-XX:MaxPermSize=256m", "-Xmx2g", "-Xss2m"
      )
    )
  )

}
