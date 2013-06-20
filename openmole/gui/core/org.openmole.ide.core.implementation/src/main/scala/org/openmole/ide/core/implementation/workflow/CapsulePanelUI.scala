/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
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
package org.openmole.ide.core.implementation.workflow

import org.openmole.ide.misc.widget.{ ContentAction, LinkLabel, PluginPanel }
import org.openmole.ide.core.model.panel.ICapsulePanelUI
import scala.swing._
import event.ButtonClicked
import org.openmole.ide.core.model.data.{ IEnvironmentDataUI, ICapsuleDataUI }
import org.openmole.ide.misc.widget.multirow.RowWidget._
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import org.openmole.ide.misc.widget.multirow.{ MultiComboLinkLabel, MultiCombo }
import org.openmole.ide.core.implementation.dataproxy.Proxies
import org.openmole.ide.misc.widget.multirow.MultiCombo.{ ComboData, ComboPanel }
import org.openmole.ide.core.implementation.data.{ EmptyDataUIs, CapsuleDataUI }
import org.openmole.ide.core.model.dataproxy.{ IDataProxyUI, IHookDataProxyUI, ISourceDataProxyUI, IEnvironmentDataProxyUI }
import java.awt.Color
import org.openmole.ide.core.implementation.execution.{ ScenesManager, GroupingStrategyPanelUI }
import org.openmole.ide.misc.widget.multirow.MultiComboLinkLabel.{ ComboLinkLabelData, ComboLinkLabelPanel }

class CapsulePanelUI(dataUI: ICapsuleDataUI, index: Int = 0) extends PluginPanel("") with ICapsulePanelUI {

  def sources = Proxies.instance.sources.toList
  def hooks = Proxies.instance.hooks.toList

  val sourcePanel = new MultiCombo("",
    sources,
    dataUI.sources.map { s ⇒
      new ComboPanel(sources, new ComboData(Some(s)))
    },
    CLOSE_IF_EMPTY,
    ADD)

  val hookPanel = new MultiCombo("",
    hooks,
    dataUI.hooks.map { h ⇒
      new ComboPanel(hooks, new ComboData(Some(h)))
    },
    CLOSE_IF_EMPTY,
    ADD)

  val environmentProxys = Proxies.instance.environments :+ EmptyDataUIs.emptyEnvironmentProxy
  val environmentCombo = new MyComboBox(environmentProxys)

  val groupingCheckBox = new CheckBox("Grouping") { foreground = Color.WHITE }
  val groupingPanel = new GroupingStrategyPanelUI(dataUI.grouping)

  dataUI.environment match {
    case Some(e: IEnvironmentDataProxyUI) ⇒ environmentCombo.selection.item = e
    case _                                ⇒ environmentCombo.selection.item = environmentProxys.last
  }

  groupingPanel.visible = dataUI.grouping.isDefined
  groupingCheckBox.selected = dataUI.grouping.isDefined

  val executionPanel = new PluginPanel("wrap") {
    contents += new PluginPanel("wrap 3") {
      contents += new Label("Environment") { foreground = Color.WHITE }
      contents += environmentCombo
      contents += new LinkLabel("", new Action("") {
        def apply =
          if (environmentCombo.selection.index != environmentProxys.size - 1) {
            ScenesManager.displayExtraPropertyPanel(environmentCombo.selection.item)
          }
      }) { icon = org.openmole.ide.misc.tools.image.Images.EYE }
    }
    contents += new PluginPanel("wrap") {
      contents += groupingCheckBox
      contents += groupingPanel
    }
  }

  val components = List(("Source", sourcePanel.panel), ("Hook", hookPanel.panel), ("Execution", executionPanel))

  listenTo(`groupingCheckBox`)
  reactions += {
    case ButtonClicked(`groupingCheckBox`) ⇒ groupingPanel.visible = groupingCheckBox.selected
  }

  def save =
    new CapsuleDataUI(dataUI.task,
      environmentCombo.selection.item.dataUI match {
        case e: EmptyDataUIs.EmptyEnvironmentDataUI ⇒ None
        case e: IEnvironmentDataUI                  ⇒ Some(environmentCombo.selection.item)
      },
      if (groupingCheckBox.selected) groupingPanel.save else None,
      sourcePanel.content.map { _.comboValue.get }.filter {
        _ match {
          case s: ISourceDataProxyUI ⇒ true
          case _                     ⇒ false
        }
      },
      hookPanel.content.map { _.comboValue.get }.filter {
        _ match {
          case s: IHookDataProxyUI ⇒ true
          case _                   ⇒ false
        }
      })
}