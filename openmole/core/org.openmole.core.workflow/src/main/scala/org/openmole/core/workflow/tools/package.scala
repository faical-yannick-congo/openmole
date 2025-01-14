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

package org.openmole.core.workflow

import scala.concurrent.stm._
import scala.ref.WeakReference

package tools {

    trait ToolsPackage {

      implicit def objectToSomeObjectConverter[T](v: T) = Some(v)
      implicit def objectToWeakReferenceConverter[T <: AnyRef](v: T) = new WeakReference[T](v)

      implicit class RefDecorator[T](r: Ref[T]) {
        def getUpdate(t: T ⇒ T): T = atomic { implicit txn ⇒ val v = r(); r() = t(v); v }
      }

      implicit class RefLongDecorator(r: Ref[Long]) {
        def next = r getUpdate (_ + 1)
      }

      type Condition = FromContext[Boolean]

    }
}

package object tools extends ToolsPackage
