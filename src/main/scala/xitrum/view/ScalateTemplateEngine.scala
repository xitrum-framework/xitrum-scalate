package xitrum.view

import java.io.File

import xitrum.{Config, Controller}
import xitrum.controller.Action

class ScalateTemplateEngine extends TemplateEngine {
  // Scalate takes several seconds to initialize.
  // On startup, an instance of the configured template engine is created, we
  // take this chance to force Scalate to initialize on startup instead of on
  // the first request.

  val b = System.currentTimeMillis()
  Scalate.renderJadeString("")(new xitrum.Controller {})
  val e = System.currentTimeMillis()
  println(e - b)

  Scalate.renderTemplateFile("t.jade")(new xitrum.Controller {})

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
}
