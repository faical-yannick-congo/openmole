/*
 * Copyright (C) 2014 Romain Reuillon
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

package org.openmole.plugin.method.evolution

import org.openmole.core.workflow.data.{ Prototype, _ }
import org.openmole.core.workflow.tools.ExpandedString
import org.openmole.plugin.hook.file.AppendToCSVFileHookBuilder

object SavePopulationHook {

  def apply[T](algorithm: T, dir: ExpandedString)(implicit wfi: WorkflowIntegration[T]) = {
    val t = wfi(algorithm)

    val fileName = dir + "/population${" + t.generationPrototype.name + "}.csv"
    val prototypes = Seq[Prototype[_]](t.generationPrototype) ++ t.resultPrototypes.map(_.toArray)
    new AppendToCSVFileHookBuilder(fileName, prototypes: _*)
  }

}
