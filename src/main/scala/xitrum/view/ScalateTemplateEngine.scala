package xitrum.view

import java.io.File

import xitrum.{Config, Action}

class ScalateTemplateEngine extends TemplateEngine {
  def renderTemplate(actionClass: Class[_ <: Action], action: Action, options: Map[String, Any]) =
    Scalate.renderTemplate(actionClass, action, options)
}
