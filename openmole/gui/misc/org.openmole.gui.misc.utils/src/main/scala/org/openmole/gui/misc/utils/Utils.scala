package org.openmole.gui.misc.utils

/*
 * Copyright (C) 22/04/15 // mathieu.leclaire@openmole.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import rx._

import scala.scalajs.js.Date

object Utils {
  implicit def tToVarT[T](t: T): Var[T] = Var(t)
  def getUUID: String = java.util.UUID.randomUUID.toString
  def toURI(path: Seq[String]): String = new java.net.URI(null, null, path.mkString("/"), null).toString

  def longToDate(date: Long) = s"${new Date(date).toLocaleDateString}, ${new Date(date).toLocaleTimeString}"
}
