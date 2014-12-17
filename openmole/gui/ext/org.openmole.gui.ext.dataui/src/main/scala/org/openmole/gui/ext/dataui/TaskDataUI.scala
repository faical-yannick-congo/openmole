package org.openmole.gui.ext.dataui

import org.openmole.gui.ext.data.TaskData
import rx._
/*
 * Copyright (C) 10/08/14 // mathieu.leclaire@openmole.org
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

trait TaskDataUI <: DataUI {
  type DATA = TaskData
  def data: DATA
  def inputs: Var[Seq[Var[(PrototypeDataUI[_], Option[String])]]]
  def outputs: Var[Seq[Var[PrototypeDataUI[_]]]]
}