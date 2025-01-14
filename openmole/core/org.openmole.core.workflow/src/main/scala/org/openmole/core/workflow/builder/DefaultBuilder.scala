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
package org.openmole.core.workflow.builder

import org.openmole.core.workflow.data.{ Default, Prototype, DefaultSet }

trait DefaultBuilder <: Builder { builder ⇒
  private var _defaults = DefaultSet.empty
  def setDefault[T](p: Prototype[T], v: T, `override`: Boolean = false): this.type = setDefault(Default.value(p, v, `override`))
  def setDefault(p: Default[_]*): this.type = { _defaults ++= p; this }

  @deprecated("use setDefault instead", "4.0")
  def addParameter(p: Default[_]) = { _defaults += p; this }

  def defaults = _defaults

  trait Built {
    def defaults = builder.defaults
  }
}
