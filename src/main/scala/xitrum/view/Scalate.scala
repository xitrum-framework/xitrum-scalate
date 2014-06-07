package xitrum.view

import java.io.{File, PrintWriter, StringWriter}

import org.fusesource.scalate.{Binding, DefaultRenderContext, RenderContext, Template, TemplateEngine => STE}
import org.fusesource.scalate.scaml.ScamlOptions
import org.fusesource.scalate.support.StringTemplateSource

import com.esotericsoftware.reflectasm.ConstructorAccess
import org.jboss.netty.handler.codec.serialization.ClassResolvers

import xitrum.{Config, Action, Log}

class Scalate extends ScalateEngine(
  "src/main/scalate",
  !Config.productionMode,
  Config.xitrum.config.getString("template.\"" + classOf[Scalate].getName + "\".defaultType")
) {
  // Scalate takes several seconds to initialize => Warm it up here
  override def start() {
    val dummyAction = new Action {
      def execute() {}
    }

    renderJadeString("")(dummyAction)
    renderMustacheString("")(dummyAction)
    renderScamlString("")(dummyAction)
    renderSspString("")(dummyAction)

    // Can't warmup Scalate.renderTemplateFile:
    // https://github.com/xitrum-framework/xitrum-scalate/issues/6
  }
}

object ScalateEngine {
  val ACTION_BINDING_ID  = "helper"
  val CONTEXT_BINDING_ID = "context"
  val CLASS_RESOLVER     = ClassResolvers.softCachingConcurrentResolver(getClass.getClassLoader)

  ScamlOptions.ugly = Config.productionMode
}

/**
 * @param allowReload Template files in templateDir will be reloaded every time
 * @param defaultType "jade", "mustache", "scaml", or "ssp"
 */
class ScalateEngine(templateDir: String, allowReload: Boolean, defaultType: String) extends TemplateEngine with Log {
  import ScalateEngine._

  private[this] var fileEngine   = createEngine(true,  allowReload)
  private[this] var stringEngine = createEngine(false, false)

  private def createEngine(allowCaching: Boolean, allowReload: Boolean): STE = {
    val ret          = new STE
    ret.allowCaching = allowCaching
    ret.allowReload  = allowReload

    ret.bindings = List(
      // import things in the current action
      Binding(ACTION_BINDING_ID, classOf[Action].getName, true),

      // import Scalate utilities like "unescape"
      Binding(CONTEXT_BINDING_ID, classOf[RenderContext].getName, true)
    )

    ret
  }

  //----------------------------------------------------------------------------
  // TemplateEngine methods

  def start() {}

  def stop() {}

  // This method is only called in development mode, thus synchronization for
  // the engines is not needed.
  def reloadOnNextRender() {
    fileEngine.shutdown()
    stringEngine.shutdown()
    fileEngine   = createEngine(true,  allowReload)
    stringEngine = createEngine(false, false)
  }

  /**
   * Renders the template at the location identified by the given action class:
   * {{{<scalateDir>/<class/name/of/the/location>.<templateType>}}}
   *
   * Ex:
   * When location = myapp.SiteIndex,
   * the template path will be:
   * src/main/scalate/myapp/SiteIndex.jade
   *
   * @param location      Action class used to identify the template location
   * @param currentAction Will be imported in the template as "helper"
   * @param options       Specific to the configured template engine
   */
  def renderView(location: Class[_ <: Action], currentAction: Action, options: Map[String, Any]): String = {
    val tpe     = templateType(options)
    val relPath = location.getName.replace('.', File.separatorChar) + "." + tpe
    renderMaybePrecompiledFile(relPath, currentAction)
  }

  /**
   * Renders the template at the location identified by the package of the given
   * action class and the given fragment:
   * {{{<scalateDir>/<package/name/of/the/location>/_<fragment>.<templateType>}}}
   *
   * Ex:
   * When location = myapp.ArticleNew, fragment = form,
   * the template path will be:
   * src/main/scalate/myapp/_form.jade
   *
   * @param location      Action class used to identify the template location
   * @param currentAction Will be imported in the template as "helper"
   * @param options       Specific to the configured template engine
   */
  def renderFragment(location: Class[_ <: Action], fragment: String, currentAction: Action, options: Map[String, Any]): String = {
    val tpe     = templateType(options)
    val relPath = location.getPackage.getName.replace('.', File.separatorChar) + File.separatorChar + "_" + fragment + "." + tpe
    renderMaybePrecompiledFile(relPath, currentAction)
  }

