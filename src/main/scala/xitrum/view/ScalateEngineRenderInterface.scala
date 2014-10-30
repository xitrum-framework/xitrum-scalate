package xitrum.view

import java.io.File
import xitrum.Action

/** Core xitrum.view.TemplateEngine interface methods. */
trait ScalateEngineRenderInterface {
  this: ScalateEngine =>

  /**
   * Renders the template at the location identified by the template path:
   * {{{<scalateDir>/<path/of/template>.<templateType>}}}
   *
   * @param path          Template path
   * @param currentAction Will be imported in the template as "helper"
   * @param options       "type" -> "jade"/"mustache"/"scaml"/"ssp", "date" -> DateFormat, "number" -> NumberFormat
   */
  def renderView(path: String, currentAction: Action, options: Map[String, Any]): String = {
    val tpe = templateType(options)
    val relPath = path + "." + tpe
    renderMaybePrecompiledFile(relPath, currentAction, options)
  }

  /**
   * Renders the template at the location identified by the directory and the fragment:
   * {{{<scalateDir>/<path/of/directory>/_<fragment>.<templateType>}}}
   *
   * @param directory     Directory path that contains the fragment
   * @param fragment      Fragment
   * @param currentAction Will be imported in the template as "helper"
   * @param options       "type" -> "jade"/"mustache"/"scaml"/"ssp", "date" -> DateFormat, "number" -> NumberFormat
   */
  def renderFragment(directory: String, fragment: String, currentAction: Action, options: Map[String, Any]): String = {
    val tpe = templateType(options)
    val relPath = directory + File.separator + "_" + fragment + "." + tpe
    renderMaybePrecompiledFile(relPath, currentAction, options)
  }
}
