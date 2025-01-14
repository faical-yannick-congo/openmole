/*
 * Copyright (C) 2012 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

package org.openmole.plugin.task.jvm

import java.io.File
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.pluginmanager.PluginManager
import org.openmole.core.serializer.plugin.Plugins
import org.openmole.plugin.task.external.ExternalTaskBuilder
import scala.collection.mutable.ListBuffer

trait JVMLanguageBuilder { builder ⇒
  private val _libraries = ListBuffer[File]()
  private val _plugins = ListBuffer[File]()

  def libraries = _libraries.toList
  def plugins = _plugins.toList

  /**
   * Add a library and make it available to the task
   *
   * For instance addLib("/tmp/malib.jar") in a groovy task make the content of the
   * jar available to the task. This method support jars but has some limitation. The
   * best way to use your own bytecode (java, scala, groovy, jython) in OpenMOLE is
   * building an OpenMOLE plugin (see the section on openmole.org for details).
   *
   * @param l a jar file
   *
   */
  def addLibrary(l: File*): this.type = {
    _libraries ++= l
    this
  }

  def addPlugins(plugin: Seq[File]*): this.type = {
    plugins.foreach {
      plugin ⇒
        PluginManager.bundle(plugin) match {
          case None ⇒ throw new UserBadDataError(s"Plugin $plugin is not loaded")
          case _    ⇒
        }
    }
    _plugins ++= plugin.flatten
    this
  }

  trait Built <: Plugins {
    def libraries: Seq[File] = builder.libraries.toList
    def plugins: Seq[File] = builder.plugins
  }
}

abstract class JVMLanguageTaskBuilder extends ExternalTaskBuilder with JVMLanguageBuilder {
  trait Built <: super[ExternalTaskBuilder].Built with super[JVMLanguageBuilder].Built
}