  //----------------------------------------------------------------------------
  // Additional utility methods

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
    val templateUri            = "scalate." + templateType
    val (context, buffer, out) = createContext(templateUri, stringEngine, currentAction)
    val template               = new StringTemplateSource(templateUri, templateContent)
    stringEngine.layout(template, context)
    out.close()
    buffer.toString
  }

  //----------------------------------------------------------------------------
  // Additional utility methods

  /**
   * Renders Scalate template file.
   *
   * @param templateFile  Absolute file path of template
   * @param currentAction Will be imported in the template as "helper"
   */
  def renderTemplateFile(templateFile: String)(implicit currentAction: Action): String = {
    val (context, buffer, out) = createContext(templateFile, fileEngine, currentAction)
    fileEngine.layout(templateFile, context)
    out.close()
    buffer.toString
  }

  /**
   * Renders precompiled Scalate template.
   *
   * @param template      Template object
   * @param templateUri   URI to identify a template
   * @param currentAction Will be imported in the template as "helper"
   */
  def renderTemplate(template: Template, templateUri: String = "precompiled_template")(implicit currentAction: Action): String = {
    val (context, buffer, out) = createContext(templateUri, fileEngine, currentAction)
    fileEngine.layout(template, context)
    out.close()
    buffer.toString
  }

  //----------------------------------------------------------------------------

  /**
   * Takes out "type" from options. It shoud be one of:
   * "jade", "mustache", "scaml", or "ssp"
   */
  private def templateType(options: Map[String, Any]): String =
    options.getOrElse("type", defaultType).asInstanceOf[String]

  private def createContext(
    templateUri: String, engine: STE, currentAction: Action
  ): (RenderContext, StringWriter, PrintWriter) =
  {
    val buffer     = new StringWriter
    val out        = new PrintWriter(buffer)
    val context    = new DefaultRenderContext(templateUri, engine, out)
    val attributes = context.attributes

    // For bindings in engine
    attributes.update(ACTION_BINDING_ID,  currentAction)
    attributes.update(CONTEXT_BINDING_ID, context)

    // Put action.at to context
    currentAction.at.foreach { case (k, v) =>
      if (k == ACTION_BINDING_ID || k == CONTEXT_BINDING_ID)
        log.warn(
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
   * Development mode: Renders Scalate template file relative to templateDir.
   * If the file does not exist, falls back to rendering the precompiled template class.
   *
   * @param action Will be imported in the template as "helper"
   */
  private def renderMaybePrecompiledFile(relPath: String, currentAction: Action): String = {
    if (Config.productionMode)
      renderPrecompiledFile(relPath, currentAction)
    else
      renderNonPrecompiledFile(relPath, currentAction)
  }

  private def renderPrecompiledFile(relPath: String, currentAction: Action): String = {
    // In production mode, after being precompiled,
    // quickstart/action/AppAction.jade will become
    // class scalate.quickstart.action.$_scalate_$AppAction_jade
    val withDots     = relPath.replace('/', '.').replace(File.separatorChar, '.')
    val xs           = withDots.split('.')
    val extension    = xs.last
    val baseFileName = xs(xs.length - 2)
    val prefix       = xs.take(xs.length - 2).mkString(".")
    val className    = "scalate." + prefix + ".$_scalate_$" + baseFileName + "_" + extension
    val klass        = CLASS_RESOLVER.resolve(className)
    val template     = ConstructorAccess.get(klass).newInstance().asInstanceOf[Template]

    renderTemplate(template, relPath)(currentAction)
  }

  private def renderNonPrecompiledFile(relPath: String, currentAction: Action): String = {
    val path = templateDir + File.separator + relPath
    val file = new File(path)
    if (file.exists) {
      renderTemplateFile(path)(currentAction)
    } else {
      // If called from a JAR library, the template may have been precompiled
      renderPrecompiledFile(relPath, currentAction)
    }
  }
}
