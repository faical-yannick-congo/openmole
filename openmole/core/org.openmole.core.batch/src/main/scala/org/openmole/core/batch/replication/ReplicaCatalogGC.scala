/*
 * Copyright (C) 2010 reuillon
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.batch.replication

import org.openmole.core.batch.environment.AuthenticationRegistry
import org.openmole.misc.updater.IUpdatable

class ReplicaCatalogGC extends IUpdatable {

  override def update: Boolean = {
    for (replica <- ReplicaCatalog.allReplicas) {
      if(replica.authenticationKey == null) ReplicaCatalog.clean(replica)
      else  {    
        //May be the env plugin is not loaded in this case a version of the description class is persisted by db4o
        if (AuthenticationRegistry.isRegistred(replica.authenticationKey)) {
          if (!replica.sourceFile.exists)  ReplicaCatalog.clean(replica)
        }
      }
    }

    true
  }
}
