package org.openmole.gui.client.core

/*
 * Copyright (C) 23/07/15 // mathieu.leclaire@openmole.org
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

import org.openmole.core.buildinfo.{MarketIndex, MarketIndexEntry}
import org.openmole.gui.client.core.AbsolutePositioning.CenterTransform
import fr.iscpif.scaladget.api.{BootstrapTags ⇒ bs}
import org.openmole.gui.ext.data.{Processing, ProcessState}
import org.openmole.gui.misc.js.{InputFilter}
import org.openmole.gui.misc.js.JsRxTags._
import org.openmole.gui.shared.Api
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import org.openmole.gui.client.core.files.treenodemanager.{instance ⇒ manager}
import org.openmole.gui.client.core.CoreUtils._
import org.openmole.gui.ext.data._
import Waiter._
import autowire._
import rx._
import scalatags.JsDom.{tags ⇒ tags}
import scalatags.JsDom.all._
import bs._

class MarketPanel extends ModalPanel {
  lazy val modalID = "marketPanelID"

  private val marketIndex: Var[Option[MarketIndex]] = Var(None)
  val tagFilter = InputFilter(pHolder = "Filter")
  val selectedEntry: Var[Option[MarketIndexEntry]] = Var(None)
  lazy val downloading: Var[Seq[(MarketIndexEntry, Var[_ <: ProcessState])]] = Var(marketIndex().map {
    _.entries.map {
      (_, Var(Processed()))
    }
  }.getOrElse(Seq()))
  val overwriteAlert: Var[Option[MarketIndexEntry]] = Var(None)

  lazy val marketTable = tags.div(`class` := "spacer20",
    Rx {
      marketIndex().map { mindex ⇒
        for {
          entry ← mindex.entries if tagFilter.exists(entry.tags)
        } yield {
          val isSelected = Some(entry) == selectedEntry()
          Seq(
            bs.div("docEntry")(
              bs.div(bs.col_md_3 + " spacer7")(
                tags.a(entry.name, cursor := "pointer", `class` := "whiteBold", onclick := { () ⇒
                  selectedEntry() = {
                    if (isSelected) None
                    else Some(entry)
                  }
                })),
              bs.div(bs.col_md_2)(downloadButton(entry, () ⇒ {
                OMPost[Api].exists(manager.current.safePath() ++ entry.name).call().foreach { b ⇒
                  if (b) overwriteAlert() = Some(entry)
                  else download(entry)
                }
              })),
              bs.div(bs.col_md_7 + " spacer7")(
                entry.tags.map { e ⇒ bs.label(e, label_primary + "marketTag") }
              ), tags.div(
                `class` := {
                  if (isSelected) "docEntry" else ""
                },
                selectedEntry().map { se ⇒
                  if (isSelected) tags.div(`class` := "mdRendering paddingTop40")(
                    RawFrag(entry.readme.getOrElse("")))(colspan := 12)
                  else tags.div()
                }
              )
            )
          )
        }.render
      }
    }
  )

  def download(entry: MarketIndexEntry) = {
    val path = manager.current.safePath() ++ entry.name
    downloading() = downloading().updatedFirst(_._1 == entry, (entry, Var(Processing())))
    OMPost[Api].getMarketEntry(entry, path).call().foreach { d ⇒
      downloading() = downloading().updatedFirst(_._1 == entry, (entry, Var(Processed())))
      downloading().headOption.foreach(_ ⇒ close)
      CoreUtils.refreshCurrentDirectory(fileFilter = panels.treeNodePanel.filter)
    }
  }

  def downloadButton(entry: MarketIndexEntry, todo: () ⇒ Unit = () ⇒ {}) =
    downloading().find {
      _._1 == entry
    }.map {
      case (e, state: Var[ProcessState]) ⇒
        state.withTransferWaiter { _ ⇒
          if (selectedEntry() == Some(e)) bs.glyphButton(" Download", btn_success + " redBackground", glyph_download_alt, todo) else tags.div()
        }
    }.getOrElse(tags.div())


  def onOpen() = marketIndex() match {
    case None ⇒ OMPost[Api].marketIndex.call().foreach { m ⇒
      marketIndex() = Some(m)
    }
    case _ ⇒
  }

  def onClose() = {}

  val dialog = bs.modalDialog(modalID,
    headerDialog(
      tags.span(tags.b("Market place"))
    ),
    bodyDialog({
      Rx {
        overwriteAlert() match {
          case Some(e: MarketIndexEntry) ⇒
            AlertPanel.string(e.name + " already exists. Overwrite ? ",
              () ⇒ {
                overwriteAlert() = None
                OMPost[Api].deleteFile(manager.current.safePath() ++ e.name, ServerFileSytemContext.project).call().foreach { d ⇒
                  download(e)
                }
              }, () ⇒ {
                overwriteAlert() = None
              }, CenterTransform())
            tags.div
          case _ ⇒
        }
      }
      tags.div(
        tagFilter.tag,
        marketTable
      )
    }
    ),
    footerDialog(closeButton)
  )

}
