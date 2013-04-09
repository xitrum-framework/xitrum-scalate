package xitrum.view

import java.io.File

import xitrum.{Config, Action}

class ScalateTemplateEngine extends TemplateEngine {
  warmup()

  /**
   * Renders the template at the location identified by the given action class.
   *
   * Ex: When location = myapp.SiteIndex and Scalate template
   * engine is used, by default the template path will be:
   * src/main/scalate/myapp/SiteIndex.jade
   *
   * @param location the action class used to identify the template location
   *
   * @param options specific to the configured template engine
   */
  def renderView(location: Class[_ <: Action], currentAction: Action, options: Map[String, Any]): String =
    Scalate.renderView(location, currentAction, options)

  /**
   * Renders the template at the location identified by the package of the given
   * action class and the given fragment.
   *
   * Ex: When location = myapp.ArticleNew, fragment = form and Scalate template
   * engine is used, by default the template path will be:
   * src/main/scalate/myapp/_form.jade
   *
   * @param location the action class used to identify the template location
   *
   * @param options specific to the configured template engine
   */
  def renderFragment(location: Class[_ <: Action], fragment: String, currentAction: Action, options: Map[String, Any]): String =
    Scalate.renderFragment(location, fragment, currentAction, options)

  // Scalate takes several seconds to initialize.
  // On Xitrum startup, an instance of the configured template engine is created.
  // We take this chance to force Scalate to initialize on startup instead of on
  // the first request.
  private def warmup() {
    val dummyAction = new Action {
      def execute() {}
    }

    Scalate.renderJadeString("")(dummyAction)

    // Using File.createTempFile may cause error like this:
    // /private/var/folders/mk/lknymby579qcj5_wx8js461h0000gr/T/scalate-8516439701475108219-workdir/src/var/folders/mk/lknymby579qcj5_wx8js461h0000gr/T/tmp8554675948254167709.jade.scala:2: error: identifier expected but 'var' found.
    // package var.folders.mk.lknymby579qcj5_wx8js461h0000gr.T
    // ^
    // one error found
    val tmpFile = new File("scalateTemplateEngineTmpFile.jade")
    tmpFile.createNewFile()
    Scalate.renderTemplateFile(tmpFile.getAbsolutePath)(dummyAction)
    tmpFile.delete()
  }
}
