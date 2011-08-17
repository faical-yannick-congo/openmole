/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.plugin.sampling.csv

import scala.swing._
import scala.swing.Label
import swing.Swing._
import scala.swing.event.ButtonClicked
import scala.swing.event.EditDone
import swing.ListView._
import scala.swing.TextField
import au.com.bytecode.opencsv.CSVReader
import java.io.File
import java.io.FileReader
import javax.swing.DefaultCellEditor
import scala.swing.Table.ElementMode._
import org.openmole.ide.core.model.display._
import org.openmole.ide.core.model.panel.ISamplingPanelUI
import javax.swing.table.DefaultTableModel
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.implementation.dataproxy.PrototypeDataProxyUI
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.implementation.data.EmptyDataUIs._
import org.openmole.ide.misc.widget.CSVChooseFileTextField
import org.openmole.ide.misc.widget.DialogClosedEvent
import scala.swing.BorderPanel.Position._

class CSVSamplingPanelUI(pud: CSVSamplingDataUI) extends BoxPanel(Orientation.Vertical) with ISamplingPanelUI {
  
  val csvTextField = new CSVChooseFileTextField(pud.csvFilePath)
  val pathFileLabel = new Label("CSV file :") {
    preferredSize = new Dimension(80,25)
    border = Swing.EmptyBorder(5,5,5,5)
  }
  
  val filePanel = new BoxPanel(Orientation.Horizontal) {
    contents.append(pathFileLabel,csvTextField)
    border = Swing.EmptyBorder(5,15,5,15)
  }
  
  listenTo(csvTextField)
  reactions += {
    case DialogClosedEvent(csvTextField)=> { 
        if (isFile(csvTextField.text)){
        val reader = new CSVReader(new FileReader(csvTextField.text))
        val headers = reader.readNext
        model.setRowCount(headers.length)
        headers.zipWithIndex.foreach{case (h,i)=> table.update(i,0,h); table.update(i,1,new PrototypeDataProxyUI(new EmptyPrototypeDataUI("")))}
        reader.close}}}
  
  val model = new DefaultTableModel(Array[Object]("CSV headers","Prototypes"),0)
  val table = buildTable(model)
  
  contents.append(filePanel,new ScrollPane(table))
  border = Swing.EmptyBorder(10, 10, 10, 10)
  
  // Load data
  if (isFile(pud.csvFilePath)) csvTextField.text = pud.csvFilePath
  model.setRowCount(pud.prototypeMapping.size)
  
  var i = 0
  pud.prototypeMapping.foreach{d => table.update(i,0,d._1);
                               table.update(i,1,comboContent.zipWithIndex.filter(_._1.dataUI.name.equals(d._2.dataUI.name))(0)._1);
                               i+=1}
  
  def isFile(s: String) = new File(s).isFile
  
  def buildTable(m: DefaultTableModel) = 
    new Table{
      model = m
      preferredSize = new Dimension(100,100)
      selection.elementMode = Cell
      rowHeight= 30
      
      
      override protected def editor(row: Int, column: Int) = {
        column match {
          case 1=> new DefaultCellEditor(new ComboBox(comboContent).peer)
          case _=> new DefaultCellEditor(new TextField{editable = false}.peer)
        }
      }
    }
  
  
  override def saveContent(name: String) = {
    var protoMapping = Map[String,PrototypeDataProxyUI]()   
    for(i<- 0 to model.getRowCount-1) {println("SAVE :: "+model.getValueAt(i,1).asInstanceOf[PrototypeDataProxyUI].dataUI.getClass);protoMapping += model.getValueAt(i,0).toString -> model.getValueAt(i,1).asInstanceOf[PrototypeDataProxyUI]}
    new CSVSamplingDataUI(name,csvTextField.text,protoMapping)
  }
  
  def comboContent: List[IPrototypeDataProxyUI] = new PrototypeDataProxyUI(new EmptyPrototypeDataUI(""))::Proxys.prototype.toList
}
