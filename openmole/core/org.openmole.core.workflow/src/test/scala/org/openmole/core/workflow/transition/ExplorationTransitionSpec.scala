/*
 * Copyright (C) 2011 Romain Reuillon
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

package org.openmole.core.workflow.transition

import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.task._
import org.scalatest._
import scala.collection.mutable.ListBuffer
import org.openmole.core.workflow.puzzle._

class ExplorationTransitionSpec extends FlatSpec with Matchers {

  "Exploration transition" should "submit one MoleJob for each value in the sampling" in {

    val data = List("A", "B", "C")
    val i = Prototype[String]("i")

    val sampling = new ExplicitSampling(i, data)

    val exc = Capsule(ExplorationTask(sampling))

    val res = new ListBuffer[String]

    val t = TestTask { context ⇒
      res.synchronized {
        context.contains(i) should equal(true)
        res += context(i)
        context
      }
    }
    t setName "Test"
    t addInput i

    val ex = exc -< t
    ex.start.waitUntilEnded
    res.toArray.sorted.deep should equal(data.toArray.deep)
  }

  "Exploration transition" should "work with the DSL interface" in {

    println("Test")

    val data = List("A", "B", "C")
    val i = Prototype[String]("i")

    val explo = ExplorationTask(new ExplicitSampling(i, data))

    val res = new ListBuffer[String]

    val t = TestTask { context ⇒
      res.synchronized {
        context.contains(i) should equal(true)
        res += context(i)
        context
      }
    }
    t setName "Test"
    t addInput i

    (explo -< t).start.waitUntilEnded
    res.toArray.sorted.deep should equal(data.toArray.deep)
  }
}
