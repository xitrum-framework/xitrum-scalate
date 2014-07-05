package xitrum.view

import java.io.File
import xitrum.Action

/** Core xitrum.view.TemplateEngine interface methods. */
trait ScalateEngineRenderInterface {
  this: ScalateEngine =>

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
   * @param options       "type" -> "jade"/"mustache"/"scaml"/"ssp", "date" -> DateFormat, "number" -> NumberFormat
   */
  def renderView(location: Class[_ <: Action], currentAction: Action, options: Map[String, Any]): String = {
    val tpe     = templateType(options)
    val relPath = location.getName.replace('.', File.separatorChar) + "." + tpe
    renderMaybePrecompiledFile(relPath, currentAction, options)
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
   * @param options       "type" -> "jade"/"mustache"/"scaml"/"ssp", "date" -> DateFormat, "number" -> NumberFormat
   */
  def renderFragment(location: Class[_ <: Action], fragment: String, currentAction: Action, options: Map[String, Any]): String = {
    // location.getPackage will only return a non-null value if the current
    // ClassLoader is already aware of the package
    val pkgElems = location.getName.split('.').dropRight(1)
    val tpe      = templateType(options)
    val relPath  = pkgElems.mkString(File.separator) + File.separator + "_" + fragment + "." + tpe
    renderMaybePrecompiledFile(relPath, currentAction, options)
  }
}
