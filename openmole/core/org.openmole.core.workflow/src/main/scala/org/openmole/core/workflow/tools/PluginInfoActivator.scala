/*
 * Copyright (C) 2015 Romain Reuillon
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

package org.openmole.core.workflow.tools

import java.util.concurrent.ConcurrentHashMap
import collection.JavaConverters._

import org.osgi.framework.{ BundleContext, BundleActivator }

object PluginInfo {
  val plugins = new ConcurrentHashMap[Class[_], PluginInfo]().asScala
  def pluginsInfo = plugins.values
}

case class PluginInfo(namespaces: List[String], keywordTraits: List[String])

trait PluginInfoActivator extends BundleActivator {
  def keyWordTraits: List[Class[_]] = Nil
  def info = PluginInfo(List(this.getClass.getPackage.getName), keyWordTraits.map(_.getCanonicalName))

  override def start(bundleContext: BundleContext): Unit =
    PluginInfo.plugins += this.getClass -> info

  override def stop(bundleContext: BundleContext): Unit =
    PluginInfo.plugins -= this.getClass
}
