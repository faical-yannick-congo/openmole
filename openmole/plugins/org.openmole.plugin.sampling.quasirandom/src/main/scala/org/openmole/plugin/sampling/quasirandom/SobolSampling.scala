/*
 * Copyright (C) 2010 Romain Reuillon
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

package org.openmole.plugin.sampling.quasirandom

import org.apache.commons.math3.random.SobolSequenceGenerator
import org.openmole.core.tools.service.Scaling._
import org.openmole.core.workflow.task.Task
import Task._
import org.openmole.core.workflow.tools._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.tools.FromContext
import util.Random

object SobolSampling {

  def apply[D](samples: FromContext[Int], factors: Factor[D, Double]*)(implicit bounds: Bounds[D, Double], domainInputs: DomainInputs[D]) =
    new SobolSampling(samples, factors: _*)

}

sealed class SobolSampling[D](val samples: FromContext[Int], val factors: Factor[D, Double]*)(implicit bounds: Bounds[D, Double], domainInputs: DomainInputs[D]) extends Sampling {

  override def inputs = PrototypeSet(factors.flatMap(f ⇒ domainInputs.inputs(f.domain)))
  override def prototypes = factors.map { _.prototype }

  override def apply() = FromContext { (context, rng) ⇒
    val sequence = new SobolSequenceGenerator(factors.size)
    val s = samples.from(context)(rng)

    for {
      v ← Iterator.continually(sequence.nextVector()).take(s)
    } yield (factors zip v).map {
      case (f, v) ⇒
        Variable(
          f.prototype,
          v.scale(bounds.min(f.domain).from(context)(rng), bounds.max(f.domain).from(context)(rng))
        )
    }
  }
}
