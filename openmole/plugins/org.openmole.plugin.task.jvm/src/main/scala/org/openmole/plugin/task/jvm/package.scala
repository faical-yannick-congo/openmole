/*
 * Copyright (C) 2012 Romain Reuillon
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.task

import java.io.File

import org.openmole.core.macros.Keyword._
import org.openmole.core.workflow.builder._

package jvm {

  import org.openmole.core.pluginmanager.PluginManager

  trait JVMPackage extends external.ExternalPackage {
    lazy val libraries = add[{ def addLibrary(l: File*) }]
    lazy val plugins = add[{ def addPlugins(plugins: Seq[File]*) }]
    def pluginsOf(o: Any): Seq[File] = pluginsOf(o.getClass)
    def pluginsOf[T](implicit m: Manifest[T]): Seq[File] = pluginsOf(manifest[T].runtimeClass)
    def pluginsOf(clazz: Class[_]): Seq[File] = PluginManager.pluginsForClass(clazz).toSeq
  }
}

package object jvm extends JVMPackage