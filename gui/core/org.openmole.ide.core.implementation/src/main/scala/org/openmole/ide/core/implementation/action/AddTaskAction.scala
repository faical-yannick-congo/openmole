/*
 * Copyright (C) 2011 Mathieu Mathieu Leclaire <mathieu.Mathieu Leclaire at openmole.org>
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

package org.openmole.ide.core.implementation.action

import org.openmole.ide.core.implementation.provider.GenericMenuProvider
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.ide.core.model.workflow.{ IBuildMoleScene, IMoleScene }
import scala.swing.Action
import org.openmole.ide.core.implementation.builder.SceneFactory
import org.openmole.ide.core.implementation.workflow.CapsuleUI
import org.openmole.ide.core.implementation.data.CapsuleDataUI

class AddTaskAction(moleScene: IBuildMoleScene,
                    dpu: ITaskDataProxyUI,
                    provider: GenericMenuProvider) extends Action(dpu.dataUI.name) {
  override def apply = {
    val capsule = CapsuleUI.withMenu(moleScene, new CapsuleDataUI(task = Some(dpu)))
    moleScene.add(capsule, provider.currentPoint)
    capsule.addInputSlot
    moleScene.refresh
  }
}