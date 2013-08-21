/*
 * Copyright (C) 2012 mathieu
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

package org.openmole.ide.plugin.task.stat

import org.openmole.ide.core.implementation.dataproxy.Proxies

class AverageTaskPanelUI(dataUI: AverageTaskDataUI) extends BasicStatPanelUI("average", dataUI) {

  def saveContent(name: String) = new AverageTaskDataUI(name,
    if (multiPrototypeCombo.isDefined)
      multiPrototypeCombo.get.content.map { c ⇒ (c.comboValue1.get, c.comboValue2.get) }.filter { case (p1, p2) ⇒ Proxies.check(p1) || Proxies.check(p2) }
    else List.empty)
}
