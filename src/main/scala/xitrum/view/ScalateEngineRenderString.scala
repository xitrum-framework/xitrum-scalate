package xitrum.view

import scala.util.control.NonFatal

import org.fusesource.scalate.InvalidSyntaxException
import org.fusesource.scalate.support.StringTemplateSource

import xitrum.Action

/** Additional utility methods. */
trait ScalateEngineRenderString {
  this: ScalateEngine =>

  def renderJadeString(templateContent: String)(implicit currentAction: Action) =
    renderString(templateContent, "jade", Map.empty)(currentAction)

  def renderJadeString(templateContent: String, options: Map[String, Any])(implicit currentAction: Action) =
    renderString(templateContent, "jade", options)(currentAction)

  def renderMustacheString(templateContent: String)(implicit currentAction: Action) =
    renderString(templateContent, "mustache", Map.empty)(currentAction)

  def renderMustacheString(templateContent: String, options: Map[String, Any])(implicit currentAction: Action) =
    renderString(templateContent, "mustache", options)(currentAction)

  def renderScamlString(templateContent: String)(implicit currentAction: Action) =
    renderString(templateContent, "scaml", Map.empty)(currentAction)

  def renderScamlString(templateContent: String, options: Map[String, Any])(implicit currentAction: Action) =
    renderString(templateContent, "scaml", options)(currentAction)

  def renderSspString(templateContent: String)(implicit currentAction: Action) =
    renderString(templateContent, "ssp", Map.empty)(currentAction)

  def renderSspString(templateContent: String, options: Map[String, Any])(implicit currentAction: Action) =
    renderString(templateContent, "ssp", options)(currentAction)

  def renderString(templateContent: String, templateType: String)(implicit currentAction: Action): String =
    renderString(templateContent, templateContent, Map.empty)(currentAction)

  /**
   * @param templateType jade, mustache, scaml, or ssp
   * @param options      "date" -> DateFormat, "number" -> NumberFormat
   */
  def renderString(templateContent: String, templateType: String, options: Map[String, Any])(implicit currentAction: Action): String = {
    val templateUri = "xitrum_scalate_string." + templateType
    val context     = createContext(templateUri, stringEngine, currentAction, options)
    try {
      val template = new StringTemplateSource(templateUri, templateContent)
      stringEngine.layout(template, context)
      context.buffer.toString
    } catch {
      case e: InvalidSyntaxException =>
        throw ScalateEngine.invalidSyntaxExceptionWithErrorLine(e)

      case NonFatal(e) =>
        throw ScalateEngine.exceptionWithErrorLine(e, templateUri, templateContent)
    } finally {
      context.out.close()
    }
  }
}
