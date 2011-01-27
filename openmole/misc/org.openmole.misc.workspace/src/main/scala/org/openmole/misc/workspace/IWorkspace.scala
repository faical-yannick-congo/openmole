/*
 * Copyright (C) 2010 reuillon
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

package org.openmole.misc.workspace

import java.io.File
import java.util.UUID

object IWorkspace {
  val OpenMoleDir = ".openmole"
  val ConfigurationFile = ".preferences"
  val GlobalGroup = "Global"
  val DefaultTmpLocation = ".tmp"
  val running = ".running"
  val UniqueID = new ConfigurationLocation(GlobalGroup, "UniqueID")

  lazy val defaultLocation = new File(System.getProperty("user.home"), OpenMoleDir)
  def isAlreadyRunningAt(location: File) = new File(location, running).exists
}

trait IWorkspace {

  def sessionUUID: UUID
  
  def location_=(location: File)
  def location: File

  def newDir(prefix: String): File
  def newDir: File
  def newFile(prefix: String, suffix: String): File
  def newFile: File

  def file(name: String): File

  def preference(location: ConfigurationLocation): String 

  def preferenceAsInt(location: ConfigurationLocation): Int

  def preferenceAsLong(location: ConfigurationLocation): Long

  def preferenceAsDouble(location: ConfigurationLocation): Double

  def preferenceAsDurationInMs(location: ConfigurationLocation): Long

  def preferenceAsDurationInS(location: ConfigurationLocation): Int

  def setPreference(configurationLocation: ConfigurationLocation, value: String)

  def removePreference(configurationElement: ConfigurationLocation)

  def password_=(password: String)
  
  def passwordIsCorrect: Boolean
  
  def passwordChoosen: Boolean

  def reset

  def isPreferenceSet(location: ConfigurationLocation): Boolean
        
  def +=(location: ConfigurationLocation, defaultValue : () => String)
  def +=(location: ConfigurationLocation, defaultValue : String)
  def defaultValue(location: ConfigurationLocation): String
  
}
