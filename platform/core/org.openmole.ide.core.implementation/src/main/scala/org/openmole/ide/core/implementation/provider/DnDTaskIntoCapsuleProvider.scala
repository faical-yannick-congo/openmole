/*
 * Copyright (C) 2011 Mathieu leclaire <mathieu.leclaire at openmole.org>
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

package org.openmole.ide.core.implementation.provider

import java.awt.Point
import java.awt.datatransfer.Transferable
import org.netbeans.api.visual.widget.Widget
import org.netbeans.api.visual.action.ConnectorState
import org.openmole.ide.core.model.workflow.ICapsuleUI
import scala.collection.JavaConversions
import org.openmole.ide.core.model.commons.IOType
import org.openmole.ide.core.model.commons.CapsuleType._
import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.implementation.display.Displays
import org.openmole.ide.core.implementation.workflow.MoleScene
import org.openmole.ide.misc.exception.GUIUserBadDataError
import org.openmole.ide.core.model.commons.Constants

class DnDTaskIntoCapsuleProvider(molescene: MoleScene,val capsule: ICapsuleUI) extends DnDProvider(molescene) {
  var encapsulated= false
  
  override def isAcceptable(widget: Widget, point: Point,transferable: Transferable)= { 

    println("ENTITY TYPE :: :: " + Displays.dataProxy.dataUI.entityType)
    Displays.dataProxy.dataUI.entityType match {
      case Constants.TASK=> if (!encapsulated) ConnectorState.ACCEPT else ConnectorState.REJECT
      case Constants.PROTOTYPE=> ConnectorState.ACCEPT
      case Constants.SAMPLING=> if (capsule.capsuleType == EXPLORATION_TASK) ConnectorState.ACCEPT else ConnectorState.REJECT
      case Constants.ENVIRONMENT=> if (encapsulated) ConnectorState.ACCEPT else ConnectorState.REJECT
      case _=> throw new GUIUserBadDataError("Unknown entity type")
    }
  }
  
  override def accept(widget: Widget,point: Point,transferable: Transferable)= { 
    println("DISPLAY :: " + Displays.dataProxy)
      Displays.dataProxy match {
      case dpu:ITaskDataProxyUI => capsule.encapsule(dpu)
      case dpu:IPrototypeDataProxyUI=> { 
          if (point.x < capsule.connectableWidget.widgetWidth / 2) capsuleDataUI.addPrototype(dpu, IOType.INPUT)
          else capsuleDataUI.addPrototype(dpu, IOType.OUTPUT)
        }
      case dpu:ISamplingDataProxyUI=> capsuleDataUI.sampling = Some(dpu)
      case dpu:IEnvironmentDataProxyUI=> capsuleDataUI.environment = Some(dpu)
    }
    
    molescene.repaint
    molescene.revalidate
  }
  
  private def capsuleDataUI = capsule.dataProxy.get.dataUI
}