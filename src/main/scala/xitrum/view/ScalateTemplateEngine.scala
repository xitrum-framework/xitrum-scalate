package xitrum.view

import java.io.File

import xitrum.{Config, Controller}
import xitrum.controller.Action

class ScalateTemplateEngine extends TemplateEngine {
  warmup()

  def renderTemplate(
    controller: Controller, action: Action,
    controllerName: String, actionName: String,
    options: Map[String, Any]
  ) = Scalate.renderTemplate(controller, action, controllerName, actionName, options)

  def renderTemplate(
    controller: Controller, controllerClass: Class[_],
    options: Map[String, Any]
  ) = Scalate.renderTemplate(controller, controllerClass, options)

  def renderFragment(
    controller: Controller, controllerClass: Class[_], fragment: String,
    options: Map[String, Any]
  ) = Scalate.renderFragment(controller, controllerClass, fragment, options)

  // Scalate takes several seconds to initialize.
  // On Xitrum startup, an instance of the configured template engine is created.
  // We take this chance to force Scalate to initialize on startup instead of on
  // the first request.
  private def warmup() {
    Scalate.renderJadeString("")(new xitrum.Controller {})

    val tmpFile = File.createTempFile("tmp", ".jade")
    Scalate.renderTemplateFile(tmpFile.getAbsolutePath)(new xitrum.Controller {})
    tmpFile.delete()
  }
}
