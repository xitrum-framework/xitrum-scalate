package xitrum.view

import xitrum.Action

/** Core xitrum.view.TemplateEngine interface methods. */
trait ScalateEngineRenderInterface {
  this: ScalateEngine =>

  def renderTemplate(uri: String, currentAction: Action, options: Map[String, Any]): String = {
    val tpe = templateType(options)
    val uriWithTemplateType = uri + "." + tpe
    renderMaybePrecompiledFile(uriWithTemplateType, currentAction, options)
  }
}
