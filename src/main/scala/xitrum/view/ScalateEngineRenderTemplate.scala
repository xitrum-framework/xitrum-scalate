package xitrum.view

import scala.util.control.NonFatal

import org.fusesource.scalate.InvalidSyntaxException
import org.fusesource.scalate.Template

import xitrum.Action

/** Additional utility methods. */
trait ScalateEngineRenderTemplate {
  this: ScalateEngine =>

  def renderTemplateFile(templateUri: String)(implicit currentAction: Action): String =
    renderTemplateFile(templateUri, Map.empty)(currentAction)

  /**
   * Renders Scalate template file.
   *
   * @param options       "date" -> DateFormat, "number" -> NumberFormat
   * @param currentAction Will be imported in the template as "helper"
   */
  def renderTemplateFile(uri: String, options: Map[String, Any])(implicit currentAction: Action): String = {
    val context = createContext(uri, fileEngine, currentAction, options)
    try {
      fileEngine.layout(uri, context)
      context.buffer.toString
    } catch {
      case e: InvalidSyntaxException =>
        throw ScalateEngine.invalidSyntaxExceptionWithErrorLine(e)

      case NonFatal(e) =>
        throw ScalateEngine.exceptionWithErrorLine(e, uri)
    } finally {
      context.out.close()
    }
  }

  //----------------------------------------------------------------------------

  def renderTemplate(template: Template)(implicit currentAction: Action): String =
    renderTemplate(template, "precompiled_template", Map.empty[String, Any])(currentAction)

  def renderTemplate(template: Template, options: Map[String, Any])(implicit currentAction: Action): String =
    renderTemplate(template, "precompiled_template", options)(currentAction)

  def renderTemplate(template: Template, templateUri: String)(implicit currentAction: Action): String =
    renderTemplate(template, templateUri, Map.empty[String, Any])(currentAction)

  /**
   * Renders precompiled Scalate template.
   *
   * @param template      Template object
   * @param templateUri   URI to identify a template
   * @param options       "date" -> DateFormat, "number" -> NumberFormat
   * @param currentAction Will be imported in the template as "helper"
   */
  def renderTemplate(template: Template, templateUri: String, options: Map[String, Any])(implicit currentAction: Action): String = {
    val context = createContext(templateUri, fileEngine, currentAction, options)
    try {
      fileEngine.layout(template, context)
      context.buffer.toString
    } finally {
      context.out.close()
    }
  }
}
