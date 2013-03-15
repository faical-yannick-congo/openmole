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
package org.openmole.ide.plugin.builder.base

import org.openmole.core.implementation.puzzle.Puzzle
import org.openmole.ide.core.model.workflow.IMoleSceneManager
import org.openmole.ide.core.model.builder.IPuzzleUIMap
import org.openmole.plugin.builder.base._
import org.openmole.ide.core.implementation.dataproxy.Proxys
import swing.{ Label, MyComboBox }

class AggregationBuilderPanelUI(puzzles: List[Puzzle],
                                puzzleSelection: Option[Puzzle],
                                manager: IMoleSceneManager) extends GenericBuilderPanelUI("Explore on", puzzles, puzzleSelection) {

  val aggregationPuzzleComboBox = new MyComboBox(puzzles)
  contents += new Label("Aggregates on")
  contents += aggregationPuzzleComboBox

  override def build(uiMap: IPuzzleUIMap) = {
    val samplingUI = Proxys.getOrGenerateSamplingComposition(samplingComboBox.selection.item)
    val sampling = samplingUI.dataUI.coreObject
    (aggregation(nameTextField.text, puzzleComboBox.selection.item, sampling, aggregationPuzzleComboBox.selection.item)(manager.dataUI.pluginSet), uiMap += (sampling, samplingUI))
  }
}