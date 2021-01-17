package xitrum.view

import java.io.File
import java.text.{DateFormat, NumberFormat}
import scala.collection.mutable.ListBuffer
import scala.util.Try
import scala.util.parsing.input.NoPosition
import org.fusesource.scalate.{Binding, InvalidSyntaxException, NoSuchViewException, RenderContext, Template, TemplateEngine => STE}
import org.fusesource.scalate.scaml.ScamlOptions
import com.esotericsoftware.reflectasm.ConstructorAccess
import io.netty.handler.codec.serialization.{ClassResolver, ClassResolvers}
import xitrum.{Action, Config, Log}

/**
 * This class is intended for use only by Xitrum. Apps that want to create
 * additional Scalate template engine instances can use [[ScalateEngine]].
 */
class Scalate extends ScalateEngine(
  ScalateEngine.DEV_TEMPLATE_DIR_URI,
  !Config.productionMode,
  Config.xitrum.config.getString("template.\"" + classOf[Scalate].getName + "\".defaultType")
) {
  override def start(): Unit = {
    // Scalate takes several seconds to initialize => Warm it up here

    val dummyAction = new Action {
      def execute(): Unit = {}
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
  val WORKDIR: String = Config.xitrum.tmpDir.getAbsolutePath + File.separator + "scalate"

  val ACTION_BINDING_ID = "helper"

  val CONTEXT_BINDING_ID = "context"

  val CLASS_RESOLVER: ClassResolver = ClassResolvers.softCachingConcurrentResolver(getClass.getClassLoader)

  val DEV_TEMPLATE_DIR_URI = "src/main/scalate"

  System.setProperty("scalate.workdir", WORKDIR)
  ScamlOptions.ugly = Config.productionMode

  /** Puts error line right in the exception message so that Xitrum can simply display it. */
  def invalidSyntaxExceptionWithErrorLine(e: InvalidSyntaxException): InvalidSyntaxException = {
    val pos = e.pos
    if (pos == NoPosition) {
      e
    } else {
      val errorLine = e.source.uri + "\n" + pos.longString
      val eWithErrorLine = new InvalidSyntaxException(e.brief + "\n" + errorLine, pos)
      eWithErrorLine.source = e.source
      eWithErrorLine
    }
  }

  def exceptionWithErrorLine(e: Throwable, templateUri: String, templateContent: String = ""): Throwable = {
    val generatedScalaFileUri = WORKDIR + "/src/" + templateUri + ".scala"
    val file = new File(generatedScalaFileUri)
    if (file.exists) {
      val src = srcWithLineNumbers(scala.io.Source.fromFile(file).getLines())
      val errorLine = e.getStackTrace()(0).getLineNumber
      val msg =
        if (templateContent.isEmpty)
          e.toString + "\n" +
            templateUri + "\n" +
            generatedScalaFileUri + ":" + errorLine + "\n" +
            src
        else
          e.toString + "\n" +
            templateUri + "\n" +
            srcWithLineNumbers(templateContent.split('\n').iterator) + "\n" +
            generatedScalaFileUri + ":" + errorLine + "\n" +
            src
      new RuntimeException(msg, e)
    } else {
      e
    }
  }

  def srcWithLineNumbers(lineIt: Iterator[String]): String = {
    val builder = new StringBuilder
    var lineNum = 1
    while (lineIt.hasNext) {
      builder.append("%4d  ".format(lineNum))
      builder.append(lineIt.next())
      builder.append("\n")
      lineNum += 1
    }
    builder.toString
  }

  def getPrecompiledTemplateInstance(relUri: String): Template = {
    val withDots = relUri.replace('/', '.')
    val xs = withDots.split('.')
    val extension = xs.last
    val baseFileName = xs(xs.length - 2)
    val prefix = xs.take(xs.length - 2).mkString(".")
    val className = "scalate." + prefix + ".$_scalate_$" + baseFileName + "_" + extension
    val klass = CLASS_RESOLVER.resolve(className)
    ConstructorAccess.get(klass).newInstance().asInstanceOf[Template]
  }

  def viewTemplate(model: AnyRef, engine: STE, viewName: String, templateType: String): Template = {
    if (Config.productionMode) {
      getPrecompiledScalateViewInstance(model, viewName, templateType)
    } else {
      val uri = getScalateViewInstance(DEV_TEMPLATE_DIR_URI, model, viewName, templateType)
      engine.load(uri, Nil)
    }
  }

  private def getPrecompiledScalateViewInstance(model: AnyRef, viewName: String, extension: String): Template = {
    val classSearchList = new ListBuffer[Class[_]]()

    def buildClassList(clazz: Class[_]): Unit = {
      if (clazz != null && clazz != classOf[Object] && !classSearchList.contains(clazz)) {
        classSearchList.append(clazz)
        buildClassList(clazz.getSuperclass)
        for (i <- clazz.getInterfaces) {
          buildClassList(i)
        }
      }
    }

    buildClassList(model.getClass)

    classSearchList.iterator.map { klass =>
      val withDots = s"${klass.getName}_$viewName.$extension"
      Try(getPrecompiledTemplateInstance(withDots))
    }.find(_.isSuccess)
      .map(_.get)
      .getOrElse(throw new NoSuchViewException(model, viewName))
  }

  private def getScalateViewInstance(templateDirUri: String, model: AnyRef, viewName: String, extension: String): String = {
    val path = model.getClass.getName.replace(".", "/")
    s"$templateDirUri/$path.$viewName.$extension"
  }
}

/**
 * This class is intended for use by both Xitrum and normal apps to create
 * additional Scalate template engine instances.
 *
 * @param allowReload Template files in templateDir will be reloaded every time
 * @param defaultType "jade", "mustache", "scaml", or "ssp"
 */
class ScalateEngine(
  templateDirUri: String, allowReload: Boolean, defaultType: String) extends TemplateEngine
    with ScalateEngineRenderInterface
    with ScalateEngineRenderTemplate
    with ScalateEngineRenderString
    with Log {
  import ScalateEngine._

  protected[this] val fileEngine: STE = createEngine(allowCaching = true, allowReload)

  // No need to cache or reload for stringEngine.
  protected[this] val stringEngine: STE = createEngine(allowCaching = false, allowReload = false)

  protected def createEngine(allowCaching: Boolean, allowReload: Boolean): STE = {
    val ret = new STE
    ret.allowCaching = allowCaching
    ret.allowReload = allowReload
    ret.bindings = List(
      // import things in the current action
      Binding(ACTION_BINDING_ID, classOf[Action].getName, importMembers = true),

      // import Scalate utilities like "unescape"
      Binding(CONTEXT_BINDING_ID, classOf[RenderContext].getName, importMembers = true))

    ret
  }

  //----------------------------------------------------------------------------
  // TemplateEngine interface methods (see also ScalateEngineRenderInterface)

  override def start(): Unit = {}

  override def stop(): Unit = {
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
    currentAction: Action, options: Map[String, Any]): ScalateRenderContext = {
    val context = new ScalateRenderContext(templateUri, engine, currentAction, templateType(options))
    val attributes = context.attributes

    // For bindings in engine
    attributes.update(ACTION_BINDING_ID, currentAction)
    attributes.update(CONTEXT_BINDING_ID, context)

    // Put action.at to context
    currentAction.at.foreach {
      case (k, v) =>
        if (k == ACTION_BINDING_ID || k == CONTEXT_BINDING_ID)
          log.warn(
            ACTION_BINDING_ID + " and " + CONTEXT_BINDING_ID +
              " are reserved key names for action's \"at\"")
        else
          attributes.update(k, v)
    }

    setFormats(context, currentAction, options)
    context
  }

  protected def setFormats(context: RenderContext, currentAction: Action, options: Map[String, Any]): Unit = {
    context.dateFormat = dateFormat(options).getOrElse(DateFormat.getDateInstance(DateFormat.DEFAULT, currentAction.locale))
    context.numberFormat = numberFormat(options).getOrElse(NumberFormat.getInstance(currentAction.locale))
  }

  //----------------------------------------------------------------------------

  /**
   * Production mode: Renders the precompiled template class.
   * Development mode: Renders Scalate template file relative to templateDir.
   * If the file does not exist, falls back to rendering the precompiled template class.
   *
   * @param currentAction Will be imported in the template as "helper"
   */
  protected def renderMaybePrecompiledFile(relUri: String, currentAction: Action, options: Map[String, Any]): String = {
    if (Config.productionMode)
      renderPrecompiledFile(relUri, currentAction, options)
    else
      renderNonPrecompiledFile(relUri, currentAction, options)
  }

  protected def renderPrecompiledFile(relUri: String, currentAction: Action, options: Map[String, Any]): String = {
    // In production mode, after being precompiled,
    // quickstart/action/AppAction.jade will become
    // class scalate.quickstart.action.$_scalate_$AppAction_jade
    val template = getPrecompiledTemplateInstance(relUri)

    renderTemplate(template, relUri, options)(currentAction)
  }

  protected def renderNonPrecompiledFile(relUri: String, currentAction: Action, options: Map[String, Any]): String = {
    val uri = templateDirUri + "/" + relUri
    val file = new File(uri)
    if (file.exists) {
      renderTemplateFile(uri, options)(currentAction)
    } else {
      // If called from a JAR library, the template may have been precompiled
      renderPrecompiledFile(relUri, currentAction, options)
    }
  }
}
