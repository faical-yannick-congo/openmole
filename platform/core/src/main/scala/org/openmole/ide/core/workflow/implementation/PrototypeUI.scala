/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.workflow.implementation

import org.openmole.ide.core.control.MoleScenesManager
import org.openmole.ide.core.properties.PanelUI
import org.openmole.ide.core.properties.PanelUIData
import org.openmole.ide.core.palette.PaletteElementFactory
import org.openmole.ide.core.properties.IFactoryUI
import org.openmole.ide.core.properties.IPrototypeFactoryUI
import org.openmole.ide.core.workflow.model.IEntityUI
import org.openmole.ide.core.commons.Constants


//class PrototypeUI(var name: String, val coreClass: Class[_],val panel: PanelUI) extends IEntityUI{
 // def this(coreClass: Class[_],panel: PanelUI) = this(MoleScenesManager.incrementPrototypeName,coreClass,panel)
class PrototypeUI(factoryUI: IFactoryUI) extends EntityUI(factoryUI){
  
  EntitiesUI.entities(Constants.PROTOTYPE).register(this);
}

//
//public class PrototyeUI extends EntityUI{
//
//    public PrototyeUI() {
//    super();
//    }
//    
//    public PrototyeUI(String name, Class type) {
//        super(name,type);
//    }
//}