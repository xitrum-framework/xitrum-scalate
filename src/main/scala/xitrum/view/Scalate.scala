package xitrum.view

import java.io.{File, PrintWriter, StringWriter}

import org.fusesource.scalate.{Binding, DefaultRenderContext, RenderContext, Template, TemplateEngine => STE}
import org.fusesource.scalate.scaml.ScamlOptions
import org.fusesource.scalate.support.StringTemplateSource

import com.esotericsoftware.reflectasm.ConstructorAccess
import org.jboss.netty.handler.codec.serialization.ClassResolvers

import xitrum.{Config, Action, Logger}

object Scalate extends Logger {
  private[this] val ACTION_BINDING_ID  = "helper"
  private[this] val CONTEXT_BINDING_ID = "context"

  private[this] val application = xitrum.Config.application
  private[this] val config      = application.getConfig("scalate")
  private[this] val defaultType = config.getString("defaultType")
  private[this] val dir         = config.getString("dir")

  private[this] val classResolver = ClassResolvers.softCachingConcurrentResolver(getClass.getClassLoader)

  private[this] val fileEngine = {
    val ret = new STE
    ret.allowCaching = true
    ret.allowReload  = !Config.productionMode
    ret
  }

  private[this] val stringEngine = {
    val ret = new STE
    ret.allowCaching = false
    ret.allowReload  = false
    ret
  }

  {
    ScamlOptions.ugly = Config.productionMode

    Seq(fileEngine, stringEngine).foreach { engine =>
      engine.bindings = List(
        // import things in the current action
        Binding(ACTION_BINDING_ID, classOf[Action].getName, true),

        // import Scalate utilities like "unescape"
        Binding(CONTEXT_BINDING_ID, classOf[RenderContext].getName, true)
      )
    }
  }

  //----------------------------------------------------------------------------
  // For ScalateTemplateEngine

  /**
   * Renders Scalate template file at the path:
   * <scalateDir>/<class/name/of/the/location>.<templateType>
   *
   * @param currentAction will be imported in the template as "helper"
   */
  def renderView(location: Class[_ <: Action], currentAction: Action, options: Map[String, Any]): String = {
    val tpe     = templateType(options)
    val relPath = location.getName.replace('.', File.separatorChar) + "." + tpe
    Scalate.renderMaybePrecompiledFile(relPath, currentAction)
  }

  /**
   * Renders Scalate template file at the path:
   * <scalateDir>/<package/name/of/the/location>/_<fragment>.<templateType>
   *
   * @param currentAction will be imported in the template as "helper"
   */
  def renderFragment(location: Class[_ <: Action], fragment: String, currentAction: Action, options: Map[String, Any]): String = {
    val tpe     = templateType(options)
    val relPath = location.getPackage.getName.replace('.', File.separatorChar) + File.separatorChar + "_" + fragment + "." + tpe
    Scalate.renderMaybePrecompiledFile(relPath, currentAction)
  }

  //----------------------------------------------------------------------------

  def renderJadeString(templateContent: String)(implicit currentAction: Action) =
    renderString(templateContent, "jade")(currentAction)

  def renderMustacheString(templateContent: String)(implicit currentAction: Action) =
    renderString(templateContent, "mustache")(currentAction)

  def renderScamlString(templateContent: String)(implicit currentAction: Action) =
    renderString(templateContent, "scaml")(currentAction)

  def renderSspString(templateContent: String)(implicit currentAction: Action) =
    renderString(templateContent, "ssp")(currentAction)

  /** @param templateType jade, mustache, scaml, or ssp */
  def renderString(templateContent: String, templateType: String)(implicit currentAction: Action): String = {
    val templateUri = "scalate." + templateType
    val (context, buffer, out) = createContext(templateUri, false, currentAction)
    val template               = new StringTemplateSource(templateUri, templateContent)
    stringEngine.layout(template, context)
    out.close()
    buffer.toString
  }

  //----------------------------------------------------------------------------

