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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.tool.pattern

import org.openmole.core.workflow.data._
import org.openmole.core.workflow.puzzle._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.transition._
import org.openmole.core.workflow.validation.TypeUtil

object Strain {

  def apply(task: Task) = Capsule(task, strain = true)

  def apply(puzzle: Puzzle): Puzzle = {
    val first = Capsule(EmptyTask(), strain = true)
    val last = Slot(Capsule(EmptyTask(), strain = true))

    val _puzzle = first -- puzzle -- last
    val outputs = TypeUtil.receivedTypes(_puzzle.toMole, _puzzle.sources, _puzzle.hooks)(last)

    _puzzle & (first oo (last, filter = Block(outputs.toSeq: _*)))
  }

}