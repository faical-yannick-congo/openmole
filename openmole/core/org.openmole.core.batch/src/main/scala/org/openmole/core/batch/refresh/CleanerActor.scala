/*
 * Copyright (C) 2012 reuillon
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

package org.openmole.core.batch.refresh

import org.openmole.core.workspace.Workspace
import org.openmole.core.batch.environment.BatchEnvironment
import org.openmole.tool.logger.Logger

object CleanerActor extends Logger

import CleanerActor.Log._

class CleanerActor(jobManager: JobManager) {
  def receive(msg: CleanSerializedJob) = withRunFinalization {
    val CleanSerializedJob(sj) = msg
    try
      sj.synchronized {
        if (!sj.cleaned) sj.storage.tryWithToken {
          case Some(t) ⇒
            sj.storage.rmDir(sj.path)(t)
            sj.cleaned = true
          case None ⇒
            jobManager ! Delay(msg, BatchEnvironment.getTokenInterval)
        }

      }
    catch {
      case t: Throwable ⇒
        logger.log(FINE, "Error when deleting a file", t)
    }
  }
}
