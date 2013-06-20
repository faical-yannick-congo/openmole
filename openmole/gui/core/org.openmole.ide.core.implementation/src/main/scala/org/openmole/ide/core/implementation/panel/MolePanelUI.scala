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

package org.openmole.ide.core.implementation.panel

import java.awt.Dimension
import org.openmole.ide.core.model.panel.IPanelUI
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.misc.widget.multirow.MultiChooseFileTextField
import org.openmole.ide.misc.widget.multirow.MultiChooseFileTextField._
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import scala.swing.RadioButton
import scala.swing.FileChooser.SelectionMode._
import org.openmole.misc.workspace.Workspace
import org.openmole.ide.core.model.workflow.IMoleUI
import org.openmole.ide.core.implementation.workflow.MoleUI

class MolePanelUI(mdu: IMoleUI) extends PluginPanel("wrap") with IPanelUI {
  minimumSize = new Dimension(300, 400)
  preferredSize = new Dimension(300, 400)
  Workspace.pluginDirLocation.list.foreach { f ⇒
    contents += new RadioButton(f) { selected = mdu.plugins.toList.contains(f) }
  }

  val components = List()

  def saveContent(name: String) =
    mdu.plugins =
      contents.flatMap { c ⇒
        c match {
          case x: RadioButton ⇒ List(x)
          case _              ⇒ Nil
        }
      }.toList.filter { _.selected }.map { _.text }

}