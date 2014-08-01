package xitrum.view

import java.io.{File, PrintWriter, StringWriter}
import java.text.{DateFormat, NumberFormat}
import java.util.Locale

import org.fusesource.scalate.{Binding, DefaultRenderContext, RenderContext, Template, TemplateEngine => STE}
import org.fusesource.scalate.scaml.ScamlOptions

import com.esotericsoftware.reflectasm.ConstructorAccess
import org.jboss.netty.handler.codec.serialization.ClassResolvers

import xitrum.{Config, Action, Log}

/**
 * This class is intended for use only by Xitrum. Apps that want to create
 * additional Scalate template engine instances can use ScalateEngine.
 */
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

  System.setProperty("scalate.workdir", Config.xitrum.tmpDir.getAbsolutePath + File.separator + "scalate")
  ScamlOptions.ugly = Config.productionMode
}

/**
 * This class is intended for use by both Xitrum and normal apps to create
 * additional Scalate template engine instances.
 *
 * @param allowReload Template files in templateDir will be reloaded every time
 * @param defaultType "jade", "mustache", "scaml", or "ssp"
 */
class ScalateEngine(
  templateDir: String, allowReload: Boolean, defaultType: String
) extends TemplateEngine
  with ScalateEngineRenderInterface
  with ScalateEngineRenderTemplate
  with ScalateEngineRenderString
  with Log
{
  import ScalateEngine._

  protected[this] val fileEngine = createEngine(true, allowReload)

  // No need to cache or reload for stringEngine.
  protected[this] val stringEngine = createEngine(false, false)

  protected def createEngine(allowCaching: Boolean, allowReload: Boolean): STE = {
    val ret          = new STE
    ret.allowCaching = allowCaching
    ret.allowReload  = allowReload
    ret.bindings     = List(
      // import things in the current action
      Binding(ACTION_BINDING_ID, classOf[Action].getName, true),

      // import Scalate utilities like "unescape"
      Binding(CONTEXT_BINDING_ID, classOf[RenderContext].getName, true)
    )

    ret
  }

  //----------------------------------------------------------------------------
  // TemplateEngine interface methods (see also ScalateEngineRenderInterface)

  def start() {}

  def stop() {
    fileEngine.shutdown()
    stringEngine.shutdown()
  }

  //----------------------------------------------------------------------------

  /**
   * Takes out "type" from options. It shoud be one of:
   * "jade", "mustache", "scaml", or "ssp"
   */
  protected def templateType(options: Map[String, Any]): String =
    options.getOrElse("type", defaultType).asInstanceOf[String]

  /** Takes out "date" format from options. */
  protected def dateFormat(options: Map[String, Any]): Option[DateFormat] =
    options.get("date").map(_.asInstanceOf[DateFormat])

  /** Takes out "number" format from options. */
  protected def numberFormat(options: Map[String, Any]): Option[NumberFormat] =
    options.get("number").map(_.asInstanceOf[NumberFormat])

  /**
   * If "date" (java.text.DateFormat) or "number" (java.text.NumberFormat)
   * is not set in "options", the format corresponding to current language in
   * "currentAction" will be used.
   */
  protected def createContext(
    templateUri: String, engine: STE,
    currentAction: Action, options: Map[String, Any]
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

    setFormats(context, currentAction, options)
    (context, buffer, out)
  }

  protected def setFormats(context: RenderContext, currentAction: Action, options: Map[String, Any]) {
    val lo = Locale.forLanguageTag(currentAction.language)
    val df = dateFormat(options).getOrElse(DateFormat.getDateInstance(DateFormat.DEFAULT, lo))
    val nf = numberFormat(options).getOrElse(NumberFormat.getInstance(lo))

    context.dateFormat   = df
    context.numberFormat = nf
  }

  //----------------------------------------------------------------------------

  /**
   * Production mode: Renders the precompiled template class.
   * Development mode: Renders Scalate template file relative to templateDir.
   * If the file does not exist, falls back to rendering the precompiled template class.
   *
   * @param action Will be imported in the template as "helper"
   */
  protected def renderMaybePrecompiledFile(relPath: String, currentAction: Action, options: Map[String, Any]): String = {
    if (Config.productionMode)
      renderPrecompiledFile(relPath, currentAction, options)
    else
      renderNonPrecompiledFile(relPath, currentAction, options)
  }

  protected def renderPrecompiledFile(relPath: String, currentAction: Action, options: Map[String, Any]): String = {
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

    renderTemplate(template, relPath, options)(currentAction)
  }

  protected def renderNonPrecompiledFile(relPath: String, currentAction: Action, options: Map[String, Any]): String = {
    val path = templateDir + File.separator + relPath
    val file = new File(path)
    if (file.exists) {
      renderTemplateFile(path)(currentAction)
    } else {
      // If called from a JAR library, the template may have been precompiled
      renderPrecompiledFile(relPath, currentAction, options)
    }
  }
}
