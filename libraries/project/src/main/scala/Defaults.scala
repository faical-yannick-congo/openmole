package root

import org.openmole.buildsystem._
import OMKeys._

import sbt._
import Keys._
/**
 * Created with IntelliJ IDEA.
 * User: luft
 * Date: 3/17/13
 * Time: 6:43 PM
 * To change this template use File | Settings | File Templates.
 */
abstract class Defaults(subBuilds: Defaults*) extends BuildSystemDefaults {

  override def subProjects = subBuilds flatMap (_.projectRefs)

  val projectName = "openmole"

  def org = "org.openmole"

  override def settings = super.settings ++
    Seq(
      scalaVersion in Global := "2.11.7",
      scalacOptions ++= Seq("-deprecation"),
      publishArtifact in (packageDoc in install) := false,
      publishArtifact in (packageSrc in install) := false,
      resolvers += Resolver.sonatypeRepo("snapshots")
    )
}