  /**
   * Renders Scalate template file
   *
   * @param templateFile absolute file path of template
   * @param currentAction will be imported in the template as "helper"
   */
  def renderTemplateFile(templateFile: String)(implicit currentAction: Action): String = {
    val (context, buffer, out) = createContext(templateFile, true, currentAction)
    fileEngine.layout(templateFile, context)
    out.close()
    buffer.toString
  }

  //----------------------------------------------------------------------------

  /**
   * Takes out "type" from options. It shoud be one of:
   * "jade", "mustache", "scaml", or "ssp"
   */
  private def templateType(options: Map[String, Any]) =
    options.getOrElse("type", defaultType)

  private def createContext(templateUri: String, isFile: Boolean, currentAction: Action):
    (RenderContext, StringWriter, PrintWriter) = {
    val buffer     = new StringWriter
    val out        = new PrintWriter(buffer)
    val engine     = if (isFile) fileEngine else stringEngine
    val context    = new DefaultRenderContext(templateUri, engine, out)
    val attributes = context.attributes

    // For bindings in engine
    attributes.update(ACTION_BINDING_ID,  currentAction)
    attributes.update(CONTEXT_BINDING_ID, context)

    // Put action.at to context
    currentAction.at.foreach { case (k, v) =>
      if (k == ACTION_BINDING_ID || k == CONTEXT_BINDING_ID)
        logger.warn(
          ACTION_BINDING_ID + " and " + CONTEXT_BINDING_ID +
          " are reserved key names for action's \"at\""
        )
      else
        attributes.update(k, v)
    }

    (context, buffer, out)
  }

  //----------------------------------------------------------------------------

  /**
   * Production mode: Renders the precompiled template class.
   * Development mode: Renders Scalate template file relative to dir. If the file
   * does not exist, falls back to rendering the precompiled template class.
   * @param action will be imported in the template as "helper"
   */
  private def renderMaybePrecompiledFile(relPath: String, currentAction: Action): String = {
    if (Config.productionMode)
      renderPrecompiledFile(relPath, currentAction)
    else
      renderNonPrecompiledFile(relPath, currentAction)
  }

  private def renderPrecompiledFile(relPath: String, currentAction: Action): String = {
    val (context, buffer, out) = createContext(relPath, true, currentAction)

    // In production mode, after being precompiled
    // quickstart/action/AppAction.jade  -> class scalate.quickstart.action.$_scalate_$AppAction_jade
    val withDots     = relPath.replace('/', '.').replace(File.separatorChar, '.')
    val xs           = withDots.split('.')
    val extension    = xs.last
    val baseFileName = xs(xs.length - 2)
    val prefix       = xs.take(xs.length - 2).mkString(".")
    val className    = "scalate." + prefix + ".$_scalate_$" + baseFileName + "_" + extension
    val klass        = classResolver.resolve(className)
    val template     = ConstructorAccess.get(klass).newInstance().asInstanceOf[Template]
    fileEngine.layout(template, context)

    out.close()
    buffer.toString
  }

  private def renderNonPrecompiledFile(relPath: String, currentAction: Action): String = {
    val path = dir + File.separator + relPath
    val file = new File(path)
    if (file.exists()) {
      renderTemplateFile(path)(currentAction)
    } else {
      // If called from a JAR library, the template may have been precompiled
      renderPrecompiledFile(relPath, currentAction)
    }
  }

/*
val normalErrorMsg = e.toString + "\n\n" + e.getStackTraceString
val errorMsg = if (e.isInstanceOf[org.fusesource.scalate.InvalidSyntaxException]) {
  val ise = e.asInstanceOf[org.fusesource.scalate.InvalidSyntaxException]
  val pos = ise.pos
  "Scalate syntax error: " + ise.source.uri + ", line " + pos.line + "\n" +
  pos.longString + "\n\n" +
  normalErrorMsg
} else {
  normalErrorMsg
}
*/
}
