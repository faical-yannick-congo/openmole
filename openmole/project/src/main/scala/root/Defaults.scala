package root

import org.openmole.buildsystem._
import OMKeys._

import sbt._
import Keys._

abstract class Defaults(subBuilds: Defaults*) extends BuildSystemDefaults {

  override def subProjects = subBuilds flatMap (_.projectRefs)

  val projectName = "openmole"

  def org = "org.openmole"

  override def settings = super.settings ++
    Seq(
      scalaVersion in Global := "2.11.7",
      scalacOptions ++= Seq("-target:jvm-1.7", "-language:higherKinds"),
      javacOptions in (Compile, compile) ++= Seq("-source", "1.7", "-target", "1.7"),
      publishArtifact in (packageDoc in install) := false,
      publishArtifact in (packageSrc in install) := false
    )
}
