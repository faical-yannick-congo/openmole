package org.openmole.gui.ext.data

/*
 * Copyright (C) 13/02/15 // mathieu.leclaire@openmole.org
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

import scala.util.Try

trait Factory

trait CoreObjectFactory extends Factory {
  def coreObject(): Try[Any]
}

trait AuthenticationFactory extends Factory {
  def coreObject(data: AuthenticationData): Option[Any]
  def buildAuthentication(data: AuthenticationData): Unit
  def allAuthenticationData: Seq[AuthenticationData]
  def removeAuthentication(data: AuthenticationData): Unit
}
