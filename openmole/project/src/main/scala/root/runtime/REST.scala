/*
 * Copyright (C) 2015 Mark Hammons, Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package root.runtime

import root.{ Defaults, _ }
import sbt.Keys._
import sbt._
import org.openmole.buildsystem.OMKeys._

object REST extends Defaults {
  import Libraries._
  import ThirdParties._

  val dir = file("runtime/rest")
  implicit val artifactPrefix = Some("org.openmole.rest")

  lazy val message = Project("org-openmole-rest-message", dir / "message") settings (scalariformDefaults: _*)

  lazy val server = OsgiProject(
    "server",
    privatePackages = Seq("org.openmole.rest.message.*"),
    imports = Seq("org.h2", "!com.sun.*", "*")
  ) dependsOn
    (Core.workflow, openmoleTar, openmoleCollection, root.Runtime.console, message, openmoleCrypto) settings
    (libraryDependencies ++= Seq(bouncyCastle, logback, scalatra, scalaLang, arm, codec))

  lazy val client = Project("org-openmole-rest-client", dir / "client") settings (
    libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.3.5",
    libraryDependencies += "org.apache.httpcomponents" % "httpmime" % "4.3.5",
    libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.2.10"
  ) dependsOn (message, openmoleTar) settings (scalariformDefaults: _*)

}
