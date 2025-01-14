package org.openmole.gui.client.core

import org.openmole.gui.client.core.dataui._
import org.openmole.gui.ext.dataui.FactoryWithDataUI
import org.openmole.gui.misc.js.{ InputFilter, Select }
import org.openmole.gui.misc.utils.Utils._
import scalatags.JsDom._
import scalatags.JsDom.all._
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import org.openmole.gui.ext.data.ProtoTYPE.DOUBLE
import bs._
import ClientService._
import org.openmole.gui.misc.js.JsRxTags._
import rx._

/*
 * Copyright (C) 12/11/14 // mathieu.leclaire@openmole.org
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

object Panel {

  object ConceptFilter extends Enumeration {

    implicit def dataBagToConceptState(db: DataBagUI): ConceptState = db match {
      case t: TaskDataBagUI      ⇒ TASKS
      case p: PrototypeDataBagUI ⇒ PROTOTYPES
      case _                     ⇒ ALL
    }

    case class ConceptState(name: String, factories: Seq[FactoryWithDataUI]) extends Val(name)

    val ALL = ConceptState("All", ClientService.factories)
    val TASKS = ConceptState("Tasks", ClientService.taskFactories)
    val PROTOTYPES = ConceptState("Prototypes",
      PrototypeFactoryUI.doubleFactory +: ClientService.prototypeFactories filterNot (_.dataUI.dataType == DOUBLE)
    )
  }
}

import Panel.ConceptFilter._

class SettingsPanel(defaultDataBagUI: Either[DataBagUI, ConceptState] = Right(TASKS)) extends ModalPanel {
  lazy val modalID = "settingsPanelID"
  val editionState: Var[Boolean] = Var(false)
  val filter: Var[ConceptState] = Var(defaultDataBagUI.right.toOption.getOrElse(TASKS))
  val rows = Var(0)
  val currentDataBagUI = Var(defaultDataBagUI.left.toOption)
  val panelSequences = new PanelSequences

  val settingTabs: Var[Option[SettingTabs]] = Var(None)

  Obs(currentDataBagUI) {
    resetSettingTabs
  }

  val inputFilter = InputFilter(currentDataBagUI().map {
    _.name()
  }.getOrElse(""))

  val factorySelector: Select[FactoryWithDataUI] = Select("factories",
    filter().factories.map { f ⇒ (f, emptyCK) },
    currentDataBagUI(),
    btn_primary, onclickExtra = () ⇒ {
      currentDataBagUI().map { db ⇒
        db.dataUI() = factorySelector.content().map { f ⇒
          //FIXME: I am sure, there is a better idea than a cast...
          f.dataUI.asInstanceOf[db.DATAUI]
        }.get
        resetSettingTabs
      }
    })

  private val filters = Map[ConceptState, DataBagUI ⇒ Boolean](
    (ALL, db ⇒ inputFilter.contains(db.name())),
    (TASKS, db ⇒ isTaskUI(db) && inputFilter.contains(db.name())),
    (PROTOTYPES, db ⇒ isPrototypeUI(db) && inputFilter.contains(db.name()))
  )

  Obs(filter) {
    filter() match {
      case TASKS      ⇒ factorySelector.setContents(ClientService.taskFactories)
      case PROTOTYPES ⇒ factorySelector.setContents(ClientService.prototypeFactories)
      case _          ⇒ factorySelector.setContents(ClientService.factories)
    }
  }

  val conceptTable = bs.table(striped)(
    thead,
    Rx {
      val dbUIs: Seq[DataBagUI] = filter().factories.head
      tbody({
        val elements = for (db ← dbUIs.sortBy(_.name()) if filters(filter())(db)) yield {
          bs.tr(row)(
            bs.td(col_md_6)(tags.a(dataBagUIView(db), cursor := "pointer", onclick := { () ⇒
              setCurrent(db)
              editionState() = true
            })),
            bs.td(col_md_5)(bs.label(db.dataUI().dataType, label_primary)),
            bs.td(col_md_1)(bs.button(glyph(glyph_trash))(onclick := { () ⇒
              ClientService -= db
            }))
          )
        }
        rows() = elements.size
        elements
      }
      )
    }
  ).render

  def resetSettingTabs = settingTabs() = currentDataBagUI().map { db ⇒ SettingTabs(this, db) }

  def stack(db: DataBagUI, index: Int = 0) = {
    panelSequences.stack(db, index)
  }

  def dataBagUIView(db: DataBagUI) = db match {
    case proto: PrototypeDataBagUI ⇒ proto.name() + " [" + proto.dataUI().dimension().toString + "]"
    case _                         ⇒ db.name() + ""
  }

  val dimInput = bs.input("0")(placeholder := "Dim", width := 50, autofocus).render

  //New button
  val newGlyph =
    //FIXME: THE SIZE OF THE GLYPH IS SMALLER THAN THE REST OF THE GROUP WHEN GROUPEL
    // bs.button(glyph(glyph_plus))(onclick := { () ⇒ add
    bs.button("Add")(onclick := { () ⇒ add
    }).render

  def add = {
    val factory = filter().factories.head
    val dbUI = DataBagUI(factory)
    dbUI.name() = inputFilter.tag.value
    dimInput.value = "0"
    ClientService += dbUI
    setCurrent(dbUI)
    editionState() = true
  }

  val conceptFilter = Rx {
    nav("filterNav", nav_pills, navItem("allfilter", "All", () ⇒ {
      filter() = ALL
    }, active = filter() == ALL),
      navItem("valfilter", "Val", () ⇒ {
        filter() = PROTOTYPES
      }, active = filter() == PROTOTYPES),
      navItem("taskfilter", "Task", () ⇒ {
        filter() = TASKS
      }, active = filter() == TASKS),
      navItem("envfilter", "Env", () ⇒ {
        println("not impl yet")
      })
    )
  }

  def setCurrent(dbUI: DataBagUI) = {
    currentDataBagUI() = Some(dbUI)
    factorySelector.content() = currentDataBagUI()
    filter() = currentDataBagUI().get
    //inputFilter.focus
  }

  def saveHeader = {
    save

    panelSequences.flush match {
      case Some((db: DataBagUI, ind: Int)) ⇒
        setCurrent(db)
        settingTabs().map {
          _.set(ind)
        }
      case _ ⇒
        editionState() = false
        inputFilter.nameFilter() = ""
    }
  }

  val saveHeaderButton = bs.button("Apply", btn_primary)(`type` := "submit", onclick := { () ⇒
    saveHeader
  }).render

  val saveButton = bs.button("Close", btn_test)(data("dismiss") := "modal", onclick := { () ⇒
    do {
      saveHeader
    } while (!panelSequences.isEmpty)
  })

  def onOpen() = {}

  def onClose() = {}

  val dialog = modalDialog(modalID,
    headerDialog(
      Rx {
        tags.div(
          nav(getUUID, navbar_form)(
            bs.form()(
              inputGroup(navbar_left)(
                inputFilter.tag,
                for (c ← prototypeExtraForm) yield c,
                if (editionState()) inputGroupButton(factorySelector.selector)
                else inputGroupButton(newGlyph)
              ),
              if (editionState()) {
                tags.span(inputGroup(navbar_right)(
                  currentDataBagUI().map { db ⇒
                    val h = db.dataUI().help
                    inputGroupButton(bs.button("Help", btn_default)) //FIXME
                  },
                  inputGroupButton(saveHeaderButton)
                )
                )
              }
              else bs.span(navbar_right)(conceptFilter),
              onsubmit := { () ⇒
                if (editionState()) save
                else if (rows() == 0) add
                false
              }
            )
          ))
      }
    ),
    bodyDialog(
      Rx {
        if (editionState()) {
          inputFilter.tag.value = currentDataBagUI().map {
            _.name()
          }.getOrElse((""))
          settingTabs() match {
            case Some(s: SettingTabs) ⇒ s.view
            case _                    ⇒ tags.div(h1("Create a  first data !"))
          }
        }
        else {
          inputFilter.tag.value = ""
          tags.div(conceptTable)
        }
      }),
    footerDialog(
      h2(saveButton)
    )
  )

  def prototypeExtraForm: Seq[Modifier] = currentDataBagUI() match {
    case Some(db: DataBagUI) ⇒ db.dataUI() match {
      case p: PrototypeDataUI ⇒
        if (editionState()) {
          dimInput.value = p.dimension().toString
          Seq(inputGroupButton(attrs.style := "width:0px;"),
            dimInput)
        }
        else Seq()
      case _ ⇒ Seq()
    }
    case _ ⇒ Seq()
  }

  def save = {

    currentDataBagUI().map {
      db ⇒
        ClientService.setName(db, inputFilter.tag.value)
        //In case of prototype
        prototypeUI(db).map {
          _.dimension() = dimInput.value.toInt
        }
    }

    settingTabs().map {
      _.save
    }

  }
}