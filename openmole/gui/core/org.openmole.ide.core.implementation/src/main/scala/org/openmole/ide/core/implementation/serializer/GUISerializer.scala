/*
 * Copyright (C) 2011 Mathieu Mathieu Leclaire <mathieu.Mathieu Leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.core.implementation.serializer

import util.{ Failure, Success, Try }
import com.ice.tar.TarInputStream
import com.ice.tar.TarOutputStream
import com.thoughtworks.xstream.XStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileReader
import java.io.FileWriter
import org.openmole.ide.misc.tools.util.ID
import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.implementation.dataproxy._
import java.io.ObjectInputStream
import java.nio.file.Files
import org.openmole.ide.core.implementation.workflow.BuildMoleScene
import org.openmole.ide.core.implementation.workflow.MoleScene
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.misc.workspace.Workspace
import org.openmole.misc.tools.io.TarArchiver._
import com.thoughtworks.xstream.io.{ HierarchicalStreamReader, HierarchicalStreamWriter }
import org.openmole.misc.exception.{ UserBadDataError, ExceptionUtils, InternalProcessingError }
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter
import com.thoughtworks.xstream.converters.{ UnmarshallingContext, MarshallingContext }
import collection.mutable
import org.openmole.ide.core.model.workflow.{ IMoleUI, IMoleScene }
import org.openmole.ide.core.model.data.{ ICapsuleDataUI }

class GUISerializer { serializer ⇒

  sealed trait SerializationState
  case class Serializing(id: ID.Type) extends SerializationState
  case class Serialized(id: ID.Type) extends SerializationState

  val serializationStates: mutable.HashMap[IDataProxyUI, SerializationState] = mutable.HashMap.empty
  val deserializationStates: mutable.HashMap[ID.Type, IDataProxyUI] = mutable.HashMap.empty

  val xstream = new XStream
  val workDir = Workspace.newDir

  class GUIConverter extends ReflectionConverter(xstream.getMapper, xstream.getReflectionProvider) {
    override def marshal(
      o: Object,
      writer: HierarchicalStreamWriter,
      mc: MarshallingContext) = {
      val dataUI = o.asInstanceOf[IDataProxyUI]
      serializationStates.get(dataUI) match {
        case None ⇒
          serializationStates += dataUI -> new Serializing(dataUI.id)
          marshal(o, writer, mc)
        case Some(Serializing(id)) ⇒
          serializationStates(dataUI) = new Serialized(id)
          super.marshal(dataUI, writer, mc)
        case Some(Serialized(id)) ⇒
          writer.addAttribute("id", id.toString)
      }
    }

    override def unmarshal(
      reader: HierarchicalStreamReader,
      uc: UnmarshallingContext) = {
      if (reader.getAttributeCount != 0) {
        val dui = existing(reader.getAttribute("id"))
        dui match {
          case Some(y: IDataProxyUI) ⇒ y
          case _ ⇒
            serializer.deserializeConcept(uc.getRequiredType)
            unmarshal(reader, uc)
        }
      }
      else {
        val o = super.unmarshal(reader, uc)
        o match {
          case y: IDataProxyUI ⇒
            existing(y.id) match {
              case None ⇒ add(y)
              case _    ⇒
            }
            y
          case _ ⇒ throw new UserBadDataError("Can't load object " + o)
        }
      }
    }

    def existing(id: String) = deserializationStates.get(id)
    def add(e: IDataProxyUI) = deserializationStates.put(e.id, e)

  }

  val taskConverter = new GUIConverter {
    override def canConvert(t: Class[_]) = classOf[ITaskDataProxyUI].isAssignableFrom(t)
  }

  val prototypeConverter = new GUIConverter {
    override def canConvert(t: Class[_]) = classOf[IPrototypeDataProxyUI].isAssignableFrom(t)
  }

  val samplingConverter = new GUIConverter {
    override def canConvert(t: Class[_]) = classOf[ISamplingCompositionDataProxyUI].isAssignableFrom(t)
  }

  val environmentConverter = new GUIConverter {
    override def canConvert(t: Class[_]) = classOf[IEnvironmentDataProxyUI].isAssignableFrom(t)
  }

  val hookConverter = new GUIConverter {
    override def canConvert(t: Class[_]) = classOf[IHookDataProxyUI].isAssignableFrom(t)
  }

  val sourceConverter = new GUIConverter {
    override def canConvert(t: Class[_]) = classOf[ISourceDataProxyUI].isAssignableFrom(t)
  }

  xstream.registerConverter(taskConverter)
  xstream.registerConverter(prototypeConverter)
  xstream.registerConverter(samplingConverter)
  xstream.registerConverter(environmentConverter)
  xstream.registerConverter(hookConverter)
  xstream.registerConverter(sourceConverter)

  //xstream.registerConverter(new MoleSceneConverter(this))

  /*xstream.alias(moleScene, classOf[IMoleScene])
  xstream.alias(task, classOf[ITaskDataProxyUI])
  xstream.alias(sampling, classOf[ISamplingCompositionDataProxyUI])
  xstream.alias(prototype, classOf[IPrototypeDataProxyUI])
  xstream.alias(environment, classOf[IEnvironmentDataProxyUI])
  xstream.alias(hook, classOf[IHookDataProxyUI])
  xstream.alias(source, classOf[ISourceDataProxyUI]) */

  def folder(clazz: Class[_]) =
    clazz match {
      case c if c == classOf[IPrototypeDataProxyUI] ⇒ "prototype"
      case c if c == classOf[IEnvironmentDataProxyUI] ⇒ "environment"
      case c if c == classOf[ISamplingCompositionDataProxyUI] ⇒ "sampling"
      case c if c == classOf[IHookDataProxyUI] ⇒ "hook"
      case c if c == classOf[ISourceDataProxyUI] ⇒ "source"
      case c if c == classOf[ITaskDataProxyUI] ⇒ "task"
      case c if c == classOf[MoleData] ⇒ "mole"
      case c ⇒ c.getSimpleName
    }

  def serializeConcept(clazz: Class[_], set: Iterable[(_, ID.Type)]) = {
    val conceptDir = new File(workDir, folder(clazz))
    conceptDir.mkdirs
    set.foreach {
      case (s, id) ⇒
        new File(conceptDir, id + ".xml").withWriter {
          xstream.toXML(s, _)
        }
    }
  }

  def serialize(file: File, proxies: Proxies, moleScenes: Iterable[MoleData]) = {
    serializeConcept(classOf[IPrototypeDataProxyUI], proxies.prototypes.map { s ⇒ s -> s.id })
    serializeConcept(classOf[IEnvironmentDataProxyUI], proxies.environments.map { s ⇒ s -> s.id })
    serializeConcept(classOf[ISamplingCompositionDataProxyUI], proxies.samplings.map { s ⇒ s -> s.id })
    serializeConcept(classOf[IHookDataProxyUI], proxies.hooks.map { s ⇒ s -> s.id })
    serializeConcept(classOf[ISourceDataProxyUI], proxies.sources.map { s ⇒ s -> s.id })
    serializeConcept(classOf[ITaskDataProxyUI], proxies.tasks.map { s ⇒ s -> s.id })
    serializeConcept(classOf[MoleData], moleScenes.map { ms ⇒ ms -> ms.id })
    val os = new TarOutputStream(new FileOutputStream(file))
    try os.createDirArchiveWithRelativePath(workDir)
    finally os.close
    clear
  }

  def read(f: File) = {
    try xstream.fromXML(f)
    catch {
      case e: Throwable ⇒
        throw new InternalProcessingError(e, "An error occurred when loading " + f.getAbsolutePath + "\n")
    }
  }

  def deserializeConcept[T](clazz: Class[_]) =
    new File(workDir, folder(clazz)).listFiles.toList.map(read).map(_.asInstanceOf[T])

  def deserialize(fromFile: String) = {
    val os = new TarInputStream(new FileInputStream(fromFile))
    os.extractDirArchiveWithRelativePathAndClose(workDir)

    val proxies: Proxies = new Proxies

    Try {
      deserializeConcept[IPrototypeDataProxyUI](classOf[IPrototypeDataProxyUI]).foreach(proxies.+=)
      deserializeConcept[ISamplingCompositionDataProxyUI](classOf[ISamplingCompositionDataProxyUI]).foreach(proxies.+=)
      deserializeConcept[IEnvironmentDataProxyUI](classOf[IEnvironmentDataProxyUI]).foreach(proxies.+=)
      deserializeConcept[IHookDataProxyUI](classOf[IHookDataProxyUI]).foreach(proxies.+=)
      deserializeConcept[ISourceDataProxyUI](classOf[ISourceDataProxyUI]).foreach(proxies.+=)
      deserializeConcept[ITaskDataProxyUI](classOf[ITaskDataProxyUI]).foreach(proxies.+=)

      val moleScenes = deserializeConcept[MoleData](classOf[MoleData])
      (proxies, moleScenes)
    }
  }

  def clear = {
    serializationStates.clear
    deserializationStates.clear
    workDir.recursiveDelete
    workDir.mkdirs
  }
}