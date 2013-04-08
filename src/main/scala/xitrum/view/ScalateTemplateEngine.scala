package xitrum.view

import java.io.File

import xitrum.{Config, ActionEnv}

class ScalateTemplateEngine extends TemplateEngine {
  warmup()

  def renderTemplate(actionClass: Class[_ <: ActionEnv], action: ActionEnv, options: Map[String, Any]) =
    Scalate.renderTemplate(actionClass, action, options)

  // Scalate takes several seconds to initialize.
  // On Xitrum startup, an instance of the configured template engine is created.
  // We take this chance to force Scalate to initialize on startup instead of on
  // the first request.
  private def warmup() {
    val dummyAction = new ActionEnv {
      def execute() {}
      def onResponded() {}
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
