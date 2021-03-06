import sbt._
import sbt.Keys._

object ScriptedHelper extends AutoPlugin {

  override def requires = empty
  override def trigger = allRequirements

  override def projectSettings = Seq(
    crossScalaVersions := Seq("2.11.8", "2.12.0-M4"),
    scalaVersion := crossScalaVersions.value.head
  )

}
