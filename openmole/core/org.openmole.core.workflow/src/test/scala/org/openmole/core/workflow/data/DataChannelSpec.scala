/*
 * Copyright (C) 2011 Romain Reuillon
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

package org.openmole.core.workflow.data

import java.util.concurrent.locks.ReentrantLock

import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.transition._
import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.transition._
import org.openmole.core.workflow.puzzle._

import org.scalatest._
import org.scalatest.junit._
import scala.collection.mutable.ListBuffer

class DataChannelSpec extends FlatSpec with Matchers {

  "A datachannel" should "enable variable values to be transmitted from a task to another" in {
    val p = Prototype[String]("p")

    val t1 = TestTask { _ + (p -> "Test") }
    t1 setName "Test write"
    t1 addOutput p

    val t2 = EmptyTask()

    val t3 =
      TestTask { context ⇒
        context(p) should equal("Test")
        context
      }
    t3 setName "Test read"
    t3 addInput p

    val t1c = Capsule(t1)
    val t2c = Capsule(t2)
    val t3c = Slot(Capsule(t3))

    val ex = (t1c -- t2c -- t3c) & (t1c oo t3c)

    ex.start.waitUntilEnded
  }

  "A data channel" should "be able to transmit the value to the multiple execution of an explored task" in {

    val j = Prototype[String]("j")
    val tw = TestTask { _ + (j -> "J") }
    tw setName "Test write"
    tw addOutput j

    val data = List("A", "B", "C")
    val i = Prototype[String]("i")

    val sampling = new ExplicitSampling(i, data)

    val exc = Capsule(ExplorationTask(sampling))

    val res = new ListBuffer[String]

    val t = TestTask { context ⇒
      res.synchronized {
        context.contains(i) should equal(true)
        context.contains(j) should equal(true)
        res += context(i)
      }
      context
    }
    t setName "Test"
    t addInput (i, j)

    val twc = Capsule(tw)
    val tc = Slot(t)

    val ex = (twc -- exc -< tc) & (twc oo tc)
    ex.start.waitUntilEnded
    res.toArray.sorted.deep should equal(data.toArray.deep)
  }

}
