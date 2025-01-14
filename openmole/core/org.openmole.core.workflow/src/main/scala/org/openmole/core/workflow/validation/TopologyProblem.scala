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

package org.openmole.core.workflow.validation

import org.openmole.core.workflow.data.DataChannel
import org.openmole.core.workflow.mole.Capsule
import org.openmole.core.workflow.transition.ITransition

object TopologyProblem {

  case class DuplicatedTransition(transitions: Iterable[ITransition]) extends TopologyProblem {
    override def toString = "DuplicatedTransition: from " + transitions.head.start + " to " + transitions.head.end.capsule + " has been found " + transitions.size + " times."
  }

  case class LevelProblem(
      capsule: Capsule,
      paths: List[(List[Capsule], Int)]) extends TopologyProblem {
    override def toString = "LevelProblem: " + capsule + ", " + paths.map { case (p, l) ⇒ "Folowing the path (" + p.mkString(", ") + " has level " + l + ")" }.mkString(", ")
  }

  case class NegativeLevelProblem(
      capsule: Capsule,
      path: List[Capsule],
      level: Int) extends TopologyProblem {

    override def toString = "LevelProblem: " + capsule + ", " + path.mkString(", ") + " has a negative level " + level
  }

  case class DataChannelNegativeLevelProblem(dataChannel: DataChannel) extends TopologyProblem {
    override def toString = "DataChannelNegativeLevelProblem: " + dataChannel + ", links a capsule of upper level to lower level, this is not supported, use aggregation transitions."
  }

  case class NoTransitionToCapsuleProblem(capsule: Capsule, dataChannel: DataChannel) extends TopologyProblem {
    override def toString = s"NoTransitionToCapsuleProblem: $capsule is linked with $dataChannel but not with any transition"
  }
}

trait TopologyProblem extends Problem