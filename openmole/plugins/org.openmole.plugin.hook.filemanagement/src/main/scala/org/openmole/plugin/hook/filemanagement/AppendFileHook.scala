/*
 *  Copyright (C) 2010 reuillon
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.hook.filemanagement

import org.openmole.core.implementation.hook.CapsuleExecutionHook
import org.openmole.core.implementation.task.Task
import org.openmole.core.implementation.tools.VariableExpansion
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.execution.IProgress
import org.openmole.core.model.capsule.IGenericCapsule
import org.openmole.core.model.data.IContext
import java.io.File
import org.openmole.misc.tools.io.FileUtil.copy
import java.io.FileInputStream
import java.io.FileOutputStream
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.misc.exception.UserBadDataError

/**
 * Appends a variable content to an existing file.
 * The content of toBeDumpedPrototype file is appended to the outputFile safely(
 * concurent accesses are treated).
 * In the case of directories, all the files of the original directory are append to the
 * files of the target one.
 */
class AppendFileHook(moleExecution: IMoleExecution, capsule: IGenericCapsule, toBeDumpedPrototype: IPrototype[File], outputFile: String) extends CapsuleExecutionHook(moleExecution, capsule) {
  
  override def process(moleJob: IMoleJob) = {
    import moleJob.context
    
    context.value(toBeDumpedPrototype) match {
      case Some(from) =>
    
        val to = new File(VariableExpansion.expandData(context,outputFile))
        if (!from.exists) throw new UserBadDataError("The file " + from + " does not exist.")

        if (!to.exists){
          if(from.isDirectory) to.mkdirs
          else {
            to.getParentFile.mkdirs
            to.createNewFile
          }
        }
    
        if (from.isDirectory && to.isDirectory){
          val toFiles = to.list
          from.list foreach ( f => {
              if (!toFiles.contains(f)) new File(f).createNewFile
              lockAndAppendFile(new File(from,f),new File(to,f))
            })
        }
        else if (from.isFile && to.isFile) lockAndAppendFile(from,to)
        else throw new UserBadDataError("The merge can only be done from a file to another or from a directory to another. ("+from.toString+" and "+to.toString+" found)")
      case None => throw new UserBadDataError("Variable not found " + toBeDumpedPrototype)
    }
  }
  
  def lockAndAppendFile(from: String, to: String): Unit = lockAndAppendFile(new File(from), new File(to))
  
  def lockAndAppendFile(from: File,to: File): Unit = {
    val channelI = new FileInputStream(from) getChannel()
    try{
      val channelO = new FileOutputStream(to,true) getChannel()
      try{
        val lock = channelO lock()
        try copy(channelI,channelO)
        finally lock release
      } finally channelO close
    } finally channelI close
  }
}

