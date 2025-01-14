/*
 *  Copyright (C) 2010 Romain Reuillon <romain.Romain Reuillon at openmole.org>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 * 
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.task.external

import java.io.File
import org.openmole.core.exception.UserBadDataError
import org.openmole.tool.file._
import org.openmole.core.tools.service.OS
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.tools._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.tools.ExpandedString
import org.openmole.core.workspace.Workspace

import scala.util.Random

object ExternalTask {
  val PWD = Prototype[String]("PWD")

  case class InputFile(
    prototype: Prototype[File],
    destination: ExpandedString,
    link: Boolean)

  case class InputFileArray(
    prototype: Prototype[Array[File]],
    prefix: ExpandedString,
    suffix: ExpandedString,
    link: Boolean)

  case class OutputFile(
    origin: ExpandedString,
    prototype: Prototype[File])

  case class Resource(
    file: File,
    destination: ExpandedString,
    link: Boolean,
    os: OS)
}

import ExternalTask._

trait ExternalTask extends Task {

  def inputFileArrays: Iterable[InputFileArray]
  def inputFiles: Iterable[InputFile]
  def outputFiles: Iterable[OutputFile]
  def resources: Iterable[Resource]

  protected case class ToPut(file: File, name: String, link: Boolean)

  protected def listInputFiles(context: Context)(implicit rng: RandomProvider): Iterable[(Prototype[File], ToPut)] =
    inputFiles.map {
      case InputFile(prototype, name, link) ⇒ prototype -> ToPut(context(prototype), name.from(context), link)
    }

  protected def listInputFileArray(context: Context)(implicit rng: RandomProvider): Iterable[(Prototype[Array[File]], Seq[ToPut])] =
    for {
      ifa ← inputFileArrays
    } yield {
      (ifa.prototype,
        context(ifa.prototype).zipWithIndex.map {
          case (file, i) ⇒
            ToPut(file, s"${ifa.prefix.from(context)}$i${ifa.suffix.from(context)}", link = ifa.link)
        }.toSeq)
    }

  protected def listResources(context: Context, resolver: PathResolver)(implicit rng: RandomProvider): Iterable[ToPut] = {
    val byLocation =
      resources groupBy {
        case Resource(_, name, _, _) ⇒ resolver(name.from(context)).getCanonicalPath
      }

    val selectedOS =
      byLocation.toList flatMap {
        case (_, values) ⇒ values.find { _.os.compatible }
      }

    selectedOS.map {
      case Resource(file, name, link, _) ⇒ ToPut(file, name.from(context), link)
    }
  }

  type PathResolver = String ⇒ File

  def relativeResolver(workDirectory: File)(filePath: String): File = {
    def resolved = workDirectory.resolve(filePath)
    resolved.toFile
  }

  protected def outputFileVariables(context: Context, resolver: PathResolver)(implicit rng: RandomProvider) =
    outputFiles.map {
      case OutputFile(name, prototype) ⇒
        val fileName = name.from(context)
        Variable(prototype, resolver(fileName))
    }

  private def copy(f: ToPut, to: File) = {
    to.createParentDir

    if (f.link) to.createLink(f.file.getCanonicalFile)
    else {
      f.file.realFile.copy(to)
      to.applyRecursive { _.deleteOnExit }
    }

  }

  def prepareInputFiles(context: Context, resolver: PathResolver)(implicit rng: RandomProvider): Context = {
    def destination(f: ToPut) = resolver(f.name)

    for { f ← listResources(context, resolver) } copy(f, destination(f))

    val copiedFiles =
      for { (p, f) ← listInputFiles(context) } yield {
        val d = destination(f)
        copy(f, d)
        Variable(p, d)
      }

    val copiedArrayFiles =
      for { (p, fs) ← listInputFileArray(context) } yield {
        val copied =
          fs.map { f ⇒
            val d = destination(f)
            copy(f, d)
            d
          }
        Variable(p, copied.toArray)
      }

    context ++ copiedFiles ++ copiedArrayFiles
  }

  def fetchOutputFiles(context: Context, resolver: PathResolver)(implicit rng: RandomProvider): Context =
    context ++ outputFileVariables(context, resolver)

  def checkAndClean(context: Context, rootDir: File) = {
    lazy val contextFiles =
      filterOutput(context).values.map(_.value).collect { case f: File ⇒ f }

    for {
      f ← contextFiles
      if !f.exists
    } throw new UserBadDataError("Output file " + f.getAbsolutePath + " for task " + this.toString + " doesn't exist")

    rootDir.applyRecursive(f ⇒ f.delete, contextFiles)

    // This delete the dir only if it is empty
    rootDir.delete
  }

  def withWorkDir[T](executionContext: TaskExecutionContext)(f: File ⇒ T): T = {
    val tmpDir = executionContext.tmpDirectory.newDir("externalTask")
    val res =
      try f(tmpDir)
      catch {
        case e: Throwable ⇒
          tmpDir.recursiveDelete
          throw e
      }
    tmpDir.delete
    res
  }

}
