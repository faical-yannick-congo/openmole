/*
 * Copyright (C) 2015 Romain Reuillon
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
package org.openmole.plugin.environment.ssh

import org.openmole.core.batch.control.LimitedAccess
import org.openmole.core.batch.environment.SimpleBatchEnvironment
import org.openmole.core.workspace.Workspace

trait ClusterEnvironment extends SimpleBatchEnvironment with SSHPersistentStorage {

  val usageControl =
    new LimitedAccess(
      Workspace.preferenceAsInt(SSHEnvironment.MaxConnections),
      Workspace.preferenceAsInt(SSHEnvironment.MaxOperationsByMinute)
    )

}
