package org.openmole.gui.client.core

import org.openmole.gui.client.core.AbsolutePositioning.{ RightTransform, TopZone, CenterTransform }
import org.openmole.gui.shared.Api
import org.scalajs.dom.raw.{ HTMLElement, HTMLFormElement }
import org.openmole.gui.client.core.panels._
import scalatags.JsDom.{ tags ⇒ tags }
import org.openmole.gui.misc.js.{ OMTags, ToolTipHelp }
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import org.openmole.gui.misc.js.Tooltip._
import bs._
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import org.openmole.gui.misc.js.JsRxTags._
import org.scalajs.dom
import rx._
import scalatags.JsDom.all._

/*
 * Copyright (C) 15/04/15 // mathieu.leclaire@openmole.org
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

@JSExport("ScriptClient")
object ScriptClient {

  @JSExport
  def run(): Unit = {

    val shutdownButton =
      a(`class` := "shutdownButton",
        bs.glyph(glyph_off),
        cursor := "pointer",
        onclick := { () ⇒
          AlertPanel.string("This will stop the server, the application will no longer be usable. Halt anyway?",
            () ⇒ {
              treeNodePanel.fileDisplayer.tabs.saveAllTabs(() ⇒
                dom.window.location.href = "shutdown"
              )
            },
            transform = RightTransform(),
            zone = TopZone())
        })

    val passwordChosen = Var(true)
    val passwordOK = Var(false)

    OMPost[Api].passwordState().call().foreach { b ⇒
      passwordChosen() = b.chosen
      passwordOK() = b.hasBeenSet
    }

    val body = dom.document.body
    val maindiv = body.appendChild(tags.div())

    val passwordInput = bs.input("")(
      placeholder := "Password",
      `type` := "password",
      width := "130px",
      autofocus := true
    ).render

    val passwordAgainInput = bs.input("")(
      placeholder := "Password again",
      `type` := "password",
      width := "130px",
      autofocus
    ).render

    def cleanInputs = {
      passwordInput.value = ""
      passwordAgainInput.value = ""
    }

    def resetPassword = OMPost[Api].resetPassword().call().foreach { b ⇒
      passwordChosen() = false
      passwordOK() = false
      cleanInputs
    }

    val authenticationPanel = new AuthenticationPanel(() ⇒ {
      resetPassword
    }
    )

    def setPassword(s: String) = OMPost[Api].setPassword(s).call().foreach { b ⇒
      passwordOK() = b
      cleanInputs
    }

    lazy val connectButton = bs.button("Connect", btn_primary)(onclick := { () ⇒
      connection
    }).render

    def connection: Unit = {
      if (passwordChosen()) setPassword(passwordInput.value)
      else if (passwordInput.value == passwordAgainInput.value) {
        passwordChosen() = true
        setPassword(passwordInput.value)
      }
      else cleanInputs
    }

    def connectionForm(i: HTMLElement): HTMLFormElement =
      tags.form(i, `type` := "submit", onsubmit := { () ⇒
        connection
        false
      }
      ).render

    val alert: Var[Boolean] = Var(false)

    /* val openmoleText = tags.div(
      tags.h1(`class` := "openmole-connection openmole-pen")("pen"),
      tags.h1(`class` := "openmole-connection openmole-mole")("MOLE")
    )*/

    val connectionDiv = tags.div(`class` := Rx {
      if (!passwordOK()) "connectionTabOverlay" else "displayOff"
    })(
      tags.div(
        tags.img(src := "img/openmole.png", `class` := "openmole-logo"),
        // openmoleText,
        shutdownButton,
        tags.div(`class` := Rx {
          if (!passwordOK()) "centerPage" else ""
        },
          Rx {
            tags.div(
              if (alert())
                AlertPanel.string("Careful! Resetting your password will wipe out all your preferences! Reset anyway?",
                () ⇒ {
                  alert() = false
                  resetPassword
                }, () ⇒ {
                  alert() = false
                }, CenterTransform())
              else {
                tags.div(
                  connectionForm(
                    tags.span(passwordInput,
                      tags.a(onclick := { () ⇒
                        alert() = true
                      }, cursor := "pointer")("Reset password")).render),
                  if (!passwordChosen()) connectionForm(passwordAgainInput) else tags.div(),
                  connectButton
                )
              }
            )
          }
        )
      )
    )

    val openFileTree = Var(true)

    val authenticationTriggerer = new PanelTriggerer {
      val modalPanel = authenticationPanel
    }

    val execItem = dialogNavItem("executions", glyph(glyph_settings).tooltip("Executions"), () ⇒ executionTriggerer.triggerOpen)

    val authenticationItem = dialogNavItem("authentications", glyph(glyph_lock).tooltip("Authentications"), () ⇒ authenticationTriggerer.triggerOpen)

    val marketItem = dialogNavItem("market", glyph(glyph_market).tooltip("Market place"), () ⇒ marketTriggerer.triggerOpen)

    val pluginItem = dialogNavItem("plugin", bs.div(OMTags.glyph_plug).tooltip("Plugins"), () ⇒ pluginTriggerer.triggerOpen)

    val envItem = dialogNavItem("envError", glyph(glyph_exclamation).render, () ⇒ environmentStackTriggerer.open)

    val docItem = dialogNavItem("doc", bs.div(OMTags.glyph_book).tooltip("Documentation"), () ⇒ docTriggerer.open)

    val modelWizardItem = dialogNavItem("modelWizard", glyph(OMTags.glyph_upload_alt).tooltip("Model import"), () ⇒ modelWizardTriggerer.triggerOpen)

    val fileItem = dialogNavItem("files", glyph(glyph_file).tooltip("Files"), todo = () ⇒ {
      openFileTree() = !openFileTree()
    })

    maindiv.appendChild(
      nav("mainNav",
        nav_pills + nav_inverse + nav_staticTop,
        fileItem,
        modelWizardItem,
        execItem,
        authenticationItem,
        marketItem,
        pluginItem,
        docItem
      )
    )
    maindiv.appendChild(tags.div(shutdownButton))
    maindiv.appendChild(executionTriggerer.modalPanel.dialog.render)
    maindiv.appendChild(modelWizardTriggerer.modalPanel.dialog.render)
    maindiv.appendChild(authenticationTriggerer.modalPanel.dialog.render)
    maindiv.appendChild(marketTriggerer.modalPanel.dialog.render)
    maindiv.appendChild(pluginTriggerer.modalPanel.dialog.render)
    maindiv.appendChild(environmentStackTriggerer.modalPanel.dialog.render)
    maindiv.appendChild(docTriggerer.modalPanel.dialog.render)
    maindiv.appendChild(AlertPanel.alertDiv)

    Settings.workspacePath.foreach { projectsPath ⇒
      maindiv.appendChild(
        tags.div(`class` := "fullpanel")(
          tags.div(`class` := Rx {
            "leftpanel " + {
              if (openFileTree()) "open" else ""
            }
          })(treeNodePanel.view.render),
          tags.div(`class` := Rx {
            "centerpanel " + {
              if (openFileTree()) "reduce" else ""
            }
          })(treeNodePanel.fileDisplayer.tabs.render,
            tags.img(src := "img/version.svg", `class` := "logoVersion"),
            tags.div("Loving Lobster", `class` := "textVersion")
          )

        ).render
      )
    }

    body.appendChild(connectionDiv)
    body.appendChild(maindiv)
  }

}